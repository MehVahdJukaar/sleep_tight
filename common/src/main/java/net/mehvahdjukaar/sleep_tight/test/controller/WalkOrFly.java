package net.mehvahdjukaar.sleep_tight.test.controller;

import net.mehvahdjukaar.sleep_tight.test.throttle.FlightEnvelope;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Walk or fly, decided once per destination, plus the numbers it was decided on so the debug tool
 * can report them.
 * <p>
 * The comparison is in ticks, which is the only currency a walker and a flier share. Both sides are
 * measured off a real path rather than off the straight line distance, because the point of the
 * question is exactly the case where one of the two has to go round something the other flies over.
 * <p>
 * Only ever asked from a mob whose feet are already down. An airborne bird flies, full stop: landing
 * in order to walk the last four blocks costs more than flying them, and vanilla's
 * {@code GroundPathNavigation.canUpdatePath} would refuse to produce a path anyway.
 */
public record WalkOrFly(boolean walk, @Nullable Path groundPath,
                         double walkTicks, double flightTicks, String reason) {

    /** Vanilla's default block friction, which is the drag a walker settles its speed against. */
    private static final double DEFAULT_FRICTION = 0.6;

    // the friction the speed attribute is calibrated against, from LivingEntity.getFrictionInfluencedSpeed.
    // Being DEFAULT_FRICTION cubed is the whole point: on ordinary ground the correction is exactly 1
    private static final double FRICTION_REFERENCE = 0.21600002;

    public static WalkOrFly decide(Mob mob, PathNavigation groundNavigation,
                                    BlockPos target, @Nullable Path flightPath) {
        double flightTicks = flightTicks(mob, flightPath);
        if (!(mob instanceof PerchingFlier flier) || !flier.isGrounded()) {
            return fly(flightTicks, "airborne");
        }

        Vec3 away = Vec3.atBottomCenterOf(target).subtract(mob.position());
        double distance = away.horizontalDistance();
        if (distance > BirdGroundConfig.walkMaxDistance) {
            return fly(flightTicks, String.format("%.1f blocks out", distance));
        }
        if (Math.abs(away.y) > BirdGroundConfig.walkMaxRise) {
            return fly(flightTicks, String.format("%.1f blocks of rise", away.y));
        }

        // the ground search is last because it is the only expensive test here, and the two gates
        // above throw out everything it would have been asked about on a normal flight
        Path groundPath = groundNavigation.createPath(target, 0);
        if (groundPath == null || !groundPath.canReach()) {
            return fly(flightTicks, "no walkable route");
        }

        double walkTicks = walkTicks(mob, groundPath);
        if (walkTicks > flightTicks) {
            return new WalkOrFly(false, null, walkTicks, flightTicks, "walking costs more");
        }
        return new WalkOrFly(true, groundPath, walkTicks, flightTicks, "walking is cheaper");
    }

    private static WalkOrFly fly(double flightTicks, String reason) {
        return new WalkOrFly(false, null, Double.NaN, flightTicks, reason);
    }

    private static double walkTicks(Mob mob, Path groundPath) {
        double speed = walkSpeed(mob);
        if (speed <= 1.0E-6) {
            return Double.MAX_VALUE;
        }
        return pathLength(groundPath, mob) / speed * BirdGroundConfig.walkCostPenalty;
    }

    /** Cruising the whole line, plus the fixed cost of getting off the ground and back onto it. */
    private static double flightTicks(Mob mob, @Nullable Path flightPath) {
        if (flightPath == null) {
            return Double.MAX_VALUE;
        }
        double speed = flightSpeed(mob);
        if (speed <= 1.0E-6) {
            return Double.MAX_VALUE;
        }
        return pathLength(flightPath, mob) / speed + BirdGroundConfig.takeoffCostTicks;
    }

    /**
     * Ground covered per tick on foot, which is neither the attribute nor the terminal velocity.
     * <p>
     * Two things make it not the attribute. {@code Mob.setSpeed} writes the attribute into
     * {@code zza} <i>and</i> into {@code speed}, and {@code travel} then scales that input vector by
     * {@code getFrictionInfluencedSpeed}, which is the same number again at vanilla's 0.6 friction
     * (the {@code 0.216/f^3} term is exactly 1 there). {@code getInputVector} only normalises an
     * input longer than 1, and a walk speed never is, so both survive: the acceleration is the
     * attribute <b>squared</b>. And it is the distance per tick that a travel time needs, not the
     * speed the mob settles at, because {@code move()} runs before drag is applied, so the mob
     * covers {@code v + a} each tick, which at equilibrium is {@code a / (1 - drag)}.
     * <p>
     * At MOVEMENT_SPEED 0.25 that is 0.138 blocks a tick, 2.8 a second. The friction is assumed
     * rather than sampled: the mob is about to walk over ground it has not covered yet, and the
     * answer only feeds a comparison.
     */
    private static double walkSpeed(Mob mob) {
        double input = mob.getAttributeValue(Attributes.MOVEMENT_SPEED) * BirdGroundConfig.walkSpeedModifier;
        double accel = input * input * (FRICTION_REFERENCE / (DEFAULT_FRICTION * DEFAULT_FRICTION * DEFAULT_FRICTION));
        return accel / (1.0 - DEFAULT_FRICTION * BirdFlightConfig.airDrag);
    }

    /**
     * The same measure in the air, so the two are comparable: cruising velocity is what the profile
     * plans in, and the ground covered while holding it is that over the drag, by the same
     * {@code v + a} argument as above.
     * <p>
     * Worth knowing when reading the answers this produces: at vanilla's hardcoded 0.02
     * {@code getFlyingSpeed}, cruise is 0.178 blocks a tick against the walk's 0.138, so flying is
     * only about a third quicker than walking and the takeoff cost decides most short hops on its
     * own. Give the mob a real {@code getFlyingSpeed} override and that gap opens up as it should.
     */
    private static double flightSpeed(Mob mob) {
        FlightEnvelope envelope = FlightEnvelope.forMob(mob);
        return envelope.drag() <= 0.0 ? envelope.cruiseSpeed() : envelope.cruiseSpeed() / envelope.drag();
    }

    /**
     * Plain polyline length through the node positions. Deliberately not the
     * {@code FlightLine} version, which lifts every node to centre a flier in its cell: the two
     * paths have to be measured the same way or the comparison is between two different rulers, and
     * that lift is worth a fraction of a block on a hop this short anyway.
     */
    public static double pathLength(Path path, Entity entity) {
        double length = 0.0;
        for (int i = 1; i < path.getNodeCount(); i++) {
            length += path.getEntityPosAtNode(entity, i).distanceTo(path.getEntityPosAtNode(entity, i - 1));
        }
        return length;
    }
}
