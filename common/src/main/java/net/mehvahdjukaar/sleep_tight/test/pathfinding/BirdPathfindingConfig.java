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

    // A* heuristic weight. Vanilla uses 1.5; 2.0 halves search cost with no measurable
    // smoothness loss on the lattice
    public static float heuristicWeight = 2.0F;
}
