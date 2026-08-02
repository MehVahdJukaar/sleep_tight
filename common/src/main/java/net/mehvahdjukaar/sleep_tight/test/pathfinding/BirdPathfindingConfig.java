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

    // the same ladder in the vertical plane, charged on the change in BirdNode#climb between two
    // steps. Only two rungs exist because the lattice only has three climb states: one bin is
    // levelling off or tipping into a slope (35 to 45 degrees, depending on the step's horizontal
    // length), two is a climb reversed straight into a dive.
    //
    // Without these a pitch reversal was free, and two things followed from that. Porpoising: a
    // shallow climb has to be a sawtooth of 45 degree steps and level ones, and that cost the same
    // as the smooth ramp it should lose to. And, worse, hopping over ground: a cell with a block
    // under it pays vanilla's WALKABLE +1 plus its wallHugCost share, and stepping over one cell
    // diagonally instead of through it only costs 2*sqrt(2) - 2 = 0.83 of extra distance, so the
    // +1 alone was already enough to make the detour cheaper. Every approach to a perch ended in a
    // pop-up and a 90 degree dive. At 5, that detour now costs 0.83 + 0.5 + 5 against the 1.3 to
    // 2.1 of flying straight through, and where a climb genuinely is needed the search spreads it
    // over two 1-bin changes (1.0) rather than one 2-bin snap, which is the same ordering that
    // makes four 45s beat a 180 horizontally.
    //
    // Deliberately separate knobs from the yaw ladder even though they start at the same values: a
    // pull-up is not a bank and there is no reason the two have to stay in step.
    public static float pitchCost45 = 0.5F;
    public static float pitchCost90 = 5.0F;

    // purely vertical moves only; climbs and dives with horizontal motion are ordinary flight.
    // priced rather than forbidden so vertical shafts stay reachable as a last resort.
    // A vertical hop is really +-90 degrees of pitch but only reads as one climb bin, so entering
    // one from level flight is charged pitchCost45 rather than pitchCost90. That understatement is
    // deliberate: these two costs already dominate anything the pitch ladder would add
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
    //   statesPerCell          BirdNodeEvaluator.HEADING_BINS * CLIMB_STATES. Vanilla has one node
    //                          per cell; the lattice has one per heading per climb state, so a
    //                          budget sized vanilla's way reaches a twenty-fourth as far and
    //                          returns a partial path from a search that had plenty of room left
    //
    // At FOLLOW_RANGE 64 that is 64 * 16 * 8 * 3 = 24576. It is synchronous server thread work, so
    // this is the first knob to look at if pathing shows up in a profile
    public static int nodesPerBlockOfRange = 16;

    // A* heuristic weight. Vanilla uses 1.5. 2.0 halves search cost but is greedy enough that it
    // ignores wallHugCost (it commits to the first line that reaches the goal), so this sits at
    // 1.6: still cheaper than vanilla, still lets clearance steer the route
    public static float heuristicWeight = 1.6F;

}
