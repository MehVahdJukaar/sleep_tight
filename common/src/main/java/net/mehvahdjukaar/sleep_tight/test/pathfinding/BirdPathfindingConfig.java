package net.mehvahdjukaar.sleep_tight.test.pathfinding;

/**
 * Tuning knobs for the bird state-lattice pathfinder. Values were tuned in the standalone
 * PathfindingTest prototype (2026-07). All costs are in "block lengths" (a straight step costs 1).
 */
public class BirdPathfindingConfig {

    // turns sharper than this many 45 degree bins per step are not expanded at all.
    // 2 (= 90 degrees) is the sweet spot: a 45 cap kills reachability in cluttered terrain
    public static int maxTurnBins = 2;

    // heading changes between consecutive steps. Small corrections are near free so arcs
    // stay smooth without zigzag wobble; sharp snaps are possible but discouraged
    public static float turnCost45 = 0.5F;
    public static float turnCost90 = 5.0F;
    public static float turnCost135 = 12.0F; // only reachable if maxTurnBins is raised to 3

    // purely vertical moves only; climbs and dives with horizontal motion are ordinary flight.
    // priced rather than forbidden so vertical shafts stay reachable as a last resort
    public static float straightUpCost = 3.0F;
    public static float straightDownCost = 2.0F;

    // how much a cell is charged for having blocks touching it, so the bird keeps some air around
    // itself instead of scraping along surfaces and clipping corners. Counts the 26 touching cells,
    // each weighted by how far it really is (face 1.0, edge 0.71, corner 0.58), normalized so this
    // value is what a fully boxed-in cell would pay. Real cells pay a fraction of it: a flat
    // surface on one side lands near 0.3x, an inside corner roughly double that.
    public static float wallHugCost = 3.5F;

    // A* heuristic weight. Vanilla uses 1.5. 2.0 halves search cost but is greedy enough that it
    // ignores wallHugCost (it commits to the first line that reaches the goal), so this sits at
    // 1.6: still cheaper than vanilla, still lets clearance steer the route
    public static float heuristicWeight = 1.6F;

    // record the open and closed sets on the finished path. Off by default: it is only used to
    // shade every node the search touched, which buries the path itself and is a lot of nodes to
    // ship to the client. The path draws fine without it
    public static boolean collectDebugData = false;
}
