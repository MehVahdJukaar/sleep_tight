# ThrottlePlanner, step by step

Plain walkthrough of what actually happens when a path is handed to `ThrottlePlanner`. For *why* the
layer exists at all, and for the reasoning behind the design choices, read `THROTTLE_NOTES.md`. For
where it sits relative to the search and the follower, read `../FLIGHT_ARCHITECTURE.md`.

The one sentence version: **it turns a line into a line with a speed limit painted along it.**

---

## Input

`ThrottlePlanner.fromPath(path, entity, envelope)` pulls exactly two things off the path:

- **positions**, one per node, through `path.getEntityPosAtNode`. Those are the points the follower
  will actually steer at, rather than raw cell corners.
- **`enclosure`**, the 0..1 "how walled in is this cell" the search already measured and stored on
  `BirdNode`.

It deliberately does **not** read `heading`, even though the lattice nodes carry it. Heading is
search state; the turn angle is recomputed from the geometry, which is the ground truth and which
also survives `PathNavigation.trimPath` replacing a `BirdNode` with a plain vanilla `Node`.

Plus the `FlightEnvelope`: yaw rate, top speed, minimum speed, drag, acceleration, corridor margin,
maximum climb angle.

---

## Step 0: measure the path

Walk the points once, adding up the distance between them. Every node now has an **arc length**:
node 0 sits at 0, node 1 at 1.0, node 7 at 8.4, and so on.

From here on nothing thinks in node indices. Everything is "how far along the path", because that is
what the follower's cursor is.

---

## Pass 1: what does each node's own shape allow?

Two questions per node, and the smaller answer wins.

### Is it a corner?

Measure the angle between the leg arriving at the node and the leg leaving it. Then:

```
speedLimit = yawRate × margin / (1/cos(angle/2) − 1)
```

- `yawRate` is how fast the mob can turn, radians per tick
- `margin` is `corridorMargin × (1 − enclosure)`: how far off the drawn line the flown arc is allowed
  to drift, shrunk by how boxed in this particular cell is
- the denominator is how far a turn of that angle throws you off the line per block of turn radius

This one line is the entire reason the layer exists. It converts "this corner is 90 degrees" into
"you may be doing 0.118 blocks per tick when you get here".

| turn | denominator | meaning |
|---|---|---|
| 45 deg | 0.08 | barely throws you off the line at all |
| 90 deg | 0.41 | throws you off by 0.41 blocks per block of radius |
| 135 deg | 1.61 | ruinous |

### Is it steep?

If the leg's pitch is steeper than `maxClimbAngle`, scale the speed down toward `hoverSpeedFraction`,
reaching it on a purely vertical hop. A climbing bird with no forward airspeed is hovering, and
hovering is slow and effortful. Below `maxClimbAngle` this does nothing.

### Then clamp

Floor everything at `minSpeed`, because a bird that stops dead in mid air looks broken. The last node
is the exception: it gets `arrivalSpeed`, which is allowed to be zero.

**After pass 1** you have a speed per node that completely ignores whether it can be reached.

---

## Pass 2: walk backwards. Can it slow down in time?

Drag is the only brake the mob has, and drag is geometric, so the exact answer is one line:

```
maxEntrySpeed = exitSpeed + legLength × (1 − brakingDrag)
```

At vanilla's 0.91 that is **0.09 blocks per tick shed per block flown**, which is slow: halving speed
from cruise takes about 1.1 blocks.

Walking from the end and applying `limit[i] = min(limit[i], maxEntrySpeed(limit[i+1], legLength))`
spreads every slow node backwards into a braking ramp.

---

## Pass 3: walk forwards. Can it speed up in time?

The same idea mirrored: no node may be faster than what could have been accelerated to on the way in.

Simulated tick by tick rather than solved, because accelerating against drag has no clean closed form
and this runs once per path leg rather than once per tick. Tick order matches `LivingEntity.travel`:
add thrust, then move, then apply drag.

This is what stops the profile promising 0.2 one block after a corner that forced 0.05.

### Why one pass each is enough

The order is load-bearing. Pass 3 can only ever raise a node's speed relative to its predecessor (a
body under thrust below terminal speed never slows down), so it can never undo the braking guarantee
pass 2 established. Doing them the other way round would need iterating to a fixed point.

---

## Worked example

Five nodes: straight, a 90 degree corner, straight, landing. Bird at 8 deg/tick, top speed 0.2, open
air, drag 0.91.

| node | arc | pass 1 | pass 2 | pass 3 | final |
|---|---|---|---|---|---|
| n0 | 0 | 0.200 | 0.200 | 0.200 | **0.200** |
| n1 | 1 | 0.200 | 0.200 | 0.200 | **0.200** |
| n2, corner 90 deg | 2 | **0.118** | 0.118 | 0.118 | **0.118** |
| n3 | 3 | 0.200 | **0.090** | 0.090 | **0.090** |
| n4, arrival | 4 | 0.000 | 0.000 | 0.000 | **0.000** |

Read it as: cruise, ease off into the corner, take the corner at 0.118, keep braking because there
are only 2 blocks left in which to stop. Note that n3 was pinned by pass 2, not by any corner.

**The same path with the current test bird** (15 deg/tick, top speed 0.081): the corner rule returns
0.221, far above the bird's top speed, so it never bites and the profile is flat at maximum until the
arrival ramp. That is the correct answer and not a bug. At that speed the bird's turn radius is 0.31
blocks and it can pivot inside its own corridor. The profile only starts doing real work once the
bird is either faster or less twitchy.

---

## Output

A `ThrottleProfile`: two parallel arrays, `arc[]` and `limit[]`, plus the expected flight time. Four
things to ask it:

| call | what it is for |
|---|---|
| `speedLimitAt(d)` | the limit at that arc length, interpolated so the ramps are continuous. **This is the one the follower reads.** Pass 2 already turned every downstream limit into a ramp leading here, so this single point is the fastest it can be and still make everything ahead. Interpolation is exact, not approximate: `maxEntrySpeed` is linear in distance, so a braking ramp is a straight line |
| `expectedFlightTicks()` | how long this flight should take. The correct budget for a path timeout, unlike vanilla's cruise-speed guess, which fires spuriously as soon as the bird slows for a corner |
| `limitAtNode(i)`, `arcAtNode(i)` | per node, for the debug renderer's speed arrows and for `BirdFlightNavigation`'s braking correction, which re-derives pass 2's inequality with the follower's real braking authority in place of the profile's assumed one |

There used to be a `speedLimitOver(d, window)` here, the tightest limit in the next `window` blocks.
It is gone: reading it instead of `speedLimitAt` brakes for the same corner twice, once where the
planner put the ramp and again on the approach.

---

## One design point that is easy to misread

Pass 3 starts from `limit[0]`, the envelope's top speed, **not** from the mob's actual current speed.
So the profile can promise a speed at node 1 that a mob starting from a standstill will not reach.

That is deliberate, and it errs in the safe direction. Being *under* the limit is always fine: the
follower just accelerates toward it and arrives late. Being *over* is what cuts corners into walls.
If the profile started from the mob's live speed it would have to be replanned every time the mob
fell behind, and it would stop being an immutable property of the path.
