# Making the bird believable - design analysis

Written 2026-07-27 against the current `controller/` + `pathfinding/` code. This is the *forward
looking* companion to `MOB_AI_NOTES.md` (which documents vanilla and what was already replaced) and
`pathfinding/PATHFINDING_NOTES.md` (which documents the search). Nothing here is implemented yet.

The question this answers: what stands between the current follower and flight that reads as a bird,
with no artifacts like 180 degree turns when a node is narrowly missed.

## 1. The dominant problem is sideslip, and it is quantifiable

`MOB_AI_NOTES.md` section 10 got the braking analysis right but stopped one step short. The same drag
number that causes overshoot also breaks turning, much more severely.

Horizontal air drag is a flat `0.91`, so velocity is a first-order lag with time constant
`tau = 1/(1-0.91) = 11 ticks`. Thrust is applied along `yRot` only, and nothing in `travel()` damps
velocity perpendicular to the body. In a sustained turn at rate `omega`, the velocity vector settles
at a lag angle behind the body of

```
phi = atan(omega * tau)
```

With `maxYawPerTick = 15` degrees (`omega = 0.262 rad/tick`):

| | value |
|---|---|
| `omega * tau` | 2.91 |
| **velocity lag behind body** | **71 degrees** |
| speed retained in the turn, `(1/tau)/sqrt(1/tau^2+omega^2)` | **0.33x** |
| combined with `minTurnSpeedFactor = 0.35` | ~0.11x cruise |

So through every hard turn the bird points roughly 70 degrees away from where it is moving, and
nearly stops. Visually that is a hovering drone crabbing sideways. It also explains overshoot better
than the braking analysis does: the mob leaves the turn still carrying velocity along the *old* leg.

The trap: this cannot be fixed by lowering the yaw rate. Keeping `phi` under 20 degrees needs
`omega < 1.9 deg/tick`, a 190 tick full circle. The physics cannot track any yaw rate that reads as
a bird.

Three ways out, in increasing order of preference:

1. **Anisotropic drag.** Split `deltaMovement` into along-body and across-body components in
   `travel()`, 0.91 along and roughly 0.5 across. `tau_perp = 1.4`, so `phi` drops to 20 degrees.
   Physically what a wing does. Keeps thrust/drag as the only mechanism.
2. **Kinematic velocity steering.** Each tick, rotate the horizontal component of `deltaMovement` by
   the yaw delta the move control just applied; drag and thrust then govern magnitude only. No
   sideslip at all, no new constant to tune, and turn radius becomes exactly `v/omega`, which is the
   property we actually want (section 3).
3. Leave physics alone and cheat the render. Hides nothing that matters - overshoot and path tracking
   are unaffected. Not worth it.

Take (2). It is fewer lines than the current braking code, it makes the follower analytically
predictable, and it removes the reason `minTurnSpeedFactor` exists. Rotate by ~90% of the yaw delta
rather than 100% if the turns should feel weighted rather than on rails.

## 2. Waypoint chasing is the wrong shape of follower

This is the direct cause of the 180s. `followThePath` currently asks a boolean question ("am I close
enough to node i?") about a discrete node. Every failure of that controller class is structural:

- Miss the acceptance sphere on the outside of an arc and the target is behind you. `hasPassedWaypoint`
  patches this, but only within `passedWaypointRange = 3`. Drift 4 blocks wide and the mob turns around.
- `waypointRadius = 1.0` equals the lattice node spacing. On accepting node i you are frequently
  already inside node i+1's sphere, so the target jumps two or three nodes in one tick and the
  heading command is discontinuous.
- If the acceptance radius ever drops below the turn radius, pursuit becomes a limit cycle and the
  mob orbits the node forever. Geometric certainty, not a tuning failure.
- Every acceptance is a step change in commanded direction. Even working perfectly, the yaw command
  is piecewise constant, which is exactly what the lattice's turn costs were paid to avoid.

**Replace it with arc-length pure pursuit.** At `moveTo` time, resample the path once into a polyline
with cumulative arc lengths (O(n), cached on the path). Then per tick:

1. Project the mob onto the polyline, searching **forward only** from the current cursor, within a
   bounded window.
2. Advance the cursor to that projection. Monotone by construction.
3. Place the carrot at `arclength(cursor) + L` and hand *that* to `setWantedPosition`.

What falls out: missing a node stops being an event, the commanded heading is continuous, corners
round off with radius ~`L` which is what a bird does, and a 180 becomes impossible because the cursor
never moves backwards. `waypointRadius` and `passedWaypointRange` both disappear as concepts.
`Path.advance()` is still called so `isDone()` and stuck detection keep working, just driven by the
projection instead of by proximity.

Two things to get right:

- **Forward-only windowed projection is mandatory.** The lattice can emit hairpins (two 90 degree
  turns to reverse). A global nearest-point search snaps the cursor onto the return leg and shortcuts
  the hairpin, possibly through geometry. Bound the window to a few blocks of arc length ahead.
- **Pure pursuit corner-cuts by design**, by up to roughly `L`. That cut must fit inside the clearance
  `BirdNodeEvaluator` guaranteed. Either keep `L` at or below the clearance margin, or shorten the
  carrot when the straight line to it is blocked.

Optional: run the carrot along a Catmull-Rom spline through the nodes instead of the raw polyline.
Marginal smoothness gain given pure pursuit already rounds corners, but it yields a curvature signal
for free, which is directly useful for bank angle and speed scheduling.

Efficiency note: `remainingHorizontalDistance()` walks up to 16 blocks of nodes *every tick*, calling
`getEntityPosAtNode` each time. Precomputing cumulative arc length once per path makes both the
braking distance and the carrot O(1)/O(window). Irrelevant for one test mob, relevant for a flock.

## 3. Planner and follower must agree on turn radius

The invariant that keeps the system honest:

```
planner minimum turn radius  >=  follower achievable turn radius at cruise
```

Violate it and the follower cannot fly what the planner drew, falls behind, and every artifact in
section 7 follows.

`maxTurnBins = 2` lets the search request 90 degrees per 1 block step. The follower at
`maxYawPerTick = 15` and cruise `0.061 b/t` covers a block in 16 ticks, so it can turn 240 degrees
per block. Compatible on paper. Once velocity tracks heading (section 1) the real constraint is
`r = v/omega`: **0.23 blocks at follow speed, 0.77 blocks at full throttle.** So `maxTurnBins` is not
currently the limiting factor - the sideslip is.

Worth building anyway: expose `maxYawPerTick` as the single source, derive `r = cruiseSpeed/omega`
from it, and assert the lattice's turn cap is no tighter than `r`. When the mob's speed is raised
later (a fleeing bird at full throttle is 3.3x the test speed, `r = 0.77`) the mismatch is caught
instead of showing up as mystery drift.

Related mismatch: **speed is not part of the search state.** The lattice is `(x,y,z,heading)`, so it
prices a turn but assumes constant speed, and the follower then slows in turns. Vanilla's per-node
timeout (`distance/speed*20` ticks, 3x budget) therefore fires spuriously on turn-heavy paths. Budget
from a conservative speed or replace the timeout.

## 4. Vertical is decoupled and should not be

`applyVerticalThrust` drives `yya` from altitude error with its own approach band, independent of
forward motion. That is an elevator. Two consequences:

- The mob can climb faster than it flies forward - the helicopter ascent already flagged in the notes.
- `matchPitchToVelocity` then faithfully renders a 60 degree nose-up attitude on a body with no
  forward airspeed, which reads worse than not pitching at all.

The fix is a coupling constraint, not more tuning: clamp `|vy| <= tan(maxClimbAngle) * horizontalSpeed`
with `maxClimbAngle` around 25-35 degrees. A bird needing altitude with no forward room then
physically cannot get it in a straight line, which is correct and pushes the problem back to the
planner, where the lattice already solves it (that is what `straightUpCost` is for). It also makes
genuine vertical flight read as a deliberate, effortful hover rather than as the default.

Second order but cheap: climb should cost speed and descent should buy it. A small term coupling `vy`
into forward thrust makes dives and pull-ups read as energy trades instead of independent axes.

## 5. What actually makes flight read as flight

Ranked by believability per line of code:

1. **Velocity aligned with the body.** Section 1. Nothing else matters until this is true.
2. **Bank proportional to yaw rate.** Vanilla entities have no roll, so this lives in the model,
   driven client-side off the `yRotO -> yRot` delta, smoothed, and slightly *leading* the turn. No
   sync needed since yaw is already interpolated. Biggest visual win in the list.
3. **Continuous curvature.** Pure pursuit gives this. A jerk limit (rate-limit the change in yaw
   rate, not just yaw) makes the bird ease into and out of turns instead of snapping to full rate.
4. **Head stabilization.** Birds hold their head level and locked while the body rotates underneath.
   `BirdLookControl` already frees pitch for the move control; the complement is keeping *head* pitch
   near level and letting head yaw track a target independently of the violently moving body.
5. **Never fully stop in cruise.** A bird decelerating to zero mid-air looks broken.
   `minTurnSpeedFactor` acknowledges this; it should be a floor on the whole controller, not just on
   turns.
6. **Flap/glide tied to vertical thrust.** Flapping while diving is the classic tell.
7. **Path noise.** Real birds do not fly straight lines. A slow low-amplitude perturbation applied to
   the *carrot* (not to velocity, so it never fights the controller) is a few lines and dissolves the
   remaining machine feel.

## 6. Missing: flight modes

One PD law is covering cruise, climb, arrival and landing, and the endpoints are where it looks worst.
Real bird movement reads as distinct gaits. At minimum:

- **Takeoff** - burst of vertical, then convert to forward. Distinct from cruise climb.
- **Cruise** - pure pursuit, banked; the sections 1-4 controller.
- **Approach / flare** - entered within some distance of the final node. Level out, bleed horizontal
  speed, pitch up, kill velocity. Critically, **arrival is absorbing**: once flaring, never re-target
  the final node. The current controller will overshoot the last node, turn back, overshoot again and
  oscillate. That is the second most likely 180 after the missed-node one.
- **Perch** - `setNoGravity(false)`, zero velocity, hand off to ground navigation.

`setNoGravity(true)` is currently set in the `BirdTestMob` constructor and again every move control
tick, and never cleared. A bird that can never land is not a bird, and a dead or AI-disabled one floats.

## 7. Artifact and edge case catalogue

| Artifact | Cause | Prevented by |
|---|---|---|
| 180 on a missed node | acceptance sphere ends up behind the mob | monotone arc-length cursor |
| Orbiting a node forever | acceptance radius < turn radius | carrot instead of sphere; assert `L > r` |
| Cutting across a hairpin, possibly through blocks | global nearest-point projection | forward-only windowed projection |
| Several nodes consumed in one tick, heading jumps | `waypointRadius` equals node spacing | arc-length carrot |
| Flying backwards while turning | thrust applied along yaw when the error exceeds 90 deg | zero forward thrust past ~120 deg, or a pivot mode |
| Yaw wobble on a near-straight leg | no deadband on the yaw command | deadband, or the lookahead absorbing it |
| Elevator ascent | vertical axis independent of forward | max climb angle coupling |
| Oscillation at the destination | no absorbing arrival state | explicit flare/perch mode |
| Jerk on replan | new path starts from a stale heading assumption | seed the search from current yaw *and* from a start cell projected slightly ahead, since the search costs a tick and the mob cannot turn instantly |
| Ping-pong on unreachable targets | `PathFinder` returns a best-partial path on budget exhaustion; mob arrives, replans, repeats | check `Path.canReach()`, randomized cooldown, fall back to a random reachable pos (`MoveToTargetSink` is the reference) |
| False "stuck" while hovering or circling | `doStuckDetection` compares 100-tick displacement against `speed*100*0.25`, which a deliberately hovering or slowly circling bird fails | flier-specific stuck rule, or suspend it outside cruise mode |
| Turn-heavy path abandoned mid-flight | per-node timeout budgets from cruise speed; follower slows in turns | budget conservatively or drop the timeout |
| Corner cut into geometry | pure pursuit cuts by up to `L`; planner only guaranteed clearance on the polyline | `L` <= clearance margin, or raycast-shorten the carrot |

## 8. Limitations that cannot be engineered around

- **Client interpolation eats the smoothness.** The server sends position deltas and the client lerps
  over 3 ticks; `yRot`, `yHeadRot` and position arrive on separate packets with independent
  thresholds. Vanilla mobs are slow enough that nobody notices. A fast, continuously banking bird
  will show rounded corners and yaw/position desync on the client that no server-side smoothing
  fixes. Budget for this before tuning server-side turn rates to perfection, and consider deriving
  bank purely client-side from interpolated yaw so at least the roll is smooth.
- **Search cost.** `FOLLOW_RANGE = 64` means a 1024 node budget and a `PathNavigationRegion` copy
  scaling with range cubed, and the heading dimension makes the state space 8x vanilla's. Fine for
  one test mob, not for a flock. Decouple the search range from `FOLLOW_RANGE` (currently doing
  triple duty as attribute, region size and node budget), and expect partial paths to be far more
  common than in vanilla.
- **Pathfinding is synchronous on the server thread.** Nothing in this design changes that.
- **`FLYING_SPEED` is being used as a thrust fraction clamped to [0,1]**, not as a speed. At 0.4 the
  mob can never exceed 40% throttle regardless of `speedModifier`. It works, but the attribute no
  longer means what it means everywhere else, and a flee-speed modifier above 2.5 silently does
  nothing.
- **Vanilla costs are node-local.** Already worked around with `getEdgeCost`, but it caps how much of
  the flight model can live in the search. Speed-aware planning would need an
  `(x,y,z,heading,speed)` lattice, another multiplier on the state space. Probably not worth it;
  schedule speed in the follower from path curvature instead.

## 9. Suggested order of work

1. Velocity steering (section 1). Everything else is measuring a system fighting itself until this
   lands.
2. Arc-length pure pursuit replacing `followThePath` (section 2). The direct answer to the 180s.
3. Absorbing arrival / flare mode (section 6). Kills the second class of 180.
4. Max climb angle coupling (section 4).
5. Client-side bank from yaw rate (section 5.2). Cheapest believability win; do it whenever.
6. Flier-appropriate stuck detection and replan handling (section 7).

Steps 1 and 2 together delete `minTurnSpeedFactor`, `waypointRadius`, `passedWaypointRange` and most
of `brakeFactor`'s reason to exist, so `BirdFlightConfig` gets smaller rather than larger.

## Caveat on the numbers

The drag and acceleration constants come from `MOB_AI_NOTES.md` section 10, not from an independent
reading of `LivingEntity.travel`. The lag-angle derivation in section 1 is new and follows from those
constants; if the 0.91 figure is wrong the 71 degrees moves. The qualitative conclusion holds for any
drag above roughly 0.8: velocity cannot track a bird-like yaw rate under multiplicative-only drag.

Nothing in the steering half has been measured in game. It has only been reasoned about from vanilla
physics, which is also what `PATHFINDING_NOTES.md` section "Not done yet" says.
