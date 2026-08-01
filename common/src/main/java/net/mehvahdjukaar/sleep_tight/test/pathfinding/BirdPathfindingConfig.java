package net.mehvahdjukaar.sleep_tight.test.pathfinding;

/**
 * Tuning knobs for the bird state-lattice pathfinder. Values were tuned in the standalone
 * PathfindingTest prototype (2026-07). All costs are in "block lengths" (a straight step costs 1).
 */
public class BirdPathfindingConfig {

    // turns sharper than this many 45 degree bins per step are not expanded at all. 4 (= 180) means
    // nothing is, which is the point: in flight the way ahead is a strong preference, not a rule.
    // A hard cap made whole regions unreachable rather than expensive - a bird in a corridor
    // narrower than its turn radius had no legal horizontal move at all, since the two purely
    // vertical moves are the only ones exempt, so it climbed out of dead ends it could have flown
    // back down. Pricing reversals instead lets the throttle layer answer them the way it answers
    // every other corner: arrive slower. Lower this only to forbid a manoeuvre outright.
    // Does not apply to a state with no heading to conserve, which is the start of a search from a
    // perched bird and the vertical climb straight off it, see BirdNode#freeHeading
    public static int maxTurnBins = 4;

    // heading changes between consecutive steps. Small corrections are near free so arcs stay smooth
    // without zigzag wobble; sharp snaps are possible but discouraged. Hand-tuned rather than derived
    // from the flight envelope (FLIGHT_ARCHITECTURE.md still lists that as owed), so read the ladder
    // as an aesthetic ordering, not as seconds: a 90 barely troubles the throttle in open air yet is
    // priced at 5, because a bird that snaps 90 degrees looks wrong whether or not it is expensive
    public static float turnCost45 = 0.5F;
    public static float turnCost90 = 5.0F;
    public static float turnCost135 = 12.0F;
    // continues the same ladder. Worth about 3 blocks of straight flight in real time (decelerate to
    // minSpeed, ~12 ticks of turning, accelerate back), so this is deliberately ~8x the physical
    // cost: in open air the search should always prefer a wide banked reversal, which comes out as
    // four 45s for 2.0, and only pay this where the geometry leaves nothing else
    public static float turnCost180 = 25.0F;

    // purely vertical moves only; climbs and dives with horizontal motion are ordinary flight.
    // priced rather than forbidden so vertical shafts stay reachable as a last resort
    public static float straightUpCost = 7.0F;
    public static float straightDownCost = 7.0F;

    // how much a cell is charged for having blocks touching it, so the bird keeps some air around
    // itself instead of scraping along surfaces and clipping corners. Counts the 26 touching cells,
    // each weighted by how far it really is (face 1.0, edge 0.71, corner 0.58), normalized so this
    // value is what a fully boxed-in cell would pay. Real cells pay a fraction of it: a flat
    // surface on one side lands near 0.3x, an inside corner roughly double that.
    // Only the charge: setting this to 0 removes the search bias but keeps the measurement, which
    // the throttle planner needs to know how much room a corner has.
    //
    // This deliberately stacks with vanilla's WALKABLE +1 air preference in BirdNodeEvaluator's
    // findAcceptedLatticeNode, and the pair is what keeps birds off the deck. Flat ground under a
    // cell blocks its whole bottom 3x3 shell, which is 1 face + 4 edges + 4 corners = 6.14 of the
    // 19.10 a fully boxed cell would score, so 0.32 measured and 1.12 charged at the default. With
    // the +1 on top that is 2.12 on a step whose length is 1.0: skimming the ground costs three
    // times what cruising one cell higher does, and straightUpCost has paid for itself after two
    // blocks of it. So a bird climbs off the floor at the first opportunity and stays up, and near
    // a wall it will happily detour rather than run along the surface.
    //
    // That is the intended shape and not an accident of two knobs colliding: birds fly, they do not
    // hover along the floor. Treat the two as one dial. Unstacking them (dropping the +1, or
    // excluding the down-facing shell when the only blocked thing is the ground) is what to do if
    // you ever want a mob that does hug terrain, and it will need retuning against straightUpCost
    // and the turn ladder, since those were picked against this total
    public static float wallHugCost = 3.5F;

    // the measurement itself, 26 path type lookups per cell touched by the search. Turning it off
    // is the way to make clearance genuinely free, at the cost of the throttle planner having to
    // assume every corner has full room to swing wide into
    public static boolean measureClearance = true;

    // How many states the A* may expand before it gives up and returns the best partial path.
    // Sized as followRange * nodesPerBlockOfRange * statesPerCell, so both factors are visible:
    //
    //   nodesPerBlockOfRange   how much slack per block of range the search gets to route around
    //                          obstacles. 16 is vanilla's number and the only reason to change it
    //                          is if paths keep coming back partial in cluttered terrain
    //   statesPerCell          BirdNodeEvaluator.HEADING_BINS. Vanilla has one node per cell; the
    //                          lattice has one per heading, so a budget sized vanilla's way reaches
    //                          an eighth as far and returns a partial path from a search that had
    //                          plenty of room left
    //
    // At FOLLOW_RANGE 64 that is 64 * 16 * 8 = 8192. It is synchronous server thread work, so this
    // is the first knob to look at if pathing shows up in a profile
    public static int nodesPerBlockOfRange = 16;

    // A* heuristic weight. Vanilla uses 1.5. 2.0 halves search cost but is greedy enough that it
    // ignores wallHugCost (it commits to the first line that reaches the goal), so this sits at
    // 1.6: still cheaper than vanilla, still lets clearance steer the route
    public static float heuristicWeight = 1.6F;

    // record the open and closed sets on the finished path. Off by default: it is only used to
    // shade every node the search touched, which buries the path itself and is a lot of nodes to
    // ship to the client. The path draws fine without it
    public static boolean collectDebugData = false;

    // record the moves offered at each node of the finished path, so the renderer can show what the
    // search turned down. Cheap next to the search itself (26 checks per path node, against
    // thousands of expansions) but it does add a few hundred cells to every debug packet, so this
    // is the knob to pull if the debug channel starts costing anything
    public static boolean collectConsideredMoves = true;
}
