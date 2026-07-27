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

    // slow down through hard turns, so the turn happens over less ground and the mob does not
    // drift wide. Below turnSlowdownStart nothing happens, at turnSlowdownFull thrust is scaled
    // by minTurnSpeedFactor. Never goes to zero: a bird that stops mid turn looks broken.
    // Mostly redundant once velocitySteerFraction is up: the drift it compensates for is gone
    public static float turnSlowdownStart = 10.0F;
    public static float turnSlowdownFull = 60.0F;
    public static float minTurnSpeedFactor = 0.35F;

    // scales the "how fast can I still be going and make the turn/stop" limit. Above 1 the mob
    // brakes later and overshoots a little, below 1 it creeps in
    public static float brakeAggressiveness = 1.0F;

    // how far ahead along the path the braking logic looks. Cheap linear walk over nodes, so
    // keep it modest; anything past this counts as "still far away, keep cruising"
    public static double brakeLookahead = 16.0;

    // vertical thrust ramps linearly over this many blocks of altitude error instead of vanilla's
    // full-throttle-either-way. Directly fixes the vertical hunting
    public static float verticalApproachBand = 2.0F;

    // pitch is cosmetic (travel() ignores it for anything that is not elytra flying) but it is
    // what sells the dive/climb, so it tracks actual velocity rather than the waypoint
    public static float maxPitch = 60.0F;
    public static float maxPitchPerTick = 6.0F;

    // how far along the path ahead of the mob the steering target sits. This is the corner rounding
    // dial: the flown arc cuts inside a corner by roughly a fifth to a third of this, so raising it
    // buys smoothness and spends clearance. Lattice nodes are one block apart, and anything below
    // that stops smoothing and just tracks the polyline
    public static double lookahead = 1.5;

    // how far ahead of the cursor to look when projecting the mob back onto the path. Only has to
    // cover a tick of travel; kept short because a long window can snap the cursor across a hairpin
    // and skip the leg in between
    public static double projectionWindow = 2.0;
}