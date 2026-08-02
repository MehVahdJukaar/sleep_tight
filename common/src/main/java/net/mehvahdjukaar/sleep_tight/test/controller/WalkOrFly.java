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
    private static final float DEFAULT_FRICTION = 0.6F;

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
        double cruise = FlightEnvelope.forMob(mob).cruiseSpeed();
        if (cruise <= 1.0E-6) {
            return Double.MAX_VALUE;
        }
        return pathLength(flightPath, mob) / cruise + BirdGroundConfig.takeoffCostTicks;
    }

    /**
     * The speed a walker settles at, worked out the same way {@link FlightEnvelope#maxSpeed()} is: the
     * attribute is an acceleration per tick, not a speed, and what it settles at follows from the
     * drag it is fighting. On the ground that drag is the block's friction times the same air drag
     * the flier pays, so at vanilla's 0.6 a MOVEMENT_SPEED of 0.25 comes out around 0.3 blocks a
     * tick, comfortably faster than this bird cruises. Which is the whole reason
     * {@link BirdGroundConfig#walkCostPenalty} exists.
     * <p>
     * The friction is assumed rather than sampled: the mob is about to walk over ground it has not
     * pathed across yet, and the answer only feeds a comparison.
     */
    private static double walkSpeed(Mob mob) {
        return FlightEnvelope.terminalSpeed(mob.getAttributeValue(Attributes.MOVEMENT_SPEED),
                DEFAULT_FRICTION * BirdFlightConfig.airDrag);
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
