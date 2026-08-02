package net.mehvahdjukaar.sleep_tight.test.controller;

import net.mehvahdjukaar.sleep_tight.test.pathfinding.BirdPathfindingConfig;

/**
 * Tuning knobs for flying the lattice paths, as opposed to {@link BirdPathfindingConfig} which
 * tunes the search that produces them. Same deal: public static and mutable so they can be poked
 * at runtime.
 */
public class BirdFlightConfig {

    // yaw is the whole ballgame. Vanilla move controls snap up to 90 degrees in a single tick, which
    // throws away the turn budget the lattice paid for. What is configured is the radius of the arc
    // rather than the rate, because the radius is the part that has to stay put when speed changes:
    // radius is speed over rate, so a fixed deg/tick silently widens every corner the moment the
    // bird flies faster, and the arcs stop fitting the lattice that drew them. Half a block is the
    // tie to that lattice - nodes sit a block apart and a step turns up to 90 degrees, so an arc
    // this size rounds a corner without leaving the cells the search certified. FlightEnvelope turns
    // it into a yaw rate; nothing else may hold an opinion about how fast the bird turns
    public static double turnRadius = 0.5;

    // no arc tighter than this, whatever the numbers say, so a degenerate radius cannot ask for a
    // pivot on the spot. Vanilla's FlyingMoveControl snaps at exactly this rate and looks like it
    public static double maxYawPerTickCeiling = 90.0;

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
    // steering is actually doing. Expressed as the angle at a full-rate turn and scaled by how much
    // of that rate is actually being used, rather than as degrees of bank per degree of yaw: the
    // yaw rate now moves with speed, and a fixed ratio would peg a fast bird at full bank forever
    public static float maxBankAngle = 55.0F;


    // how far along the path ahead of the mob the steering target sits when there is nothing anywhere
    // near the line. Steering only: it says nothing about speed, which comes from the throttle
    // profile. This is the corner rounding dial: the flown arc cuts inside a corner by roughly a
    // fifth to a third of this, so 1.8 puts the cut at the planner's corridorMargin. Lattice nodes
    // are one block apart, and anything below that stops smoothing and just tracks the polyline.
    // Stays a distance rather than becoming a number of ticks because what pure pursuit actually
    // needs is room to turn in, about three times turnRadius, and that ratio no longer moves with
    // speed now the radius is the thing being held fixed
    public static double openAirLookahead = 1.8;

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

    // how hard the lookahead is pulled in each time the cut it implies still does not fit the
    // corridor, and how many times that is tried. Three trims take the open air 1.5 to 0.51, which is
    // already under the enclosed floor, so this always ends on the floor rather than on the counter
    public static double lookaheadTrim = 0.7;
    public static int maxLookaheadTrims = 3;

    // how far off the line the mob has to be before the profile stops being applied at face value,
    // and the distance by which the speed floor has fully ramped in. Comfortably outside anything
    // normal flying reaches, rounding corners off peaks near a block, so this only fires on a real
    // shove
    public static double rejoinFrom = 2.0;
    public static double rejoinBy = 4.0;

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

    // SUSTAINED velocity added per tick at full effort while airborne, averaged over a wingbeat.
    // Everything downstream that has an opinion about how fast the bird can go reads this one:
    // terminal speed, and through it the yaw rate, the turn radius and every corner the planner
    // prices. A single beat delivers far more than this for one tick (see maxBeatRate) but the
    // average is held here, so making the flight bursty moved no planner number at all.
    //
    // Vanilla hardcodes LivingEntity.getFlyingSpeed to 0.02, which is why this used to be 0.02 and
    // was documented as untouchable. BirdTestMob now overrides it with FlightEnvelope.peakThrust,
    // so this is a real knob again
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

    // ---- burst thrust ----
    // How much harder the wings can push from a standstill than they can sustain, as a multiple of
    // maxThrustAccel. The one knob behind both a bird leaving a perch and a bird holding itself up
    // after a jump, because those are the same thing: wings flat out with no airspeed to help.
    //
    // What it replaced was a first-order lag on the thrust (thrustSpoolTicks, 8) whose job was to
    // stop the bird leaping to cruise. It did that far too well: two exponentials in series, the
    // lag's 8 ticks and drag's own 11, took about 57 ticks to settle, so every takeoff was a slow
    // drift out of the perch. Allowing a burst instead is the honest version of the same idea, and
    // it does not remove the ramp - thrust is still bounded, so top speed still takes a second or so
    // to reach, it just starts with a shove rather than a nudge.
    //
    // FlightEnvelope.thrustCapAt fades it out linearly with speed, ending exactly on the sustained
    // figure at top speed, so no number the planner derives from top speed moves at all. Needs
    // BirdTestMob to override getFlyingSpeed, since vanilla pins that to the sustained figure
    public static double peakThrustFactor = 4.0;

    // ---- wing animation ----
    // Wingbeats per tick at no effort and at full effort. Purely how the model reads: the flap rate
    // is interpolated between them on FlightEnvelope.effortFor(thrust), so the wings beat fast on a
    // burst off a perch and slowly on a glide into one. Nothing flows back the other way - thrust is
    // what the bird emits and the wings are a readout of it, not a driver
    public static double minFlapRate = 0.06;
    public static double maxFlapRate = 0.25;

    // how far the flown arc is allowed to bulge off the drawn line when rounding a corner. This is
    // the dial that turns a turn angle into a speed limit: radius is speed/yawRate, and a corner of
    // angle t passes radius * (1/cos(t/2) - 1) blocks inside the corner point. That use of it shrinks
    // with how walled in the cell is, since only the cells on the line were certified clear.
    //
    // It is also the ceiling on how far BirdFlightNavigation.carrotFor may bias the aim point, and
    // there it is flat rather than scaled by enclosure, which is what makes it the open air rounding
    // dial: a walled in cell is already down to enclosedLookahead and never asks for a cut this big,
    // so raising it widens the curves that have room and leaves the tight ones alone. Pair it with
    // openAirLookahead, since a cut that does not fit here just gets the lookahead trimmed back
    public static double corridorMargin = 0.45;

    // there is no knob for the vertical equivalent on purpose: FlightEnvelope measures it off the mob
    // as FlightLine.verticalNodeOffset, half a cell minus half the mob's height, 0.125 for the 0.75
    // tall test bird. Unlike the horizontal allowance above it is not a preference to be tuned, it is
    // exactly the room left over once the mob sits centred in the cells the search certified, and
    // padding it would be claiming space nothing checked. Vertical neighbours are far more often
    // solid than sideways ones, so that restraint is worth more here than it would be horizontally

    // above this the bird is climbing steeply enough to be hovering rather than flying, which is
    // slow and effortful. Legs steeper than this get scaled down towards hoverSpeedFraction, reaching
    // it on a purely vertical move. Stops the lattice's vertical hops being flown like an elevator
    public static double maxClimbAngle = 30.0;
    public static double hoverSpeedFraction = 0.25;
}