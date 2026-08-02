package net.mehvahdjukaar.sleep_tight.test;

import net.mehvahdjukaar.sleep_tight.test.controller.GaitChoice;
import net.mehvahdjukaar.sleep_tight.test.navigator.BirdPathNavigation;
import net.mehvahdjukaar.sleep_tight.test.pathfinding.BirdNodeEvaluator;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.mehvahdjukaar.sleep_tight.test.debug.PathDebugPackets;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.pathfinder.FlyNodeEvaluator;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;

/**
 * Right click a block with a ghast tear to make the nearest {@link BirdTestMob} path to the block
 * above it, draw the result, travel it slowly, and report what the search cost against plain vanilla
 * flying A* on the same query. Short hops may come out as a walk rather than a flight, in which case
 * what is drawn is the ground path the mob chose; the report says which and why.
 * <p>
 * Drawing goes through our own debug channel in {@link net.mehvahdjukaar.sleep_tight.test.debug},
 * so it works on a plain client with nothing enabled. Vanilla's equivalent cannot: its DebugPackets
 * bodies are empty stubs in the release jar and its DebugRenderer never calls its own pathfinding
 * renderer.
 */
public class BirdDebug {

    private static final double MOB_SEARCH_RADIUS = 64;
    // how close a node has to get to the target to count as reached; 0 means "exactly there"
    private static final int ACCURACY = 0;

    public static void onDebugToolUse(ServerLevel level, Player player, BlockPos clickedPos) {
        BlockPos target = clickedPos.above();
        BirdTestMob mob = findNearestTestMob(level, player, clickedPos);
        if (mob == null) {
            feedback(player, "No test mob within " + (int) MOB_SEARCH_RADIUS + " blocks", ChatFormatting.RED);
            return;
        }

        // throwaway run first: it warms the chunk and path type caches both searches read, so
        // whichever went second wouldn't just look faster for it
        baselineNavigation(mob).createPath(target, ACCURACY);

        SearchRun lattice = runLattice(mob, target);
        SearchRun vanilla = runVanillaBaseline(mob, target);

        // the mob may decide the hop is not worth flying and walk it instead, in which case the path
        // that gets drawn is the ground one it picked rather than the lattice one measured above
        GaitChoice choice = mob.travelTo(target, lattice.path);
        Path travelled = choice.walk() ? choice.groundPath() : lattice.path;
        if (travelled == null) {
            feedback(player, "No path to " + target.toShortString(), ChatFormatting.RED);
            return;
        }

        broadcastPath(mob, travelled);
        report(player, travelled, lattice, vanilla, choice);
    }

    /**
     * Pushes a path to every player's debug renderer. Resent while the mob flies so the highlighted
     * next node keeps up with it; the client drops each entry a minute after it last arrived.
     */
    public static void broadcastPath(BirdTestMob mob, Path path) {
        PathDebugPackets.sendPathFindingPacket(mob, path, mob.getBbWidth() / 2);
    }

    private static SearchRun runLattice(BirdTestMob mob, BlockPos target) {
        // deliberately not getNavigation(): that is vanilla's walker while the bird is on foot
        BirdPathNavigation navigation = mob.getFlightNavigation();
        BirdNodeEvaluator evaluator = (BirdNodeEvaluator) navigation.getNodeEvaluator();
        long start = System.nanoTime();
        Path path = navigation.createPath(target, ACCURACY);
        return new SearchRun(path, System.nanoTime() - start, evaluator.expansions, evaluator.generatedNeighbors);
    }

    private static SearchRun runVanillaBaseline(BirdTestMob mob, BlockPos target) {
        BaselineNavigation navigation = baselineNavigation(mob);
        long start = System.nanoTime();
        Path path = navigation.createPath(target, ACCURACY);
        return new SearchRun(path, System.nanoTime() - start,
                navigation.evaluator.expansions, navigation.evaluator.generatedNeighbors);
    }

    private static BaselineNavigation baselineNavigation(BirdTestMob mob) {
        return new BaselineNavigation(mob, mob.level());
    }

    private static void report(Player player, Path path, SearchRun lattice, SearchRun vanilla,
                               GaitChoice choice) {
        feedback(player, String.format("%d nodes, %s (dist %.1f)", path.getNodeCount(),
                        path.canReach() ? "reached" : "closest approach", path.getDistToTarget()),
                path.canReach() ? ChatFormatting.GREEN : ChatFormatting.YELLOW);

        player.sendSystemMessage(Component.literal(String.format("%s: %s (walk %s, fly %s)",
                        choice.walk() ? "WALK" : "FLY", choice.reason(),
                        ticks(choice.walkTicks()), ticks(choice.flightTicks())))
                .withStyle(choice.walk() ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.WHITE));

        player.sendSystemMessage(Component.literal(String.format(
                        "lattice: %d expanded, %d generated, %.2f ms", lattice.expansions,
                        lattice.generated, lattice.millis()))
                .withStyle(ChatFormatting.AQUA));
        player.sendSystemMessage(Component.literal(String.format(
                        "vanilla A*: %d expanded, %d generated, %.2f ms%s", vanilla.expansions,
                        vanilla.generated, vanilla.millis(), vanilla.path == null ? " (no path)" : ""))
                .withStyle(ChatFormatting.GRAY));
        player.sendSystemMessage(Component.literal(String.format(
                        "cost: %.2fx expanded, %.2fx time", ratio(lattice.expansions, vanilla.expansions),
                        ratio(lattice.nanos, vanilla.nanos)))
                .withStyle(ChatFormatting.GOLD));
    }

    private static double ratio(double lattice, double vanilla) {
        return vanilla <= 0 ? Double.NaN : lattice / vanilla;
    }

    /** An estimate that was never worked out prints as a dash rather than as NaN or a huge number. */
    private static String ticks(double estimate) {
        return Double.isNaN(estimate) || estimate >= Double.MAX_VALUE
                ? "-" : String.format("%.0ft", estimate);
    }

    @Nullable
    private static BirdTestMob findNearestTestMob(ServerLevel level, Player player, BlockPos clickedPos) {
        // measured from the player, so you pick which mob to drive by walking up to it
        BlockPos center = player.blockPosition();
        AABB box = new AABB(center).inflate(MOB_SEARCH_RADIUS);
        List<BirdTestMob> mobs = level.getEntitiesOfClass(BirdTestMob.class, box);
        return mobs.stream().min(Comparator.comparingDouble(m -> m.distanceToSqr(
                center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5))).orElse(null);
    }

    private static void feedback(Player player, String message, ChatFormatting color) {
        player.displayClientMessage(Component.literal(message).withStyle(color), true);
    }

    private record SearchRun(@Nullable Path path, long nanos, int expansions, int generated) {
        double millis() {
            return this.nanos / 1_000_000.0;
        }
    }

    /**
     * Plain vanilla flying navigation, built fresh per query so it never carries state between
     * runs. Mirrors BirdPathNavigation exactly apart from the evaluator and finder, so the two
     * searches see the same range, node budget and door rules.
     */
    private static class BaselineNavigation extends FlyingPathNavigation {
        private CountingFlyNodeEvaluator evaluator;

        BaselineNavigation(Mob mob, Level level) {
            super(mob, level);
        }

        @Override
        protected PathFinder createPathFinder(int maxVisitedNodes) {
            this.evaluator = new CountingFlyNodeEvaluator();
            this.nodeEvaluator = this.evaluator;
            this.nodeEvaluator.setCanPassDoors(true);
            return new PathFinder(this.nodeEvaluator, maxVisitedNodes);
        }
    }

    /** Counts the same two things BirdNodeEvaluator does, so the numbers are comparable. */
    private static class CountingFlyNodeEvaluator extends FlyNodeEvaluator {
        int expansions;
        int generatedNeighbors;

        @Override
        public void prepare(PathNavigationRegion level, Mob mob) {
            super.prepare(level, mob);
            this.expansions = 0;
            this.generatedNeighbors = 0;
        }

        @Override
        public int getNeighbors(Node[] outputArray, Node node) {
            int count = super.getNeighbors(outputArray, node);
            this.expansions++;
            this.generatedNeighbors += count;
            return count;
        }
    }
}
