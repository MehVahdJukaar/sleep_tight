package net.mehvahdjukaar.sleep_tight.test;

import net.mehvahdjukaar.sleep_tight.test.controller.BirdGroundControl;
import net.mehvahdjukaar.sleep_tight.test.controller.BirdLookControl;
import net.mehvahdjukaar.sleep_tight.test.controller.PerchingFlier;
import net.mehvahdjukaar.sleep_tight.test.debug.MobTrail;
import net.mehvahdjukaar.sleep_tight.test.controller.BirdMoveControl;
import net.mehvahdjukaar.sleep_tight.test.navigator.BirdPathNavigation;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
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
 * It flies with {@link BirdPathNavigation} but has no goal that ever sets a walk target: paths are
 * computed on demand by {@link BirdDebug}, drawn, and then flown at a crawl so the shape of the
 * lattice path can actually be watched. The mob's yaw seeds the search heading, so the first turn
 * of a new path depends on where it happened to be pointing when the query was made.
 */
public class BirdTestMob extends PathfinderMob implements FlyingAnimal, PerchingFlier {

    // synched rather than derived, because there is no measurement that distinguishes a bird
    // gripping a branch from one hovering an inch above it. BirdGroundControl decides it server side
    // and this mirrors the answer out to the client for the model to pose off
    private static final EntityDataAccessor<Boolean> PERCHED =
            SynchedEntityData.defineId(BirdTestMob.class, EntityDataSerializers.BOOLEAN);

    private final BirdGroundControl groundControl = new BirdGroundControl(this);

    // server side only, kept around so the client side debug renderer entry can be refreshed
    @Nullable
    private Path debugPath;
    // sampled every tick, sent with the path so the drawn plan can be compared against the flown line
    private final MobTrail debugTrail = new MobTrail();

    public BirdTestMob(EntityType<? extends BirdTestMob> entityType, Level level) {
        super(entityType, level);
        this.moveControl = new BirdMoveControl(this);
        this.lookControl = new BirdLookControl(this);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PERCHED, false);
    }

    @Override
    public boolean isPerched() {
        return this.entityData.get(PERCHED);
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
     * Airborne, in the gait sense: feet off. Not {@code !onGround()}, which also says yes to a bird
     * that has settled on a branch but whose collision has not caught up yet.
     * <p>
     * Note this is not what picks the flier drag in {@code LivingEntity.travel}; that is an
     * {@code instanceof FlyingAnimal} test and applies whatever this returns.
     */
    @Override
    public boolean isFlying() {
        return !this.isPerched();
    }

    /**
     * Runs after the navigation and before the move control, which is the ordering the launch turn
     * needs: a hold requested this tick has to be visible to this tick's steering.
     */
    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        this.groundControl.tick();
        // set rather than pushed, so the control never has to know what a synched field is.
        // SynchedEntityData ignores a write that does not change the value, so this costs nothing
        this.entityData.set(PERCHED, this.groundControl.isPerched());
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(1, new RandomLookAroundGoal(this));
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new BirdPathNavigation(this, level);
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) return;
        // the ground control rides on customServerAiStep, which does not run for a dead or AI
        // disabled mob. Nothing would then ever clear noGravity and the corpse would hang in the air
        if (this.isImmobile() || !this.isEffectiveAi()) {
            this.setNoGravity(false);
        }
        if (this.debugPath == null) return;
        // sampled at full tick rate even though the packet only goes out every fifth one: the
        // whole point is to catch the wobble between waypoints, which resampling would smooth away.
        // Stops with the flight, so the finished line stays on screen instead of being buried under
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

    /** Which locomotion phase the ground half is in, for the debug overlay. */
    public String getGaitName() {
        return this.groundControl.getPhaseName();
    }

    /** The heading a launch turn is aiming at, or NaN when there is no launch turn. */
    public float getLaunchYaw() {
        return this.groundControl.getLaunchYaw();
    }

    /** Draws the path and starts flying it. Passing null just clears whatever was being followed. */
    public void followPath(@Nullable Path path) {
        this.debugPath = path;
        this.debugTrail.reset();
        // clear first: moveTo keeps the old path when the new one compares equal, and an already
        // finished one would make it bail out
        this.getNavigation().stop();
        if (path != null) {
            this.getNavigation().moveTo(path, 0.7);
        }
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
