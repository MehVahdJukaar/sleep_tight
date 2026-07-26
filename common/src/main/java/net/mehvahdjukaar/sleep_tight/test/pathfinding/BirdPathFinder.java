package net.mehvahdjukaar.sleep_tight.test.pathfinding;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import net.minecraft.core.BlockPos;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.profiling.metrics.MetricCategory;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.pathfinder.BinaryHeap;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.Target;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Vanilla PathFinder's A* loop with two changes for the bird lattice: the edge cost between
 * states (turning, purely vertical flight) is added to g, and the heuristic weight comes from
 * {@link BirdPathfindingConfig} instead of the hardcoded 1.5.
 */
public class BirdPathFinder extends PathFinder {

    private final Node[] neighbors = new Node[32];
    private final int maxVisitedNodes;
    private final BirdNodeEvaluator nodeEvaluator;
    private final BinaryHeap openSet = new BinaryHeap();

    public BirdPathFinder(BirdNodeEvaluator nodeEvaluator, int maxVisitedNodes) {
        super(nodeEvaluator, maxVisitedNodes);
        this.nodeEvaluator = nodeEvaluator;
        this.maxVisitedNodes = maxVisitedNodes;
    }

    @Override
    @Nullable
    public Path findPath(PathNavigationRegion region, Mob mob, Set<BlockPos> targetPositions, float maxRange, int accuracy, float searchDepthMultiplier) {
        this.openSet.clear();
        this.nodeEvaluator.prepare(region, mob);
        Node start = this.nodeEvaluator.getStart();
        if (start == null) {
            return null;
        }
        Map<Target, BlockPos> targetMap = targetPositions.stream().collect(Collectors.toMap(
                pos -> this.nodeEvaluator.getTarget(pos.getX(), pos.getY(), pos.getZ()), Function.identity()));
        Path path = this.findLatticePath(region.getProfiler(), start, targetMap, maxRange, accuracy, searchDepthMultiplier);
        this.nodeEvaluator.done();
        return path;
    }

    @Nullable
    private Path findLatticePath(ProfilerFiller profiler, Node from, Map<Target, BlockPos> targetMap, float maxRange, int accuracy, float searchDepthMultiplier) {
        profiler.push("find_path");
        profiler.markForCharting(MetricCategory.PATH_FINDING);
        Set<Target> targets = targetMap.keySet();
        from.g = 0.0F;
        from.h = this.getBestH(from, targets);
        from.f = from.h;
        this.openSet.clear();
        this.openSet.insert(from);
        int visited = 0;
        Set<Target> reachedTargets = Sets.newHashSetWithExpectedSize(targets.size());
        int maxVisited = (int) (this.maxVisitedNodes * searchDepthMultiplier);

        while (!this.openSet.isEmpty()) {
            if (++visited >= maxVisited) {
                break;
            }

            Node current = this.openSet.pop();
            current.closed = true;

            for (Target target : targets) {
                if (current.distanceManhattan(target) <= accuracy) {
                    target.setReached();
                    reachedTargets.add(target);
                }
            }

            if (!reachedTargets.isEmpty()) {
                break;
            }

            if (current.distanceTo(from) < maxRange) {
                int neighborCount = this.nodeEvaluator.getNeighbors(this.neighbors, current);

                for (int i = 0; i < neighborCount; i++) {
                    Node neighbor = this.neighbors[i];
                    float stepDistance = this.distance(current, neighbor);
                    neighbor.walkedDistance = current.walkedDistance + stepDistance;
                    float tentativeG = current.g + stepDistance + neighbor.costMalus
                            + this.nodeEvaluator.getEdgeCost(current, neighbor);
                    if (neighbor.walkedDistance < maxRange && (!neighbor.inOpenSet() || tentativeG < neighbor.g)) {
                        neighbor.cameFrom = current;
                        neighbor.g = tentativeG;
                        neighbor.h = this.getBestH(neighbor, targets) * BirdPathfindingConfig.heuristicWeight;
                        if (neighbor.inOpenSet()) {
                            this.openSet.changeCost(neighbor, neighbor.g + neighbor.h);
                        } else {
                            neighbor.f = neighbor.g + neighbor.h;
                            this.openSet.insert(neighbor);
                        }
                    }
                }
            }
        }

        Optional<Path> best = !reachedTargets.isEmpty()
                ? reachedTargets.stream()
                .map(target -> this.reconstructPath(target.getBestNode(), targetMap.get(target), true))
                .min(Comparator.comparingInt(Path::getNodeCount))
                : targets.stream()
                .map(target -> this.reconstructPath(target.getBestNode(), targetMap.get(target), false))
                .min(Comparator.comparingDouble(Path::getDistToTarget).thenComparingInt(Path::getNodeCount));
        profiler.pop();
        return best.orElse(null);
    }

    private float getBestH(Node node, Set<Target> targets) {
        float best = Float.MAX_VALUE;
        for (Target target : targets) {
            float h = node.distanceTo(target);
            target.updateBest(h, node);
            best = Math.min(h, best);
        }
        return best;
    }

    private Path reconstructPath(Node end, BlockPos targetPos, boolean reachesTarget) {
        List<Node> nodes = Lists.newArrayList();
        Node node = end;
        nodes.add(0, end);
        while (node.cameFrom != null) {
            node = node.cameFrom;
            nodes.add(0, node);
        }
        return new Path(nodes, targetPos, reachesTarget);
    }
}
