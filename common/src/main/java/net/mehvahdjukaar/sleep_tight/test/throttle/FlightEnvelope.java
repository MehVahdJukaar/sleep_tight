package net.mehvahdjukaar.sleep_tight.test.throttle;

import net.mehvahdjukaar.sleep_tight.test.controller.BirdFlightConfig;
import net.mehvahdjukaar.sleep_tight.test.pathfinding.FlightLine;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

/**
 * Everything about what a bird can physically do, in one place. The search reads it to price turns,
 * {@link ThrottlePlanner} reads it to work out speed limits, and the move control reads it to steer.
 * Before this existed the same facts were spread across two config classes with nothing linking
 * them, which is how the planner ended up drawing corners the follower could not fly.
 * <p>
 * Immutable, and snapshotted from the live config once per path, so a path is always flown against
 * the numbers it was planned with even if the config is poked mid-flight.
 * <p>
 * Speeds are blocks per tick, angles radians, drag is the multiplier applied to velocity each tick.
 */
public record FlightEnvelope(
        double maxYawRate,
        double maxSpeed,
        double cruiseSpeed,
        double minSpeed,
        double arrivalSpeed,
        double drag,
        double brakingDrag,
        double accelPerTick,
        double peakThrust,
        double maxThrottle,
        double corridorMargin,
        double verticalCorridorMargin,
        double maxClimbAngle,
        double hoverSpeedFraction
) {

    private static final int MAX_SIMULATED_TICKS = 512;

    /**
     * Reads the mob's attributes and the live config. FLYING_SPEED is treated as a throttle
     * fraction rather than a speed, matching what {@code BirdFlightControl} currently does with it:
     * it scales the acceleration, and the top speed follows from that against drag.
     */
    public static FlightEnvelope forMob(Mob mob) {
        double drag = BirdFlightConfig.airDrag;
        double throttleCap = Mth.clamp(mob.getAttributeValue(Attributes.FLYING_SPEED), 0.0, 1.0);
        double accel = BirdFlightConfig.maxThrustAccel * throttleCap;
        double maxSpeed = terminalSpeed(accel, drag);
        return new FlightEnvelope(
                yawRateFor(maxSpeed),
                maxSpeed,
                maxSpeed * BirdFlightConfig.cruiseFraction,
                maxSpeed * BirdFlightConfig.minSpeedFraction,
                BirdFlightConfig.arrivalSpeed,
                drag,
                BirdFlightConfig.brakingDrag,
                accel,
                accel * Math.max(1.0, BirdFlightConfig.peakThrustFactor),
                throttleCap,
                BirdFlightConfig.corridorMargin,
                // measured off the mob rather than configured, because it is not a preference: it is
                // exactly the room left over once the mob is centred in the cells the search tested
                FlightLine.verticalNodeOffset(mob),
                BirdFlightConfig.maxClimbAngle * Mth.DEG_TO_RAD,
                BirdFlightConfig.hoverSpeedFraction);
    }

    /**
     * The yaw rate that traces {@link BirdFlightConfig#turnRadius} at top speed, which is the only
     * place a turn rate is allowed to come from. Deriving it rather than configuring it is what
     * keeps the corners the planner draws flyable at any speed: radius is {@code speed / yawRate},
     * so pinning the rate and moving the speed widens every arc, and a bird given a faster attribute
     * quietly stops fitting through the lattice it is being handed.
     * <p>
     * Held constant for the whole path rather than recomputed per tick, so the planner's corner rule
     * and the follower's steering are the same number by construction. A consequence worth knowing:
     * below top speed the bird turns tighter than the configured radius, which is the right way
     * round - slow birds should be nimble.
     */
    private static double yawRateFor(double maxSpeed) {
        double ceiling = BirdFlightConfig.maxYawPerTickCeiling * Mth.DEG_TO_RAD;
        if (BirdFlightConfig.turnRadius <= 1.0E-6) {
            return ceiling;
        }
        return Math.min(ceiling, maxSpeed / BirdFlightConfig.turnRadius);
    }

    /**
     * The speed thrust and drag settle at: {@code v = drag * (v + accel)} solved for v. Nothing can
     * go faster than this in level flight, which is why it doubles as {@link #maxSpeed}.
     */
    public static double terminalSpeed(double accel, double drag) {
        return drag >= 1.0 ? Double.MAX_VALUE : accel * drag / (1.0 - drag);
    }

    /**
     * How far the mob still travels after thrust is cut. Velocity decays geometrically, so the
     * whole remaining distance is a closed form and no simulation is needed.
     */
    public double stoppingDistance(double speed) {
        return speed / (1.0 - this.brakingDrag);
    }

    /**
     * The fastest we may be entering a stretch of {@code distance} blocks and still be down to
     * {@code exitSpeed} by the end of it. The inverse of {@link #stoppingDistance}, and the whole of
     * the planner's backwards pass. At vanilla's 0.91 this works out to 0.09 blocks per tick shed
     * per block flown, which is slow enough that a corner has to be seen a block or more out.
     */
    public double maxEntrySpeed(double exitSpeed, double distance) {
        return exitSpeed + distance * (1.0 - this.brakingDrag);
    }

    /**
     * The hardest the wings can push, before the mob's own throttle cap. This is what
     * {@code LivingEntity.getFlyingSpeed} has to hand back for a burst to actually arrive: vanilla
     * hardcodes it to {@link BirdFlightConfig#maxThrustAccel}, which is the *sustained* figure, and
     * thrust that cannot exceed the sustained figure is not a burst.
     */
    public static double wingPeakThrust() {
        return BirdFlightConfig.maxThrustAccel * Math.max(1.0, BirdFlightConfig.peakThrustFactor);
    }

    /**
     * The most thrust the wings will put out at this speed: {@link #peakThrust} from a standstill,
     * decaying to {@link #accelPerTick} at top speed.
     * <p>
     * This is the takeoff burst, and the reason it is a ramp rather than a flat higher number is
     * that top speed is *defined* by the sustained figure - terminal speed is where thrust and drag
     * balance, so a cap still above sustained up there would simply move where the bird settles and
     * every planner number derived from top speed with it. Ending the ramp exactly on sustained
     * leaves all of them where they were, and gives away nothing: a bird already at speed has no use
     * for a burst, and one starting from nothing has nothing but.
     */
    public double thrustCapAt(double speed) {
        double towardsTop = this.maxSpeed <= 0.0 ? 1.0 : Mth.clamp(speed / this.maxSpeed, 0.0, 1.0);
        return Mth.lerp(towardsTop, this.peakThrust, this.accelPerTick);
    }

    /**
     * How hard the wings are working, 0 through 1, as a share of what they can put out flat out.
     * Static and off the live config rather than off a snapshot, because the only thing that reads
     * it is the model: a bird bursting off a perch is at 1 and beating fast, one holding cruise is
     * well under it, one coasting into a perch is at 0. Nothing physical depends on the answer.
     */
    public static double wingEffortFor(double thrust) {
        double peak = wingPeakThrust();
        return peak <= 1.0E-9 ? 0.0 : Mth.clamp(thrust / peak, 0.0, 1.0);
    }

    /**
     * The fastest we can be after covering {@code distance} from {@code entrySpeed} at full thrust,
     * and the whole of the planner's forwards pass. Simulated rather than solved: the closed form
     * for accelerating against drag is ugly and this runs once per path leg, not per tick. Tick
     * order matches {@code LivingEntity.travel}, which adds thrust, then moves, then applies drag.
     * <p>
     * Only the post-drag velocity is capped at {@link #maxSpeed}. Capping the thrust-added value too
     * would make the loop settle at {@code drag * maxSpeed}, so a dead straight path would never be
     * allowed within 9% of top speed and every node on it would come out acceleration limited.
     * <p>
     * The takeoff burst is deliberately not modelled here: this walks at {@link #accelPerTick}
     * throughout, so a leg the bird enters slowly is priced as if it had no burst to spend. That
     * makes the profile pessimistic about acceleration, which is the safe direction - the follower
     * simply reaches the allowed speed sooner than planned and then holds it.
     */
    public double speedAfterAccelerating(double entrySpeed, double distance) {
        if (this.accelPerTick <= 0.0) {
            return entrySpeed;
        }
        double speed = entrySpeed;
        double covered = 0.0;
        for (int tick = 0; tick < MAX_SIMULATED_TICKS && covered < distance; tick++) {
            double moving = speed + this.accelPerTick;
            covered += moving;
            speed = Math.min(this.maxSpeed, this.drag * moving);
        }
        return speed;
    }

    /**
     * Radius of the arc the mob traces turning at full rate at this speed. The number the planner
     * and the follower have to agree on: everything else about tight corners follows from it.
     */
    public double turnRadiusAt(double speed) {
        return this.maxYawRate <= 0.0 ? Double.MAX_VALUE : speed / this.maxYawRate;
    }

    /**
     * The fastest a pitch change may be flown while bulging no more than {@code margin} off the
     * drawn line, given how far that shape of bend throws the arc per block of radius.
     * <p>
     * The same formula as the horizontal corner rule, and it did not used to be. While vertical
     * velocity was left to thrust alone the climb angle came round at {@code accel/speed} instead of
     * at a fixed rate, making the radius {@code speed^2/accel} and this a square root. Now that
     * {@code BirdFlightControl.turnVelocityPitch} steers the velocity's pitch the way the yaw half
     * steers its heading, both planes turn at {@link #maxYawRate} and a turn is priced the same way
     * whichever one it happens in. The margins still differ - sideways is a tuned allowance, up and
     * down is the measured room inside the certified cells - which is why this still takes one.
     */
    public double maxSpeedForPitchChange(double margin, double overshootPerRadius) {
        if (overshootPerRadius <= 1.0E-9) {
            return Double.MAX_VALUE;
        }
        return this.maxYawRate * Math.max(0.0, margin) / overshootPerRadius;
    }

    /**
     * Trims an offset from the drawn line to what the corridor around it can actually take: the flat
     * {@link #corridorMargin} sideways and {@link #verticalCorridorMargin} up or down. The two are
     * separate because the horizontal one is a tuned allowance that counts on neighbouring cells
     * usually being free, while the vertical one is the measured room inside the cells the search
     * actually certified. The horizontal part is scaled rather than clamped per axis, so trimming it
     * never swings the direction round.
     */
    public Vec3 clampToCorridor(Vec3 offset) {
        double horizontal = offset.horizontalDistance();
        double scale = horizontal > this.corridorMargin ? this.corridorMargin / horizontal : 1.0;
        return new Vec3(offset.x * scale,
                Mth.clamp(offset.y, -this.verticalCorridorMargin, this.verticalCorridorMargin),
                offset.z * scale);
    }

    /**
     * The thrust that lands exactly on {@code targetSpeed} after this tick's drag, given where the
     * speed is now. {@code travel()} does {@code v' = drag * (v + a)}, so {@code a = target/drag - v}
     * hits the target on the nose and, since {@code |v'| <= drag * (|v| + a)}, can never leave the
     * mob above it.
     * <p>
     * This replaces the old feed-forward-plus-gain pair. It is the same controller with the gain
     * pinned to the one value the physics actually implies rather than a tuned one: at the target it
     * reduces to the throttle that holds it, and away from it asks for exactly the difference,
     * clamped by what the wings can deliver <i>at this speed</i>, see {@link #thrustCapAt}. Nothing
     * to tune and nothing to hunt.
     */
    public double thrustToReach(double targetSpeed, double currentSpeed) {
        if (this.drag <= 0.0) {
            return this.accelPerTick;
        }
        return Mth.clamp(targetSpeed / this.drag - currentSpeed, 0.0, this.thrustCapAt(currentSpeed));
    }

    /**
     * The throttle input that delivers this much thrust, i.e. what to feed {@code Mob.setSpeed} and
     * {@code setYya}. Linear: {@code moveRelative} scales the input vector by a flat constant, and
     * {@link #maxThrottle} buys {@link #peakThrust} rather than {@link #accelPerTick}, because
     * anything over full throttle is silently normalised away by {@code moveRelative} and a burst
     * has to fit under the ceiling to survive.
     */
    public double throttleForThrust(double thrust) {
        return this.peakThrust <= 1.0E-9 ? 0.0 : this.maxThrottle * thrust / this.peakThrust;
    }
}
