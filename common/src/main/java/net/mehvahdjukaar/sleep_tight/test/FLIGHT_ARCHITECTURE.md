# Believable flight: the three layers

Map of the whole thing, and the one document to read first. The per-layer detail lives next to the
code it describes:

| | package | its notes |
|---|---|---|
| what the world allows | `pathfinding/` | `pathfinding/PATHFINDING_NOTES.md` |
| how fast that can be flown | `throttle/` | `throttle/HOW_IT_WORKS.md` (what it does), `throttle/THROTTLE_NOTES.md` (why) |
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

## The one thing that is not in that stack

`controller/BirdGroundControl` is not a fourth layer, it is the **other half** of the bottom one: the
locomotion the bird does with its feet. It owns whether the bird is perched, gravity, and the turn on
the spot that lines the mob up with a path before it takes off. The flying stack talks to it through
`controller/PerchingFlier`, implemented by the mob, and through nothing else.

It is worth its own box for two reasons. It is the only thing that reaches **up past** the follower
into the search: a perched start plans its departure in any direction only because this is what makes
that true on the mob (`PATHFINDING_NOTES.md` invariant 5). And it is where a second, walking
navigation goes when short distances stop being worth flying, at which point the flying stack above
is unaffected because `PerchingFlier` is the whole contract.

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

  The one loop that is allowed is in `BirdPathNavigation.speedLimitFor`, and it is worth stating why.
  It does not recompute the planner's answer; it re-applies the planner's own braking inequality with
  a number only this layer can know, how much ground the mob is actually covering per block of route.
  A mob rounding a corner off covers arc faster than ground and so sheds less speed than the profile
  assumed. On a mob tracing the line the loop provably finds nothing and costs nothing.

## Current state

Done:

- the search, including the state lattice, turn costs and the clearance field
- the throttle layer (`ThrottlePlanner`, `ThrottleProfile`, `FlightEnvelope`)
- the debug overlay for both, including per-node speed arrows and body-versus-velocity arrows
- the follower: velocity steering (`turnVelocityWithBody`), arc-length pure pursuit (`PathRuler`),
  speed straight off the profile, and an absorbing arrival. `believable_bird_flight.md` sections 1,
  2 and 6 are done apart from the gait machine
- the follower's second pass, brought over from the `PathfindingTest` lab where three controls were
  flown over the same routes and measured. Four things came back: the carrot is sized by the node's
  enclosure and pulled in when the mob is off the line; the cursor may lose ground, capped against
  distance actually flown, so a shoved mob gives its progress back instead of reading "almost
  arrived" from a dozen blocks out; the commanded speed is corrected by real braking authority when
  a corner is being cut, and floored while the mob is far off the line; and the throttle is a
  deadbeat servo instead of a tuned gain. Measured on the lab's cluttered routes, that is a worst
  corridor breach of 0.05 blocks against 0.27, and roughly half the excursion above the profile

Not done, in the order they should happen:

1. **The flare.** `believable_bird_flight.md` section 6 wanted four gaits: takeoff, cruise, flare,
   perch. Takeoff and perch landed 2026-08-01 in `controller/BirdGroundControl`, which also took
   ownership of `setNoGravity` from the mob constructor and the move control, so arrival now descends
   and lands instead of leaving the bird hovering. What is still missing is the approach: nothing
   levels out, bleeds horizontal speed and pitches up before the perch, so the descent is the profile
   running out rather than a landing.
2. **Turn costs derived from the envelope** rather than hand-set. The existing values happen to land
   within about a factor of two of the physically correct ones, but that is luck, and it stops being
   true the moment the bird's agility changes.
3. **Longer move primitives** in `BirdNodeEvaluator`. One-block steps with a 90 degree cap pin the
   minimum turn radius at 0.64 blocks and make shallow climbs inexpressible (the only climb angles
   available are 0, 35, 45 and 90 degrees, so a gentle climb comes out as a vertical zigzag). Both
   fix themselves with 2-3 block steps.
4. ~~**Flier-appropriate watchdogs.**~~ Done, in one line. Vanilla's node timeout recomputes
   `timeoutLimit` on a node change but never zeroes `timeoutTimer`, so the clock runs from the start
   of the path against a one-node budget and every mob is on a ~200 tick fuse. Vanilla hides it by
   finishing short paths first and by repathing instantly when it does fire.
   `BirdPathNavigation.restartNodeTimeout` supplies the missing reset.

Decided against, with the reasoning in `throttle/THROTTLE_NOTES.md`: putting speed into the search
state. It multiplies the state space by the number of speed bins, which costs search range rather
than memory, and it buys a refinement of numbers the turn costs already approximate.
