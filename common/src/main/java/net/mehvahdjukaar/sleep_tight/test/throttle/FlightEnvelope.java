package net.mehvahdjukaar.sleep_tight.test.throttle;

import net.mehvahdjukaar.sleep_tight.test.controller.BirdFlightConfig;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;

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
        double maxThrottle,
        double corridorMargin,
        double maxClimbAngle,
        double hoverSpeedFraction
) {

    private static final int MAX_SIMULATED_TICKS = 512;

    /**
     * Reads the mob's attributes and the live config. FLYING_SPEED is treated as a throttle
     * fraction rather than a speed, matching what {@code BirdMoveControl} currently does with it:
     * it scales the acceleration, and the top speed follows from that against drag.
     */
    public static FlightEnvelope forMob(Mob mob) {
        double drag = BirdFlightConfig.airDrag;
        double throttleCap = Mth.clamp(mob.getAttributeValue(Attributes.FLYING_SPEED), 0.0, 1.0);
        double accel = BirdFlightConfig.maxThrustAccel * throttleCap;
        double maxSpeed = terminalSpeed(accel, drag);
        return new FlightEnvelope(
                BirdFlightConfig.maxYawPerTick * Mth.DEG_TO_RAD,
                maxSpeed,
                maxSpeed * BirdFlightConfig.cruiseFraction,
                maxSpeed * BirdFlightConfig.minSpeedFraction,
                BirdFlightConfig.arrivalSpeed,
                drag,
                BirdFlightConfig.brakingDrag,
                accel,
                throttleCap,
                BirdFlightConfig.corridorMargin,
                BirdFlightConfig.maxClimbAngle * Mth.DEG_TO_RAD,
                BirdFlightConfig.hoverSpeedFraction);
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
     * The fastest we can be after covering {@code distance} from {@code entrySpeed} at full thrust,
     * and the whole of the planner's forwards pass. Simulated rather than solved: the closed form
     * for accelerating against drag is ugly and this runs once per path leg, not per tick. Tick
     * order matches {@code LivingEntity.travel}, which adds thrust, then moves, then applies drag.
     */
    public double speedAfterAccelerating(double entrySpeed, double distance) {
        if (this.accelPerTick <= 0.0) {
            return entrySpeed;
        }
        double speed = entrySpeed;
        double covered = 0.0;
        for (int tick = 0; tick < MAX_SIMULATED_TICKS && covered < distance; tick++) {
            double moving = Math.min(this.maxSpeed, speed + this.accelPerTick);
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
     * The throttle that settles at this speed, i.e. what to feed {@code Mob.setSpeed} to hold it.
     * Linear, because terminal speed is proportional to acceleration and acceleration is
     * proportional to throttle: at {@link #maxThrottle} the mob settles at {@link #maxSpeed}.
     * <p>
     * This is the follower's feed-forward term. It gets there eventually on its own, an 11 tick time
     * constant at vanilla drag, so a controller wanting to arrive sooner adds a correction on top.
     */
    public double throttleToHold(double speed) {
        return this.maxSpeed <= 1.0E-9 ? 0.0 : this.maxThrottle * speed / this.maxSpeed;
    }
}
