# The throttle layer

How fast the bird is allowed to be at every point of a path it has already been given. Written
2026-07-28, when this package was added.

This file is the **reasoning**: why the layer exists, why the rules are shaped the way they are, and
what was considered and rejected. If you just want to know what the code does, read
**`HOW_IT_WORKS.md`** instead, which walks the four steps in order with a worked example.
`../FLIGHT_ARCHITECTURE.md` is the map of all three layers.

## Why this exists at all

Turn radius is `speed / yawRate`. That one identity is the whole reason for this layer.

A path drawn by the lattice contains corners. Whether a corner is flyable is not a property of the
corner, it is a property of the corner **and the speed at which it is entered**. So:

- the search cannot decide it (it does not know about speed, and putting speed in the state costs
  more than it is worth, see below)
- the follower cannot decide it either, because by the time it can see the corner it is already
  carrying the speed it should not have had

The fix for a corner too tight to fly is not to forbid it, it is to arrive slower. Working that out
once, over the whole route, is this package.

## The corner rule

Two legs meeting at a turn of angle `t`. A body flying that at radius `r` traces an arc that
starts turning `r * tan(t/2)` blocks before the corner and passes `r * (1/cos(t/2) - 1)` blocks
**inside** it. Call the second number the overshoot. Substitute `r = v/w` and solve for `v`:

```
v_max = yawRate * corridorMargin / (1/cos(t/2) - 1)
```

`corridorMargin` is the design input: how far off the drawn line the flown arc is allowed to drift.
It matters because the search only ever certified the cells **on the line** as clear, so a cut
corner leaves certified airspace. The margin shrinks with how walled in the cell is (`BirdNode.enclosure`),
which is why that measurement is now kept rather than thrown away.

The shape of the multiplier, for reference:

| turn | `1/cos(t/2) - 1` | overshoot at r = 1 |
|---|---|---|
| 45 deg | 0.08 | 0.08 blocks |
| 90 deg | 0.41 | 0.41 blocks |
| 135 deg | 1.61 | 1.61 blocks |

So a 45 degree corner is nearly free and a 135 is ruinous, which is the same ordering the search's
`turnCost45/90/135` already encodes by hand. That is not a coincidence, see the last section.

## The three passes

Standard velocity profiling, the shape a CNC controller or a robot trajectory planner uses.

**1. Local limits.** Each node gets the tightest speed its own geometry allows, with no regard for
whether it is reachable: the corner rule above, the climb rule below, and a floor at `minSpeed`
because a bird that stops dead in mid air looks broken. The last node gets `arrivalSpeed`, which is
the one place allowed under the floor.

**2. Backwards.** Walk from the end, capping each node by what it can still brake down from. Drag is
the only brake available and it is geometric, so the closed form is exact:

```
maxEntrySpeed(exitSpeed, distance) = exitSpeed + distance * (1 - brakingDrag)
```

At the vanilla 0.91 that is 0.09 blocks per tick shed per block flown, which is *slow*: halving speed
from cruise takes about 1.1 blocks. That number is why the follower needs to see a corner one to two
blocks out and not on top of it, and it is the argument for eventually giving the controller a flare
(spread wings, pitch up) so `brakingDrag` can be lower than cruising drag.

**3. Forwards.** Walk from the start, capping each node by what it could have accelerated up to.
Simulated rather than solved, because accelerating against drag has an ugly closed form and this runs
once per path leg rather than once per tick. Tick order matches `LivingEntity.travel`: thrust, then
move, then drag.

One pass each is enough, and the order matters. Pass 3 can only ever raise a node's speed relative to
its predecessor (a body under thrust below terminal speed never slows), so it can never invalidate
the braking guarantee pass 2 established. Doing them the other way round would need iteration.

## The climb rule

Not a physical limit the way the corner rule is. Nothing stops the mob climbing fast; the point is
that a bird with no forward airspeed is hovering, which is slow and effortful, and pretending
otherwise is what makes vertical flight read as an elevator. Legs steeper than `maxClimbAngle` scale
down towards `hoverSpeedFraction`, reaching it on a purely vertical move.

This is the throttle layer's half of the problem `believable_bird_flight.md` section 4 describes. The
other half is a hard clamp in the follower (`|vy| <= tan(maxClimbAngle) * horizontalSpeed`), and the
real fix is in the search: the lattice can only express climb angles of 0, 35, 45 and 90 degrees, so
a gentle sustained climb currently comes out as a vertical zigzag. Longer move primitives fix that
properly and this rule stops mattering as much.

## What comes out

`ThrottleProfile`, indexed by arc length rather than node index, which is what the follower wants
since its cursor is a distance. Three things worth knowing:

- `speedLimitAt(d)` interpolates between nodes, so the ramps are continuous
- `limitAtNode(i)`/`arcAtNode(i)` are the raw node values, which is what the follower's braking
  correction walks (see below); a tightest-over-a-window query used to live here and was deleted, it
  brakes for the same corner twice
- `expectedFlightTicks()` is a correct replacement for vanilla's per-node timeout, which budgets from
  cruise speed and therefore fires spuriously on any path where the bird is deliberately slowing

## Why speed is not in the search state

The obvious alternative is to make the search state `(x, y, z, heading, climb, speed)` so A* can
trade off "detour wide and fast" against "go direct and slow". Analysed and rejected:

- **Growth is multiplicative, not exponential.** Three speed bins is 3x. That was 24 states per cell
  when this was written; the lattice has since spent its 3x on `climb` instead (see the
  up-and-over hop in `../pathfinding/PATHFINDING_NOTES.md`), so it is 72 now. Bad but not fatal on
  its own.
- **The cost is search range, not memory.** `maxVisitedNodes` is a fixed budget (`FOLLOW_RANGE * 16`,
  now multiplied by the states per cell). Tripling the states per cell divides the reach by three,
  and the reach is already short of the follow range. Raising the budget instead costs synchronous
  server-thread time, which is the thing that makes flocks impossible.
- **The turn costs already approximate the answer.** If speed is a known function of turn angle then
  so is the time lost at a corner, and that is what `turnCost45/90/135` is. Working the physics
  through at 8 deg/tick gives roughly 0, 3 and 18; the hand-tuned config holds 0.5, 5 and 20. Within
  a factor of two, and conservative in the right direction.

What is genuinely given up: the search will take a route through a tight gap that forces a crawl
rather than detouring around it at speed. For a bird wandering terrain that is arguably the correct
behaviour anyway.

The conclusion that follows: **spend state space on geometry, not on speed.** Heading resolution and
step length change what the line looks like, which is what makes flight read as flight. Speed can be
an annotation, and this package is that annotation.

## Watching it in game

`PathDebugRenderer` draws the profile as one arrow per node, along the direction of travel and as
long as the speed allowed there, ramped red to green against the envelope's top speed. Flat out is
long and green everywhere; arrows shrinking and reddening into a corner is the profile braking for
it, which is otherwise completely invisible.

Two summaries sit over the target:

```
throttle 0.031-0.081 b/t of 0.081, 6/34 limited, eta 512t
```

`limited` is the count to watch. Zero means the profile is not biting at all and every corner on the
route is flyable flat out, which at the test bird's speed is the normal answer: at 15 deg/tick and
0.081 b/t its turn radius is 0.31 blocks, well inside the corridor margin. The profile only starts
doing real work once the bird is either faster or less twitchy.

And on the mob itself, yellow for where the body is pointing, cyan for where it is actually going.
The angle between them is the sideslip from `believable_bird_flight.md` section 1, and a cyan arrow
shorter than the nearby path arrows is the mob failing to keep up with its own profile.

## How much of the follower reads it

`BirdFlightControl` takes its speed entirely from the profile, and reads it through the navigation,
which is the only layer that can see the cursor and how far off the line the mob is:

```java
// BirdFlightNavigation.speedLimitFor
bleedPerArc = ruler.groundPerArc() * (1 - brakingDrag)
limit       = min over nodes i within stoppingDistance(v) of
                  limitAtNode(i) + (arcAtNode(i) - cursor) * bleedPerArc,   floored at speedLimitAt(cursor)
limit       = max(limit, rejoinSpeed(offRoute))
// BirdFlightControl
target      = min(maxSpeed * speedModifier, navigation.getSpeedLimit())
throttle    = envelope.throttleForThrust(envelope.thrustToReach(target, currentSpeed))
```

One point, no lookahead, is the whole payoff of pass 2: the ramp *is* the lookahead, already computed,
and reading the tightest limit over a window on top of it brakes for the same corner twice.
Interpolating between nodes is exact rather than approximate here, since `maxEntrySpeed` is linear in
distance and so is `speedLimitAt`.

The `bleedPerArc` walk is not a second lookahead sneaking back in. Pass 2 guarantees
`limit[i] <= limit[j] + (arc[j] - arc[i]) * (1 - drag)`, so at `groundPerArc = 1` the walk can never
find anything tighter than the value underfoot and costs exactly nothing. It only bites when the mob
is cutting a corner: then it covers arc faster than it covers ground, drag sheds per block *flown*,
and the profile's own guarantee is the thing that stops holding. Same inequality, real braking
authority substituted in.

The throttle is a deadbeat servo rather than the old `throttleToHold(target) + error * speedGain`
pair. `travel()` does `v' = drag * (v + a)`, so `a = target/drag - v` lands exactly on the target and
can never overshoot it; the gain that guessing at `speedGain` was approximating is `1/drag`, and it
was set low enough that the mob sat above the profile through most corners.

The target was originally written as `speedLimitOver(cursor, max(lookahead, stoppingDistance(v)))`, which is
worth recording as a trap. `stoppingDistance` is `11.1 * v` and the bird's top speed is 0.081 b/t, so
the term never exceeded 0.9 and the 1.5 block `lookahead` floor won at every speed the mob could
reach. With `arrivalSpeed` at 0 that cut thrust a fixed 1.5 blocks from the end, coasting covered
0.63, and the mob halted 0.87 blocks short of every destination and hung there until the stuck
detector killed the path. Paths shorter than 1.5 blocks were refused outright.

That deleted `remainingAlongPath`, `brakeFactor`, `turningSpeedFactor` and the four config knobs
behind them. Deliberately kept as a single source of slowdown: if the bird still cuts a corner, it is
the profile's numbers being wrong rather than two slowdown mechanisms fighting.

The follower is no longer the old one: it tracks arc length, sizes its carrot by how much room the
path has here, and has an absorbing arrival. See `believable_bird_flight.md` sections 1, 2 and 6.
