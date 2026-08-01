package net.mehvahdjukaar.sleep_tight.test.pathfinding;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Vanilla PathFinder's A* loop with three changes for the bird lattice: the edge cost between
 * states (turning, purely vertical flight) is added to g, the heuristic weight comes from
 * {@link BirdPathfindingConfig} instead of the hardcoded 1.5, and the node a path is reconstructed
 * from is chosen by the search rather than by {@code Target.updateBest}, see
 * {@link #nominateEndpoint}.
 */
public class BirdPathFinder extends PathFinder {

    private final Node[] neighbors = new Node[32];
    private final int maxVisitedNodes;
    private final BirdNodeEvaluator nodeEvaluator;
    private final BinaryHeap openSet = new BinaryHeap();
    private List<ConsideredMove> consideredMoves = List.of();

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
        // has to happen before done(), which drops the cell caches offeredMoves reads
        this.consideredMoves = path != null && BirdPathfindingConfig.collectConsideredMoves
                ? this.collectConsideredMoves(path) : List.of();
        this.nodeEvaluator.done();
        return path;
    }

    /**
     * The moves offered at each node of the finished path, which is what the search chose between
     * on its way through. Only the winner survives in the path itself, so without this the one
     * question the drawn path cannot answer is why it went that way and not another.
     */
    private List<ConsideredMove> collectConsideredMoves(Path path) {
        List<ConsideredMove> moves = new ArrayList<>();
        for (int i = 0; i < path.getNodeCount(); i++) {
            Node from = path.getNode(i);
            for (Node to : this.nodeEvaluator.offeredMoves(from)) {
                moves.add(new ConsideredMove(from, to));
            }
        }
        return moves;
    }

    /** Empty unless {@link BirdPathfindingConfig#collectConsideredMoves} was on for the last search. */
    public List<ConsideredMove> getConsideredMoves() {
        return this.consideredMoves;
    }

    /** One step the search had available at a path node, taken or not. */
    public record ConsideredMove(Node from, Node to) {
    }

    @Nullable
    private Path findLatticePath(ProfilerFiller profiler, Node from, Map<Target, BlockPos> targetMap, float maxRange, int accuracy, float searchDepthMultiplier) {
        profiler.push("find_path");
        profiler.markForCharting(MetricCategory.PATH_FINDING);
        Set<Target> targets = targetMap.keySet();
        from.g = 0.0F;
        // weighted like every other node's, rather than vanilla's unweighted start. Only the first
        // pop is affected, but an f on a different scale to the rest of the heap is a trap
        from.h = this.getBestH(from, targets) * BirdPathfindingConfig.heuristicWeight;
        from.f = from.h;
        this.openSet.clear();
        this.openSet.insert(from);
        int visited = 0;
        Set<Target> reachedTargets = Sets.newHashSetWithExpectedSize(targets.size());
        // seeded with the start so there is always something to reconstruct from, even if the budget
        // runs out before a single node is popped
        Map<Target, Node> bestEndpoints = Maps.newHashMapWithExpectedSize(targets.size());
        targets.forEach(target -> bestEndpoints.put(target, from));
        int maxVisited = (int) (this.maxVisitedNodes * searchDepthMultiplier);
        List<Node> closedNodes = BirdPathfindingConfig.collectDebugData ? Lists.newArrayList() : null;

        while (!this.openSet.isEmpty()) {
            if (++visited >= maxVisited) {
                break;
            }

            Node current = this.openSet.pop();
            current.closed = true;
            if (closedNodes != null) {
                closedNodes.add(current);
            }

            for (Target target : targets) {
                nominateEndpoint(bestEndpoints, target, current);
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
                .map(target -> this.reconstructPath(bestEndpoints.get(target), targetMap.get(target), true))
                .min(Comparator.comparingInt(Path::getNodeCount))
                : targets.stream()
                .map(target -> this.reconstructPath(bestEndpoints.get(target), targetMap.get(target), false))
                .min(Comparator.comparingDouble(Path::getDistToTarget).thenComparingInt(Path::getNodeCount));
        profiler.pop();
        Path path = best.orElse(null);
        if (path != null && closedNodes != null) {
            path.setDebug(this.openSet.getHeap(), closedNodes.toArray(new Node[0]), targets);
        }
        return path;
    }

    /**
     * Offers a node the search has just settled as the one a path to this target would be
     * reconstructed from, keeping whichever ends up closest and, among equally close ones, whichever
     * was cheapest to get to.
     * <p>
     * This replaces {@code Target.updateBest} / {@code getBestNode}, which is where the returned path
     * used to come from and which cannot be used here. It keeps the node with the lowest
     * <i>heuristic</i>, and vanilla calls it during relaxation, so the goal cell became the answer
     * the instant anything offered it as a neighbour, carrying whatever {@code cameFrom} it happened
     * to have at that moment. It never had to be popped, and the cost of the step into it was never
     * compared against anything. That is invisible in vanilla, which prices every edge the same, and
     * quietly fatal here: it made every {@code straightDownCost}, turn charge and clearance charge on
     * the final approach a no-op, so a bird would drop vertically into a pocket beside a wall no
     * matter what that move was priced at, and no amount of tuning could talk it out of it.
     * <p>
     * Only ever handed popped nodes, which is the fix: a node is nominated once the search is done
     * arguing about how to get to it, so its g and its {@code cameFrom} are the ones the path will
     * actually be flown along. A goal cell that is too expensive to be worth popping now simply loses
     * to the closest node that was, and the path ends a cell short, which is what pricing a move
     * out of reach is supposed to mean.
     */
    private static void nominateEndpoint(Map<Target, Node> bestEndpoints, Target target, Node settled) {
        Node incumbent = bestEndpoints.get(target);
        float settledH = settled.distanceTo(target);
        float incumbentH = incumbent.distanceTo(target);
        if (settledH != incumbentH ? settledH < incumbentH : settled.g < incumbent.g) {
            bestEndpoints.put(target, settled);
        }
    }

    /** Distance to the nearest target, and nothing else: nominating an endpoint is a separate job. */
    private float getBestH(Node node, Set<Target> targets) {
        float best = Float.MAX_VALUE;
        for (Target target : targets) {
            best = Math.min(node.distanceTo(target), best);
        }
        return best;
    }

    private Path reconstructPath(Node end, BlockPos targetPos, boolean reachesTarget) {
        List<Node> nodes = Lists.newArrayList();
        Node node = end;
        nodes.addFirst(end);
        while (node.cameFrom != null) {
            node = node.cameFrom;
            nodes.addFirst(node);
        }
        return new Path(nodes, targetPos, reachesTarget);
    }
}
