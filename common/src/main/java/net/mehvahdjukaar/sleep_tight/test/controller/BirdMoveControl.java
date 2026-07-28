package net.mehvahdjukaar.sleep_tight.test.controller;

import net.mehvahdjukaar.sleep_tight.test.navigator.BirdPathNavigation;
import net.mehvahdjukaar.sleep_tight.test.throttle.FlightEnvelope;
import net.mehvahdjukaar.sleep_tight.test.throttle.ThrottleProfile;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Steering for the bird lattice paths, replacing vanilla's
 * {@link net.minecraft.world.entity.ai.control.FlyingMoveControl}, which snaps yaw up to 90 degrees
 * per tick and applies full thrust until it is within half a millimetre of the waypoint.
 * <p>
 * Three jobs, and only three. Point the body at the carrot at a limited rate; bring the momentum
 * round with it, since {@code travel()} only ever pushes along yaw and would otherwise leave the
 * velocity decaying along the old heading; and hold whatever speed the {@link ThrottleProfile} says
 * is allowed here.
 * <p>
 * Sections 1, 2 and 6 of {@code believable_bird_flight.md} are all in now: velocity steering here,
 * arc-length pure pursuit in {@link net.mehvahdjukaar.sleep_tight.test.navigator.PathRuler}, and an
 * absorbing arrival in {@code BirdPathNavigation.followThePath}. What is still missing is the gait
 * machine from section 6 (takeoff, flare, perch) and banking from section 5.
 * <p>
 * What got deleted, because the profile subsumes all of it: the per-tick walk over the remaining
 * path nodes, the stopping-distance brake computed from it, and the separate slow-down-in-turns
 * factor. If the bird still cuts a corner now, that is the planner's numbers being wrong rather than
 * a second slowdown fighting the first, which is the whole point of testing it this way.
 */
public class BirdMoveControl extends MoveControl {

    // the carrot sits a carrotDistance ahead, so this only ever catches a degenerate direction, never
    // arrival. Not vanilla's MIN_SPEED_SQR, which is an acceptance sphere and has no meaning here
    private static final double MIN_DIRECTION_LENGTH = 1.0E-4;

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

        Vec3 toCarrot = new Vec3(this.wantedX - this.mob.getX(),
                this.wantedY - this.mob.getY(), this.wantedZ - this.mob.getZ());
        if (toCarrot.length() < MIN_DIRECTION_LENGTH) {
            this.coast();
            return;
        }

        float yawBefore = this.mob.getYRot();
        this.steerYaw(toCarrot.x, toCarrot.z);
        this.turnVelocityWithBody(Mth.degreesDifference(yawBefore, this.mob.getYRot()));
        this.applyThrust(toCarrot);
        this.matchPitchToVelocity();
    }

    /** Cut thrust and let drag do the rest. Leaves {@code speed} alone, stuck detection reads it. */
    private void coast() {
        this.mob.setXxa(0.0F);
        this.mob.setYya(0.0F);
        this.mob.setZza(0.0F);
    }

    /** Turns towards the carrot at a capped rate. */
    private void steerYaw(double dx, double dz) {
        float wantedYaw = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
        this.mob.setYRot(this.rotlerp(this.mob.getYRot(), wantedYaw, BirdFlightConfig.maxYawPerTick));
        // a bird's body points where it flies. Left alone, BodyRotationControl lags the turn by
        // several ticks and the model reads as sliding sideways through the arc
        this.mob.yBodyRot = this.mob.getYRot();
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
     * One throttle, split between forward and climb by the slope of the line to the carrot, so the
     * mob flies at the angle the path was drawn at.
     * <p>
     * The two axes share a throttle rather than running their own controllers because
     * {@code moveRelative} normalises {@code (xxa, yya, zza)} when it is longer than 1 and scales it
     * by a flat constant. Feeding it the unit direction to the carrot puts the thrust exactly along
     * that line, and with equal drag on both axes the velocity settles along it too.
     */
    private void applyThrust(Vec3 toCarrot) {
        FlightEnvelope envelope = this.envelope();
        double throttle = this.throttleFor(envelope);
        Vec3 direction = toCarrot.normalize();
        this.mob.setSpeed((float) (throttle * direction.horizontalDistance()));
        this.mob.setYya((float) (throttle * direction.y));
    }

    /**
     * Feed forward plus a proportional nudge. {@code throttleToHold} alone would get there on its
     * own but takes an 11 tick time constant to do it, which is a full block of travel; the
     * correction term is what makes the mob actually track a profile that is changing under it.
     */
    private double throttleFor(FlightEnvelope envelope) {
        double current = this.mob.getDeltaMovement().length();
        double target = this.targetSpeed(envelope);
        double throttle = envelope.throttleToHold(target) + (target - current) * BirdFlightConfig.speedGain;
        return Mth.clamp(throttle, 0.0, envelope.maxThrottle());
    }

    /**
     * Straight off the profile at the cursor, with no lookahead of its own. The planner's backwards
     * pass already rolled every downstream limit into a braking ramp, so the value here is by
     * construction the fastest we can be and still make everything ahead of us. Reading the tightest
     * limit over a window on top of that brakes for the same corner twice, once when the planner saw
     * it and again on the approach, and the mob crawls into corners it could take at speed.
     * <p>
     * Interpolating between nodes is exact rather than approximate, which is what makes reading a
     * single point safe: {@code maxEntrySpeed} is linear in distance and so is
     * {@link ThrottleProfile#speedLimitAt}, so a braking ramp is a straight line either way.
     * <p>
     * Being over the limit needs no special case either. The correction term in
     * {@link #throttleFor} goes negative, throttle clamps to zero, and coasting is the hardest this
     * mob can brake.
     * <p>
     * With no profile this falls back to plain cruising, which means no arrival braking. That is
     * fine for the test rig and is exactly what should be visible as a difference.
     */
    private double targetSpeed(FlightEnvelope envelope) {
        double ceiling = envelope.maxSpeed() * this.speedModifier;
        ThrottleProfile profile = this.throttleProfile();
        if (profile == null || !(this.mob.getNavigation() instanceof BirdPathNavigation navigation)) {
            return ceiling;
        }
        return Math.min(ceiling, profile.speedLimitAt(navigation.getRulerCursor()));
    }

    @Nullable
    private ThrottleProfile throttleProfile() {
        return this.mob.getNavigation() instanceof BirdPathNavigation navigation
                ? navigation.getThrottleProfile() : null;
    }

    /** The one the current path was planned against, so plan and flight cannot drift apart. */
    private FlightEnvelope envelope() {
        if (this.mob.getNavigation() instanceof BirdPathNavigation navigation) {
            FlightEnvelope planned = navigation.getFlightEnvelope();
            if (planned != null) {
                return planned;
            }
        }
        return FlightEnvelope.forMob(this.mob);
    }

    /**
     * Pitch tracks where the mob is actually going rather than where the carrot is, so a mob that is
     * still drifting from the last leg does not point somewhere it is not moving. Purely visual: for
     * anything that is not elytra flying, travel() derives motion from yaw and yya only.
     */
    private void matchPitchToVelocity() {
        Vec3 velocity = this.mob.getDeltaMovement();
        float wantedPitch = (float) -(Mth.atan2(velocity.y, velocity.horizontalDistance()) * (180.0 / Math.PI));
        wantedPitch = Mth.clamp(wantedPitch, -BirdFlightConfig.maxPitch, BirdFlightConfig.maxPitch);
        this.mob.setXRot(this.rotlerp(this.mob.getXRot(), wantedPitch, BirdFlightConfig.maxPitchPerTick));
    }
}
