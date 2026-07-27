package net.mehvahdjukaar.sleep_tight.test.controller;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

/**
 * Steering for the bird lattice paths. Replaces {@link net.minecraft.world.entity.ai.control.FlyingMoveControl},
 * which is a bang-bang controller: it snaps yaw up to 90 degrees per tick and applies full thrust
 * until it is within half a millimetre of the waypoint. On a nearly frictionless flying body that
 * means every turn drifts wide and every arrival overshoots.
 * <p>
 * Four changes. Yaw is rate limited so the flown arc resembles the planned one. Momentum is turned
 * with the body instead of being left to decay along the old heading. Thrust is scaled down through
 * hard turns. And both thrust axes are braked against the distance still to cover,
 * using the fact that cutting thrust leaves the mob coasting {@code v * drag / (1 - drag)} more
 * blocks: as long as the stopping distance fits in what is left, keep pushing, otherwise back off
 * proportionally.
 * <p>
 * Nothing here reads the acceleration constant. Vanilla scales AI movement input by a flat
 * {@code LivingEntity.getFlyingSpeed()} = 0.02 while airborne (the FLYING_SPEED attribute only
 * reaches physics through {@code zza}), which is easy to get wrong; the braking rule only needs the
 * drag coefficient and the current velocity, both of which are observable.
 */
public class BirdMoveControl extends MoveControl {

    // from LivingEntity.travel: airborne horizontal drag is a flat 0.91. Vertical is 0.91 for a
    // FlyingAnimal and 0.98 for everything else, and 0.98 is a 50 tick time constant, i.e. tens of
    // blocks of coast. A flier that is not a FlyingAnimal is compensating for that here and will
    // climb noticeably slower than it flies
    private static final double HORIZONTAL_DRAG = 0.91;
    private static final double FLYING_ANIMAL_VERTICAL_DRAG = 0.91;
    private static final double DEFAULT_VERTICAL_DRAG = 0.98;

    public BirdMoveControl(Mob mob) {
        super(mob);
    }

    /** {@code operation}'s type is protected on the vanilla class, so name-only is what escapes. */
    public String getOperationName() {
        return this.operation.name();
    }

    @Override
    public void tick() {
        // the navigation is the authority on whether we are still going somewhere: nothing ever
        // clears MoveControl.operation back to WAIT for us. Same approach as SmoothSwimmingMoveControl
        if (!this.hasWanted() || this.mob.getNavigation().isDone()) {
            this.coast();
            return;
        }
        this.mob.setNoGravity(true);

        double dx = this.wantedX - this.mob.getX();
        double dy = this.wantedY - this.mob.getY();
        double dz = this.wantedZ - this.mob.getZ();
        if (dx * dx + dy * dy + dz * dz < MIN_SPEED_SQR) {
            this.coast();
            return;
        }

        // a thrust fraction, not a speed: vanilla feeds the same product to setSpeed. 1.0 is full
        // throttle, which settles at about 0.22 blocks per tick
        float cruise = (float) Mth.clamp(
                this.speedModifier * this.mob.getAttributeValue(Attributes.FLYING_SPEED), 0.0, 1.0);

        float yawBefore = this.mob.getYRot();
        float yawError = this.steerYaw(dx, dz);
        this.turnVelocityWithBody(Mth.degreesDifference(yawBefore, this.mob.getYRot()));
        this.applyThrust(cruise, dx, dy, dz, yawError);
        this.matchPitchToVelocity();
    }

    /** Cut thrust and let drag do the rest. Leaves {@code speed} alone, stuck detection reads it. */
    private void coast() {
        this.mob.setXxa(0.0F);
        this.mob.setYya(0.0F);
        this.mob.setZza(0.0F);
    }

    /** Turns towards the waypoint at a capped rate and returns the yaw error before turning. */
    private float steerYaw(double dx, double dz) {
        float wantedYaw = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
        float error = Mth.degreesDifference(this.mob.getYRot(), wantedYaw);
        this.mob.setYRot(this.rotlerp(this.mob.getYRot(), wantedYaw, BirdFlightConfig.maxYawPerTick));
        // a bird's body points where it flies. Left alone, BodyRotationControl lags the turn by
        // several ticks and the model reads as sliding sideways through the arc
        this.mob.yBodyRot = this.mob.getYRot();
        return error;
    }

    /**
     * Brings the horizontal velocity round with the body by the yaw just applied. Thrust alone
     * cannot turn a flier: {@code travel()} pushes along the new yaw but leaves the old velocity to
     * decay at 0.91 a tick, an 11 tick time constant, so the velocity ends up far behind the body
     * and the mob crabs through the arc. Only the direction is changed, never the magnitude, so
     * thrust and drag still own the speed and the turn radius comes out as {@code speed / yawRate}.
     */
    private void turnVelocityWithBody(float degrees) {
        float radians = degrees * BirdFlightConfig.velocitySteerFraction * Mth.DEG_TO_RAD;
        float cos = Mth.cos(radians);
        float sin = Mth.sin(radians);
        Vec3 velocity = this.mob.getDeltaMovement();
        this.mob.setDeltaMovement(
                velocity.x * cos - velocity.z * sin, velocity.y, velocity.x * sin + velocity.z * cos);
    }

    /**
     * Splits one throttle between forward and climb by the slope of the line to the waypoint, so the
     * mob flies at the angle the path was drawn at.
     * <p>
     * The two axes have to share a throttle rather than run their own controllers.
     * {@code moveRelative} normalises {@code (xxa, yya, zza)} when it is longer than 1 and scales it
     * by a flat constant, so feeding it the unit direction to the waypoint puts the thrust exactly
     * along that line; with equal drag on both axes the velocity then settles along it too. Driving
     * altitude from its own position error instead makes the mob fly whatever slope its vertical
     * droop happens to balance at, which is never the planned one.
     */
    private void applyThrust(float cruise, double dx, double dy, double dz, float yawError) {
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        double distance = Math.sqrt(horizontal * horizontal + dy * dy);
        float forwardShare = distance < 1.0E-4 ? 0.0F : (float) (horizontal / distance);
        float climbShare = distance < 1.0E-4 ? 0.0F : (float) (dy / distance);

        Remaining left = this.remainingAlongPath();
        Vec3 velocity = this.mob.getDeltaMovement();

        float forward = cruise * forwardShare * turningSpeedFactor(Math.abs(yawError));
        forward *= brakeFactor(left.horizontal(), velocity.horizontalDistance(), HORIZONTAL_DRAG);
        this.mob.setSpeed(forward);

        float climb = cruise * climbShare;
        // only brake what is already heading at the destination. Pulling out of a dive is
        // acceleration against the current velocity, not overshoot
        if (velocity.y * left.vertical() > 0.0) {
            climb *= brakeFactor(Math.abs(left.vertical()), Math.abs(velocity.y), this.verticalDrag());
        }
        this.mob.setYya(climb);
    }

    /**
     * How much of the intended thrust survives. Cutting thrust now would still carry the mob
     * {@code speed * drag / (1 - drag)} blocks, so the fastest we can be going and still arrive
     * with {@code distance} left is {@code distance * (1 - drag) / drag}. Returns 1 while there is
     * room to keep pushing and falls off proportionally once there is not.
     */
    private static float brakeFactor(double distance, double speed, double drag) {
        if (speed < 1.0E-4) {
            return 1.0F;
        }
        double maxSafeSpeed = distance * (1.0 - drag) / drag * BirdFlightConfig.brakeAggressiveness;
        return (float) Mth.clamp(maxSafeSpeed / speed, 0.0, 1.0);
    }

    private static float turningSpeedFactor(float degreesToTurn) {
        float t = Mth.clamp((degreesToTurn - BirdFlightConfig.turnSlowdownStart)
                / (BirdFlightConfig.turnSlowdownFull - BirdFlightConfig.turnSlowdownStart), 0.0F, 1.0F);
        return Mth.lerp(t, 1.0F, BirdFlightConfig.minTurnSpeedFactor);
    }

    /** What is left to fly before the mob has to be stopped, split by axis. */
    private record Remaining(double horizontal, double vertical) {}

    /**
     * Measured along the path rather than to the next waypoint: intermediate waypoints are flown
     * through, only the end of the path is an arrival. Both axes have to be measured the same way,
     * or the one with the shorter horizon brakes against an error the other one ignores and the mob
     * cannot hold a slope. Stops summing past the lookahead.
     */
    private Remaining remainingAlongPath() {
        Path path = this.mob.getNavigation().getPath();
        if (path == null || path.isDone()) {
            return new Remaining(
                    Math.hypot(this.wantedX - this.mob.getX(), this.wantedZ - this.mob.getZ()),
                    this.wantedY - this.mob.getY());
        }
        Vec3 from = this.mob.position();
        double horizontal = 0.0;
        double endY = this.mob.getY();
        for (int i = path.getNextNodeIndex();
             i < path.getNodeCount() && horizontal < BirdFlightConfig.brakeLookahead; i++) {
            Vec3 to = path.getEntityPosAtNode(this.mob, i);
            horizontal += Math.hypot(to.x - from.x, to.z - from.z);
            endY = to.y;
            from = to;
        }
        return new Remaining(horizontal, endY - this.mob.getY());
    }

    private double verticalDrag() {
        return this.mob instanceof FlyingAnimal ? FLYING_ANIMAL_VERTICAL_DRAG : DEFAULT_VERTICAL_DRAG;
    }

    /**
     * Pitch tracks where the mob is actually going rather than where the waypoint is, so a mob that
     * is still drifting from the last leg does not point somewhere it is not moving. Purely visual:
     * for anything that is not elytra flying, travel() derives motion from yaw and yya only.
     */
    private void matchPitchToVelocity() {
        Vec3 velocity = this.mob.getDeltaMovement();
        float wantedPitch = (float) -(Mth.atan2(velocity.y, velocity.horizontalDistance()) * (180.0 / Math.PI));
        wantedPitch = Mth.clamp(wantedPitch, -BirdFlightConfig.maxPitch, BirdFlightConfig.maxPitch);
        this.mob.setXRot(this.rotlerp(this.mob.getXRot(), wantedPitch, BirdFlightConfig.maxPitchPerTick));
    }
}
