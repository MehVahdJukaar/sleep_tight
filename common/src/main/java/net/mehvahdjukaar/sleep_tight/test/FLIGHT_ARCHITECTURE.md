# Believable flight: the three layers

Map of the whole thing, and the one document to read first. The per-layer detail lives next to the
code it describes:

| | package | its notes |
|---|---|---|
| what the world allows | `pathfinding/` | `pathfinding/PATHFINDING_NOTES.md` |
| how fast that can be flown | `throttle/` | `throttle/THROTTLE_NOTES.md` |
| making the mob do it | `navigator/`, `controller/` | `believable_bird_flight.md` (design), `MOB_AI_NOTES.md` (vanilla reference) |

## The split

```
   pathfinding/          knows the WORLD
   BirdNodeEvaluator     block types, clearance, where a bird can physically fit
   BirdPathFinder        --> a Path: a polyline of cells, plus per-node heading and enclosure
        |
        v
   throttle/             knows PHYSICS
   ThrottlePlanner       drag, thrust, turn radius. No Level, no Mob, no tick
        |                --> a ThrottleProfile: a speed limit for every point along that path
        v
   navigator/            knows the MOB'S LIVE STATE
   BirdPathNavigation    where along the path it actually is, whether it is falling behind
   controller/           --> yaw, pitch and thrust, tick by tick
   BirdMoveControl
```

Each layer reads only the one above it. The arrows are one-way on purpose: the search does not know
how fast the bird flies, and the follower does not re-derive geometry.

## Why it is split here and not somewhere else

The dividing question is **turn radius**, and it is the thing that was missing before this split
existed.

A body turning at rate `w` while moving at speed `v` traces a circle of radius `v/w`. Fit that
circle into a corner of angle `t` and it passes `r * (1/cos(t/2) - 1)` blocks inside the corner
point. So:

- the **search** cannot decide whether a corner is flyable, because that depends on speed
- the **follower** cannot decide either, because by the time it sees the corner it is already
  carrying the speed it should not have had

Only something that sees the whole route at once can answer it, and the answer is not "forbid the
corner" but "arrive slower". That is the throttle layer, and it is the reason this is three layers
rather than two.

The contract, stated once:

> The planner may draw any corner it likes, as long as it also states the speed that corner
> requires. The follower guarantees it will be at or below that speed on arrival.

## FlightEnvelope: the shared vocabulary

`throttle/FlightEnvelope` is the one description of what the bird can physically do: yaw rate, top
speed, drag, acceleration, how far off the drawn line it may drift, maximum sustained climb angle.
All three layers read it.

Before it existed, `maxYawPerTick` lived in `BirdFlightConfig` and `turnCost90` in
`BirdPathfindingConfig`. Those two numbers describe the same physical fact from opposite sides with
nothing linking them, which is exactly how the planner ended up free to draw corners the follower
could not fly and nobody noticed. It is immutable and snapshotted from the live config once per
path, so a path is always flown against the numbers it was planned with.

## What each layer must never do

- **`pathfinding/`** must not reason about speed, time or momentum. It prices turns (`turnCost45/90/135`)
  because a sharp turn costs *time*, but the actual number should be derived from the envelope
  rather than hand-tuned. It must not know what a tick is.
- **`throttle/`** must not touch `Level` or `Mob`. It takes points and numbers and returns numbers.
  This is what makes it testable without a game running, which matters because the follower has
  never had a single test.
- **`navigator/` + `controller/`** must not re-derive anything the two layers above already computed.
  Any loop over path nodes down here is a smell; it means something that should have been planned
  once is being guessed at twenty times a second.

## Current state

Done:

- the search, including the state lattice, turn costs and the clearance field
- the throttle layer (`ThrottlePlanner`, `ThrottleProfile`, `FlightEnvelope`)
- the debug overlay for both, including per-node speed arrows and body-versus-velocity arrows

Not done, in the order they should happen:

1. **Follower rewrite.** `BirdMoveControl` still does its own braking from a per-tick walk over the
   remaining nodes (`remainingAlongPath`, `brakeFactor`). All of that is now the throttle layer's
   job and should collapse into one `speedLimitOver` lookup. Along with it: velocity steering,
   arc-length pure pursuit and an absorbing arrival state. See `believable_bird_flight.md` sections
   1, 2 and 6.
2. **Turn costs derived from the envelope** rather than hand-set. The existing values happen to land
   within about a factor of two of the physically correct ones, but that is luck, and it stops being
   true the moment the bird's agility changes.
3. **Longer move primitives** in `BirdNodeEvaluator`. One-block steps with a 90 degree cap pin the
   minimum turn radius at 0.64 blocks and make shallow climbs inexpressible (the only climb angles
   available are 0, 35, 45 and 90 degrees, so a gentle climb comes out as a vertical zigzag). Both
   fix themselves with 2-3 block steps.
4. **Flier-appropriate watchdogs.** Vanilla's per-node timeout budgets from cruise speed and so
   fires spuriously once the bird starts slowing for corners. `ThrottleProfile.expectedFlightTicks()`
   is the number it should be using.

Decided against, with the reasoning in `throttle/THROTTLE_NOTES.md`: putting speed into the search
state. It multiplies the state space by the number of speed bins, which costs search range rather
than memory, and it buys a refinement of numbers the turn costs already approximate.
