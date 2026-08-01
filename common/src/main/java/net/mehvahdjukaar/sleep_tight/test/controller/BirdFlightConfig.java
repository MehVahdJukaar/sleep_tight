package net.mehvahdjukaar.sleep_tight.test.controller;

import net.mehvahdjukaar.sleep_tight.test.pathfinding.BirdPathfindingConfig;

/**
 * Tuning knobs for flying the lattice paths, as opposed to {@link BirdPathfindingConfig} which
 * tunes the search that produces them. Same deal: public static and mutable so they can be poked
 * at runtime.
 */
public class BirdFlightConfig {

    // yaw is the whole ballgame. Vanilla move controls snap up to 90 degrees in a single tick,
    // which throws away the turn budget the lattice paid for. At cruise the mob covers a block in
    // roughly 5 ticks, and a lattice step turns at most 90 degrees, so ~15 deg/tick keeps the
    // flown arc close to the planned one
    public static float maxYawPerTick = 15.0F;

    // how much of the body's turn the momentum comes round with. travel() only ever pushes along
    // yaw and leaves the old velocity to decay at 0.91 a tick, so at 0 the velocity settles tens of
    // degrees behind the body and the bird crabs sideways through every turn. At 1 velocity tracks
    // the body exactly and the turn radius is just speed / yaw rate; lower leaves some slip, which
    // reads as weight rather than rails
    public static float velocitySteerFraction = 1.0F;

    // pitch is cosmetic (travel() ignores it for anything that is not elytra flying) but it is
    // what sells the dive/climb, so it tracks actual velocity rather than the waypoint
    public static float maxPitch = 60.0F;
    public static float maxPitchPerTick = 6.0F;

    // entities have no roll field, so bank is applied to the model only, worked out client side from
    // how fast the yaw is changing. Costs nothing and is the clearest read there is on what the
    // steering is actually doing
    public static float bankPerYawRate = 2.5F;
    public static float maxBankAngle = 55.0F;

    // how far along the path ahead of the mob the steering target sits when there is nothing anywhere
    // near the line. Steering only: it says nothing about speed, which comes from the throttle
    // profile. This is the corner rounding dial: the flown arc cuts inside a corner by roughly a
    // fifth to a third of this, so 1.5 puts the cut near the planner's corridorMargin. Lattice nodes
    // are one block apart, and anything below that stops smoothing and just tracks the polyline
    public static double openAirLookahead = 1.5;

    // the same, in a cell walled in on every side. The carrot is interpolated between the two on the
    // node's measured enclosure, which is the whole point: the search only certified the cells on
    // the line as clear, so rounding a corner off is only safe to the extent there is measured room
    // to round it into. Floored well above zero because aiming at your own feet is not steering, it
    // is a bird spinning on the spot
    public static double enclosedLookahead = 0.6;

    // how much the carrot is pulled in, in blocks, per block the mob is off the line. A shove leaves
    // the bird flying parallel to the path rather than along it, and a carrot far ahead rejoins so
    // gently it can take longer than the path itself. Pulling it in makes the rejoin an actual
    // correction, at the cost of a sharper turn while it happens
    public static double offRouteRecoveryGain = 1.0;

    // how far either side of the cursor to look when projecting the mob back onto the path. Only has
    // to cover a tick of travel; kept short because a long window can snap the cursor across a
    // hairpin and skip the leg in between. Symmetric because the cursor is allowed to lose ground,
    // see PathRuler.advanceCursorTo
    public static double projectionWindow = 2.0;

    // how close to the end of the path counts as arrived. Needed because braking is geometric: the
    // profile's arrival ramp closes the last of the distance asymptotically, so waiting for the
    // cursor to actually reach the end means waiting forever. Small enough to look like arrival,
    // large enough that the ramp gets there in a second or so rather than a minute
    public static double arrivalRadius = 0.25;

    // ---- flight envelope ----
    // What the bird can physically do, as opposed to how the controller drives it. Snapshotted into
    // a FlightEnvelope once per path; see test/FLIGHT_ARCHITECTURE.md for who reads what.

    // from LivingEntity.travel: a flat 0.91 while airborne, an 11 tick time constant. It applies to
    // all three axes only because the mob is a FlyingAnimal; anything else gets 0.98 vertically,
    // which is a 50 tick constant and would make every number derived from this one wrong on a climb
    public static double airDrag = 0.91;

    // LivingEntity.getFlyingSpeed: velocity added per tick at FULL throttle while airborne. Real
    // acceleration is this times the throttle, which the FLYING_SPEED attribute caps. Vanilla
    // hardcodes 0.02 and getFlyingSpeed is protected, so raising this only describes reality for a
    // mob that overrides it. BirdTestMob does not, so leave it alone
    public static double maxThrustAccel = 0.02;

    // NOT a brake pedal: a multiplier, so deceleration is proportional to current speed rather than
    // constant. There is no brake at all, the only way to slow down is to cut thrust and coast,
    // which is why this equals cruising drag. It is a separate knob so a future flare (spread wings,
    // pitch up) has somewhere to live; lower means shorter stopping distances and a later profile
    public static double brakingDrag = 0.91;

    // normal flight as a share of top speed, and the floor the throttle planner will not take a
    // corner below. A bird that decelerates to zero mid air looks broken, so corners cost speed but
    // never all of it. Arrival is the one place allowed under the floor
    public static double cruiseFraction = 0.8;
    public static double minSpeedFraction = 0.15;
    public static double arrivalSpeed = 0.0;

    // how far the flown arc is allowed to bulge off the drawn line when rounding a corner. This is
    // the dial that turns a turn angle into a speed limit: radius is speed/yawRate, and a corner of
    // angle t passes radius * (1/cos(t/2) - 1) blocks inside the corner point. Shrinks with how
    // walled in the cell is, since only the cells on the line were certified clear
    public static double corridorMargin = 0.35;

    // the same, vertically, and split in two because the vertical corridor is not centred on the
    // line the way the horizontal one is. Path.getEntityPosAtNode puts the mob at the BOTTOM of its
    // node cell, so the drawn line runs along the bird's feet: the certified room is all above it
    // (one cell minus the mob's height, 0.25 for the 0.75 tall test bird) and there is none at all
    // below. A bird smoothing over a staircase cuts the peaks downward, straight into the steps,
    // which is exactly the direction with no budget.
    // The floor value is a small fiction rather than the true zero, which would have the bird crawl
    // over every step: the cell under the line is only really solid where there is a block in it,
    // and a scalar enclosure cannot say which side of a cell is blocked. Directional clearance is
    // what would let this be measured instead of guessed
    public static double corridorMarginAbove = 0.25;
    public static double corridorMarginBelow = 0.08;

    // above this the bird is climbing steeply enough to be hovering rather than flying, which is
    // slow and effortful. Legs steeper than this get scaled down towards hoverSpeedFraction, reaching
    // it on a purely vertical move. Stops the lattice's vertical hops being flown like an elevator
    public static double maxClimbAngle = 30.0;
    public static double hoverSpeedFraction = 0.25;
}