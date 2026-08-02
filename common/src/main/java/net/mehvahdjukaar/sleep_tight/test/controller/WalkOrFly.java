package net.mehvahdjukaar.sleep_tight.test.controller;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Walk or fly, decided once per destination, plus the two numbers it was decided on so the debug
 * tool can report them.
 * <p>
 * The cost of a route is how long it is, and the walk is then scaled by
 * {@link BirdStateConfig#walkCostMultiplier}. That is a dial, not a model: flying is always the
 * shorter line, so the multiplier is the whole of what decides how much of a detour walking is
 * allowed to be before it stops being worth it. Deliberately not a travel time, which would mean
 * copying vanilla's friction and speed arithmetic in here to produce a number nobody would tune by
 * anyway.
 * <p>
 * Both sides are measured off a real path rather than off the straight line distance, because the
 * point of the question is exactly the case where one of the two has to go round something the other
 * flies over.
 * <p>
 * Only ever asked of a mob whose feet are already down. An airborne bird flies, full stop: landing
 * in order to walk the last four blocks is not worth it, and vanilla's
 * {@code GroundPathNavigation.canUpdatePath} would refuse to produce a path anyway.
 */
public record WalkOrFly(boolean walk, @Nullable Path groundPath,
                        double walkCost, double flightCost, String reason) {

    public static WalkOrFly decide(Mob mob, PathNavigation groundNavigation,
                                   BlockPos target, @Nullable Path flightPath) {
        double flightCost = flightPath != null ? pathLength(flightPath, mob) : Double.MAX_VALUE;
        if (!(mob instanceof PerchingFlier flier) || !flier.isGrounded()) {
            return fly(flightCost, "airborne");
        }

        Vec3 away = Vec3.atBottomCenterOf(target).subtract(mob.position());
        double distance = away.horizontalDistance();
        if (distance > BirdStateConfig.walkMaxDistance) {
            return fly(flightCost, String.format("%.1f blocks out", distance));
        }
        if (Math.abs(away.y) > BirdStateConfig.walkMaxRise) {
            return fly(flightCost, String.format("%.1f blocks of rise", away.y));
        }

        // the ground search is last because it is the only expensive test here, and the two gates
        // above throw out everything it would have been asked about on a normal flight
        Path groundPath = groundNavigation.createPath(target, 0);
        if (groundPath == null || !groundPath.canReach()) {
            return fly(flightCost, "no walkable route");
        }

        double walkCost = pathLength(groundPath, mob) * BirdStateConfig.walkCostMultiplier;
        if (walkCost > flightCost) {
            return new WalkOrFly(false, null, walkCost, flightCost, "walking costs more");
        }
        return new WalkOrFly(true, groundPath, walkCost, flightCost, "walking is cheaper");
    }

    private static WalkOrFly fly(double flightCost, String reason) {
        return new WalkOrFly(false, null, Double.NaN, flightCost, reason);
    }

    /**
     * Plain polyline length through the node positions. Deliberately not the {@code FlightLine}
     * version, which lifts every node to centre a flier in its cell: the two paths have to be
     * measured the same way or the comparison is between two different rulers.
     */
    public static double pathLength(Path path, Entity entity) {
        double length = 0.0;
        for (int i = 1; i < path.getNodeCount(); i++) {
            length += path.getEntityPosAtNode(entity, i).distanceTo(path.getEntityPosAtNode(entity, i - 1));
        }
        return length;
    }
}
