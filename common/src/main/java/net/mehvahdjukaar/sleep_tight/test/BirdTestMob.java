package net.mehvahdjukaar.sleep_tight.test;

import net.mehvahdjukaar.sleep_tight.test.controller.BirdGroundConfig;
import net.mehvahdjukaar.sleep_tight.test.controller.BirdGroundControl;
import net.mehvahdjukaar.sleep_tight.test.controller.WalkOrFly;
import net.mehvahdjukaar.sleep_tight.test.controller.PerchingFlier;
import net.mehvahdjukaar.sleep_tight.test.debug.MobTrail;
import net.mehvahdjukaar.sleep_tight.test.controller.BirdFlightControl;
import net.mehvahdjukaar.sleep_tight.test.navigator.BirdFlightNavigation;
import net.mehvahdjukaar.sleep_tight.test.navigator.BirdWalkNavigation;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.animal.PolarBear;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;
import org.jetbrains.annotations.Nullable;

/**
 * Scratch bird for testing the lattice pathfinder in game. Not part of the mod content.
 * <p>
 * It has no goal that ever sets a walk target: destinations come from {@link BirdDebug}, are drawn,
 * and are then travelled at a crawl so the shape of the lattice path can actually be watched. The
 * mob's yaw seeds the search heading, so the first turn of a new path depends on where it happened
 * to be pointing when the query was made.
 * <p>
 * It carries two complete locomotion pairs and installs exactly one of them at a time: the lattice
 * flier ({@link BirdFlightNavigation} plus {@link BirdFlightControl}) and plain vanilla walking. Which
 * one a destination gets is {@link WalkOrFly}'s call. Keeping them swapped rather than blended is
 * the whole design: the flight stack never has to know what a walk is, and vanilla's walking never
 * has to survive a throttle profile being applied to it.
 */
public class BirdTestMob extends PathfinderMob implements FlyingAnimal, PerchingFlier {

    // synched rather than derived, because there is no measurement that distinguishes a bird
    // gripping a branch from one hovering an inch above it. BirdGroundControl decides it server side
    // and this mirrors the answer out to the client for the model to pose off
    private static final EntityDataAccessor<Boolean> GROUNDED =
            SynchedEntityData.defineId(BirdTestMob.class, EntityDataSerializers.BOOLEAN);

    // pitch is the ground control's decision, not a look direction, so it travels in its own field
    // rather than riding on xRot the way it used to. That leaves xRot to the look control, which is
    // what a bird turning its head while diving needs, and it costs a float only on ticks it changes
    private static final EntityDataAccessor<Float> BODY_PITCH =
            SynchedEntityData.defineId(BirdTestMob.class, EntityDataSerializers.FLOAT);

    /** How hard the debug tool flies its paths, as a fraction of the envelope. */
    private static final double FLIGHT_SPEED_MODIFIER = 0.7;
    private static final double WALK_SPEED_MODIFIER = 0.7;

    private final BirdGroundControl groundControl = new BirdGroundControl(this);
    private final BirdFlightControl flightControl;
    private final MoveControl walkControl;
    private final BirdWalkNavigation groundNavigation;
    private final BirdFlightNavigation flightNavigation;

    // client side copies of the synched pitch, so the renderer has something to interpolate between
    private float bodyPitch;
    private float bodyPitchO;

    // server side only, kept around so the client side debug renderer entry can be refreshed
    @Nullable
    private Path debugPath;
    // sampled every tick, sent with the path so the drawn plan can be compared against the flown line
    private final MobTrail debugTrail = new MobTrail();

    public BirdTestMob(EntityType<? extends BirdTestMob> entityType, Level level) {
        super(entityType, level);
        // vanilla builds the navigation from inside the super constructor, through createNavigation,
        // so taking the typed reference back off the field here is the one place it can happen. The
        // reference has to be kept because getNavigation() is vanilla's walker while the bird is on foot
        this.flightNavigation = (BirdFlightNavigation) this.navigation;
        this.flightControl = new BirdFlightControl(this);
        this.walkControl = new MoveControl(this);
        this.groundNavigation = new BirdWalkNavigation(this, level);
        this.moveControl = this.flightControl;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(GROUNDED, false);
        builder.define(BODY_PITCH, 0.0F);
    }

    /**
     * The ground control is the authority, and it only exists server side, so the client reads the
     * mirror instead. Asking the control directly rather than the mirror matters on the tick a walk
     * is chosen: the two are one {@code customServerAiStep} apart, and {@link WalkOrFly} decides
     * outside that window.
     */
    @Override
    public boolean isGrounded() {
        return this.level().isClientSide ? this.entityData.get(GROUNDED) : this.groundControl.isGrounded();
    }

    @Override
    public boolean isHoldingForLaunch() {
        return this.groundControl.isHoldingForLaunch();
    }

    @Override
    public void requestLaunch(float launchYaw) {
        this.groundControl.requestLaunch(launchYaw);
    }

    @Override
    public void cancelLaunch() {
        this.groundControl.cancelLaunch();
    }

    /**
     * Airborne, in the locomotion sense: feet off. Not {@code !onGround()}, which also says yes to
     * a bird that has settled on a branch but whose collision has not caught up yet.
     * <p>
     * Note this is not what picks the flier drag in {@code LivingEntity.travel}; that is an
     * {@code instanceof FlyingAnimal} test and applies whatever this returns. Which is why a walking
     * bird still gets a flier's vertical drag, and why that does not matter: gravity dwarfs it.
     */
    @Override
    public boolean isFlying() {
        return !this.isGrounded();
    }

    /** Where the body is pointed, in degrees, positive nose down. Interpolated for rendering. */
    public float getBodyPitch(float partialTick) {
        return Mth.lerp(partialTick, this.bodyPitchO, this.bodyPitch);
    }

    /**
     * Points the body somewhere other than along its flight until cleared. Airborne only: a landing
     * always straightens the bird out, whatever is set here.
     */
    public void setFlightPitch(float degrees) {
        this.groundControl.setPitchOverride(degrees);
    }

    public void clearFlightPitch() {
        this.groundControl.clearPitchOverride();
    }

    /**
     * Runs after the navigation and before the move control, which is the ordering both the launch
     * turn and the locomotion swap need: a change made this tick has to be visible to this tick's
     * steering, and must not land between a navigation and the move control it was feeding.
     */
    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        this.groundControl.tick();
        this.installLocomotionForMode();
        // set rather than pushed, so the control never has to know what a synched field is.
        // SynchedEntityData ignores a write that does not change the value, so a perched bird holding
        // a level pitch costs nothing
        this.entityData.set(GROUNDED, this.groundControl.isGrounded());
        this.entityData.set(BODY_PITCH, this.groundControl.bodyPitch());
    }

    /**
     * Swaps the navigation and the move control over together, so exactly one locomotion pair is
     * ever live and the other is never ticked. The outgoing navigation is stopped rather than left
     * holding a path, since nothing will be advancing it and a stale one would be picked straight
     * back up on the way home.
     */
    private void installLocomotionForMode() {
        boolean walking = this.groundControl.isWalking();
        if (walking == (this.navigation == this.groundNavigation)) {
            return;
        }
        this.navigation.stop();
        this.navigation = walking ? this.groundNavigation : this.flightNavigation;
        this.moveControl = walking ? this.walkControl : this.flightControl;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(1, new RandomLookAroundGoal(this));
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new BirdFlightNavigation(this, level);
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public void tick() {
        this.bodyPitchO = this.bodyPitch;
        super.tick();
        this.bodyPitch = this.entityData.get(BODY_PITCH);
        if (this.level().isClientSide) return;
        // the ground control rides on customServerAiStep, which does not run for a dead or AI
        // disabled mob. Nothing would then ever clear noGravity and the corpse would hang in the air
        if (this.isImmobile() || !this.isEffectiveAi()) {
            this.setNoGravity(false);
        }
        if (this.debugPath == null) return;
        // sampled at full tick rate even though the packet only goes out every fifth one: the
        // whole point is to catch the wobble between waypoints, which resampling would smooth away.
        // Stops with the travel, so the finished line stays on screen instead of being buried under
        // the drift of a mob hovering at its destination
        if (!this.getNavigation().isDone()) {
            this.debugTrail.sample(this.position());
        }
        // keep resending so the highlighted next node and the "thinking" overlay track the mob live.
        // Every tick through a launch turn: it is over in a second or so and a fifth of the frames
        // is not enough to watch the heading close, which is the whole point of drawing it
        if (this.tickCount % 5 == 0 || this.isHoldingForLaunch()) {
            BirdDebug.broadcastPath(this, this.debugPath);
        }
    }

    public MobTrail getDebugTrail() {
        return this.debugTrail;
    }

    /**
     * The flying half, whether or not it is the one currently installed. The debug tool asks for it
     * by name because it wants to measure the lattice search specifically, and
     * {@code getNavigation()} hands back vanilla's walker whenever the bird is on foot.
     */
    public BirdFlightNavigation getFlightNavigation() {
        return this.flightNavigation;
    }

    /** Which mode the mob is in, for the debug overlay. */
    public String getModeName() {
        return this.groundControl.getModeName();
    }

    /** The heading a launch turn is aiming at, or NaN when there is no launch turn. */
    public float getLaunchYaw() {
        return this.groundControl.getLaunchYaw();
    }

    /**
     * The one decision point, reached from both navigations. Runs the lattice search first because
     * the comparison needs a real route to weigh against rather than a straight line, and throws it
     * away on the hops that come out as a walk.
     */
    @Override
    public boolean travelTo(BlockPos target, int accuracy, double speed) {
        Path flightPath = this.flightNavigation.createPath(target, accuracy);
        return this.walkIfCheaper(target, flightPath).walk() || this.flyPath(flightPath, speed);
    }

    /**
     * Weighs walking against the flight path it was handed, and walks if that wins. Flying is
     * deliberately left to the caller, which is about to fly that path itself if the answer comes
     * back no. Separate from {@link #travelTo} so the debug tool can weigh a path it has already
     * built and timed, and report what the answer was.
     */
    public WalkOrFly walkIfCheaper(BlockPos target, @Nullable Path flightPath) {
        WalkOrFly choice = WalkOrFly.decide(this, this.groundNavigation, target, flightPath);
        if (choice.walk()) {
            this.walkPath(choice.groundPath());
        }
        return choice;
    }

    /**
     * Draws the path and starts flying it at the debug tool's crawl. Passing null just clears
     * whatever was being followed.
     */
    public boolean followPath(@Nullable Path path) {
        return this.flyPath(path, FLIGHT_SPEED_MODIFIER);
    }

    private boolean flyPath(@Nullable Path path, double speed) {
        this.beginDebugPath(path);
        // a flight request ends any walk, and the swap has to happen before the path is handed over
        // or it would go to a navigation that is about to be uninstalled
        this.groundControl.endWalk();
        this.installLocomotionForMode();
        // clear first: moveTo keeps the old path when the new one compares equal, and an already
        // finished one would make it bail out
        this.flightNavigation.stop();
        return path != null && this.flightNavigation.moveTo(path, speed);
    }

    /**
     * The same for a ground path, which the mob walks at its own speed rather than flying. Does
     * nothing airborne, where there is no walk to begin: {@link WalkOrFly} only ever picks this from
     * a standstill, so the guard is a belt rather than a branch anything reaches.
     */
    public void walkPath(Path path) {
        this.groundControl.beginWalk();
        if (!this.groundControl.isWalking()) {
            return;
        }
        this.beginDebugPath(path);
        this.installLocomotionForMode();
        this.groundNavigation.stop();
        this.groundNavigation.moveTo(path, WALK_SPEED_MODIFIER);
    }

    private void beginDebugPath(@Nullable Path path) {
        this.debugPath = path;
        this.debugTrail.reset();
    }

    public static AttributeSupplier.Builder makeAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FLYING_SPEED, 1)
                // doubles as the pathfinder's range cap and node budget (16 nodes per block of
                // follow range), so it needs to be generous enough to path across a test arena
                .add(Attributes.FOLLOW_RANGE, 64.0);
    }
}
