package net.mehvahdjukaar.sleep_tight.test;

import net.mehvahdjukaar.sleep_tight.test.controller.BirdFlightConfig;
import net.mehvahdjukaar.sleep_tight.test.controller.BirdStateMachine;
import net.mehvahdjukaar.sleep_tight.test.throttle.FlightEnvelope;
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
    // gripping a branch from one hovering an inch above it. BirdStateMachine decides it server side
    // and this mirrors the answer out to the client for the model to pose off
    private static final EntityDataAccessor<Boolean> GROUNDED =
            SynchedEntityData.defineId(BirdTestMob.class, EntityDataSerializers.BOOLEAN);

    // pitch is the state machine's decision, not a look direction, so it travels in its own field
    // rather than riding on xRot the way it used to. That leaves xRot to the look control, which is
    // what a bird turning its head while diving needs, and it costs a float only on ticks it changes
    private static final EntityDataAccessor<Float> BODY_PITCH =
            SynchedEntityData.defineId(BirdTestMob.class, EntityDataSerializers.FLOAT);

    // what the wings are putting out, in blocks per tick squared. Whichever layer is in charge sets
    // it - the flight control while flying a path, the state machine while fluttering - and the
    // model is a readout of it and nothing else. Synched rather than derived because neither of
    // those layers exists client side, and it is the only wing input the renderer needs: the flap
    // rate follows from it, and the phase is integrated locally from that on both sides
    private static final EntityDataAccessor<Float> WING_THRUST =
            SynchedEntityData.defineId(BirdTestMob.class, EntityDataSerializers.FLOAT);

    /** How hard the debug tool flies its paths, as a fraction of the envelope. */
    private static final double FLIGHT_SPEED_MODIFIER = 0.7;
    private static final double WALK_SPEED_MODIFIER = 0.7;

    // how fast the wings open and close, in fractions of the way per tick. Fast enough that a hop
    // is spent with them out rather than still opening, slow enough not to snap
    private static final float WING_SPREAD_PER_TICK = 0.25F;

    private final BirdStateMachine stateMachine = new BirdStateMachine(this);
    private final BirdFlightControl flightControl;
    private final MoveControl walkControl;
    private final BirdWalkNavigation groundNavigation;
    private final BirdFlightNavigation flightNavigation;

    // client side copies of the synched pitch, so the renderer has something to interpolate between
    private float bodyPitch;
    private float bodyPitchO;

    // where the wings are in their stroke, 0 through 1, and how far out they are held, 0 folded to
    // 1 spread. Both integrated locally rather than synched: they run off WING_THRUST and the
    // grounded flag, which both sides have, and sending a phase that changes every tick would be
    // paying network for something either side can work out
    private float flapPhase;
    private float flapPhaseO;
    private float wingSpread;
    private float wingSpreadO;

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
        builder.define(WING_THRUST, 0.0F);
    }

    /**
     * Vanilla pins this to a flat 0.02, which is the thrust a bird can <i>sustain</i>. Handing back
     * the peak instead is what lets a takeoff burst actually arrive: everything downstream of the
     * throttle is a fraction of this number, and {@code moveRelative} normalises anything over full
     * throttle away, so a burst that does not fit under the ceiling is silently not a burst.
     * Sustained flight is unaffected - it simply flies at a lower throttle for the same thrust.
     */
    @Override
    protected float getFlyingSpeed() {
        return (float) FlightEnvelope.wingPeakThrust();
    }

    /**
     * The state machine is the authority, and it only exists server side, so the client reads the
     * mirror instead. Asking the control directly rather than the mirror matters on the tick a walk
     * is chosen: the two are one {@code customServerAiStep} apart, and {@link WalkOrFly} decides
     * outside that window.
     */
    @Override
    public boolean isGrounded() {
        return this.level().isClientSide ? this.entityData.get(GROUNDED) : this.stateMachine.isGrounded();
    }

    @Override
    public boolean isHoldingForLaunch() {
        return this.stateMachine.isHoldingForLaunch();
    }

    @Override
    public void requestLaunch(float launchYaw) {
        this.stateMachine.requestLaunch(launchYaw);
    }

    @Override
    public void cancelLaunch() {
        this.stateMachine.cancelLaunch();
    }

    /**
     * Airborne, in the locomotion sense: feet off. Not {@code !onGround()}, which also says yes to
     * a bird that has settled on a branch but whose collision has not caught up yet.
     * <p>
     * Note this is not what picks the flier drag in {@code LivingEntity.travel}; that is an
     * {@code instanceof FlyingAnimal} test and ignores whatever this returns, so a walking bird
     * still gets a flier's 0.91 vertical drag instead of the usual 0.98. That is not harmless, as
     * this used to claim: it is a fifth off the height of every jump, and it is why the mob needs a
     * {@code JUMP_STRENGTH} of 0.46 to step up what a chicken steps up on 0.42.
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
     * Where the wings are in their stroke, in whole strokes. Not wrapped back into 0..1 for the
     * caller: the phase wraps once a beat and lerping across that wrap would run the stroke
     * backwards for a frame, so the wrap is undone here instead.
     */
    public float getFlapPhase(float partialTick) {
        float to = this.flapPhase < this.flapPhaseO ? this.flapPhase + 1.0F : this.flapPhase;
        return Mth.lerp(partialTick, this.flapPhaseO, to);
    }

    /** How far the wings are held out, 0 folded against the body to 1 fully spread. */
    public float getWingSpread(float partialTick) {
        return Mth.lerp(partialTick, this.wingSpreadO, this.wingSpread);
    }

    /**
     * Runs on both sides off synched inputs, so the client never has to be told where the wings
     * are. Two independent channels: the stroke rate follows thrust, which is what makes a bird
     * bursting off a perch beat hard and one gliding into a perch barely beat at all; and the
     * spread follows whether the feet are down, so a walking bird has them folded flat whatever
     * else is going on.
     */
    private void tickWings() {
        this.flapPhaseO = this.flapPhase;
        this.wingSpreadO = this.wingSpread;

        double effort = FlightEnvelope.wingEffortFor(this.entityData.get(WING_THRUST));
        double rate = Mth.lerp(effort, BirdFlightConfig.minFlapRate, BirdFlightConfig.maxFlapRate);
        this.flapPhase = (float) ((this.flapPhase + rate) % 1.0);

        float wanted = this.isGrounded() ? 0.0F : 1.0F;
        this.wingSpread = Mth.approach(this.wingSpread, wanted, WING_SPREAD_PER_TICK);
    }

    /**
     * Points the body somewhere other than along its flight until cleared. Airborne only: a landing
     * always straightens the bird out, whatever is set here.
     */
    public void setFlightPitch(float degrees) {
        this.stateMachine.setPitchOverride(degrees);
    }

    public void clearFlightPitch() {
        this.stateMachine.clearPitchOverride();
    }

    /**
     * Runs after the navigation and before the move control, which is the ordering both the launch
     * turn and the locomotion swap need: a change made this tick has to be visible to this tick's
     * steering, and must not land between a navigation and the move control it was feeding.
     */
    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        this.stateMachine.tick();
        this.installLocomotionForMode();
        // set rather than pushed, so the control never has to know what a synched field is.
        // SynchedEntityData ignores a write that does not change the value, so a perched bird holding
        // a level pitch costs nothing
        this.entityData.set(GROUNDED, this.stateMachine.isGrounded());
        this.entityData.set(BODY_PITCH, this.stateMachine.bodyPitch());
        this.entityData.set(WING_THRUST, (float) this.wingThrust());
    }

    /**
     * What the wings are putting out, in blocks per tick squared. Two regimes rather than two copies
     * of one number: flying it out of a servo against a speed limit, and holding yourself up with no
     * path to fly, which is a constant because there is nothing to servo against. Everything with
     * its feet down is zero, which is what folds the wings.
     * <p>
     * Read one {@code customServerAiStep} before the flight control recomputes it, so a flying
     * bird's figure is a tick stale. A tick of lag on a flap rate is not something anyone can see,
     * and the alternative is the entity having to know when the move control has run.
     */
    private double wingThrust() {
        if (this.stateMachine.isFluttering()) {
            return BirdStateMachine.flutterThrust();
        }
        return this.stateMachine.isAirborne() ? this.flightControl.wingThrust() : 0.0;
    }

    /**
     * Swaps the navigation and the move control over together, so exactly one locomotion pair is
     * ever live and the other is never ticked. The outgoing navigation is stopped rather than left
     * holding a path, since nothing will be advancing it and a stale one would be picked straight
     * back up on the way home.
     * <p>
     * A mode may also decline to have an opinion, which fluttering does, and that abstention is
     * what lets a bird stride over a one block gap: it loses ground contact for a tick or two, and
     * swapping the pair over during that would stop the walk it is halfway through.
     */
    private void installLocomotionForMode() {
        BirdStateMachine.Locomotion wanted = this.stateMachine.locomotion();
        boolean walking = wanted == BirdStateMachine.Locomotion.WALK;
        if (wanted == BirdStateMachine.Locomotion.KEEP
                || walking == (this.navigation == this.groundNavigation)) {
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
        this.tickWings();
        super.tick();
        this.bodyPitch = this.entityData.get(BODY_PITCH);
        if (this.level().isClientSide) return;
        // the state machine rides on customServerAiStep, which does not run for a dead or AI
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
        return this.stateMachine.getModeName();
    }

    /** The heading a launch turn is aiming at, or NaN when there is no launch turn. */
    public float getLaunchYaw() {
        return this.stateMachine.getLaunchYaw();
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
        this.stateMachine.onFlightRequested();
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
        this.stateMachine.beginWalk();
        if (!this.stateMachine.isWalking()) {
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
                // vanilla's 0.42 is tuned against the 0.98 vertical drag everything that is not a
                // FlyingAnimal gets. This mob is one, so LivingEntity.travel gives it 0.91 instead
                // (the same drag as its horizontal axes) and the identical jump peaks at 1.09 blocks
                // rather than 1.25 - which reads as "can't quite make a one block step", because it
                // can't, for about two ticks either side of the apex. 0.46 puts the peak back on
                // 1.25 exactly, so the bird steps up what a chicken steps up
                .add(Attributes.JUMP_STRENGTH, 0.6)
                // doubles as the pathfinder's range cap and node budget (16 nodes per block of
                // follow range), so it needs to be generous enough to path across a test arena
                .add(Attributes.FOLLOW_RANGE, 64.0);
    }
}
