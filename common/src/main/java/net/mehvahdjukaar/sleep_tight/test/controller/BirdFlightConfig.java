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

    // how hard the move control chases the speed the throttle profile asks for, on top of the
    // feed-forward throttle that would settle there on its own. Units are throttle per block per
    // tick of error. Too low and the mob lags the profile through corners, too high and it hunts
    public static double speedGain = 4.0;

    // pitch is cosmetic (travel() ignores it for anything that is not elytra flying) but it is
    // what sells the dive/climb, so it tracks actual velocity rather than the waypoint
    public static float maxPitch = 60.0F;
    public static float maxPitchPerTick = 6.0F;

    // entities have no roll field, so bank is applied to the model only, worked out client side from
    // how fast the yaw is changing. Costs nothing and is the clearest read there is on what the
    // steering is actually doing
    public static float bankPerYawRate = 2.5F;
    public static float maxBankAngle = 55.0F;

    // how far along the path ahead of the mob the steering target sits. This is the corner rounding
    // dial: the flown arc cuts inside a corner by roughly a fifth to a third of this, so raising it
    // buys smoothness and spends clearance. Lattice nodes are one block apart, and anything below
    // that stops smoothing and just tracks the polyline
    public static double lookahead = 1.5;

    // how far ahead of the cursor to look when projecting the mob back onto the path. Only has to
    // cover a tick of travel; kept short because a long window can snap the cursor across a hairpin
    // and skip the leg in between
    public static double projectionWindow = 2.0;

    // ---- flight envelope ----
    // What the bird can physically do, as opposed to how the controller drives it. Snapshotted into
    // a FlightEnvelope once per path; see test/FLIGHT_ARCHITECTURE.md for who reads what.

    // from LivingEntity.travel: airborne horizontal drag is a flat 0.91, an 11 tick time constant
    public static double horizontalDrag = 0.91;

    // LivingEntity.getFlyingSpeed, the velocity added per tick at full input while airborne. It is
    // a flat constant in vanilla and the FLYING_SPEED attribute never reaches it, but it is
    // protected, so a mob that wants real control authority can override it and raise this to match
    public static double airAcceleration = 0.02;

    // the drag the mob manages while deliberately slowing. Same as cruising drag until the
    // controller grows a flare (spread wings, pitch up), which is the only way to brake harder than
    // coasting. Lower means shorter stopping distances and a throttle profile that can be later
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

    // above this the bird is climbing steeply enough to be hovering rather than flying, which is
    // slow and effortful. Legs steeper than this get scaled down towards hoverSpeedFactor, reaching
    // it on a purely vertical move. Stops the lattice's vertical hops being flown like an elevator
    public static double maxClimbAngle = 30.0;
    public static double hoverSpeedFactor = 0.25;
}