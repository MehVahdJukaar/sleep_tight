# Making the bird believable - design analysis

Written 2026-07-27 against the current `controller/` + `pathfinding/` code. This is the *forward
looking* companion to `MOB_AI_NOTES.md` (which documents vanilla and what was already replaced) and
`pathfinding/PATHFINDING_NOTES.md` (which documents the search). Nothing here is implemented yet.

The question this answers: what stands between the current follower and flight that reads as a bird,
with no artifacts like 180 degree turns when a node is narrowly missed.

**Update 2026-07-28.** Parts of this have since been built as a separate layer, `throttle/`, which
sits between the search and the follower and works out a speed limit for every point of a path. It
absorbs the speed scheduling asked for in sections 3 and 4, the arrival ramp in section 6 and the
per-node timeout complaint at the end of section 3, and it deletes rather than adds follower code.
See `FLIGHT_ARCHITECTURE.md` and `throttle/THROTTLE_NOTES.md`. Sections 1, 2 and 5 (velocity
steering, arc-length pure pursuit, banking) are still the outstanding follower work, and section 9's
ordering still holds for them.

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

1. Project the mob onto the polyline, searching within a bounded window either side of the current
   cursor.
2. Move the cursor to that projection, by no more than a small multiple of the ground actually
   covered this tick.
3. Place the carrot at `arclength(cursor) + L` and hand *that* to `setWantedPosition`.

What falls out: missing a node stops being an event, the commanded heading is continuous, and corners
round off with radius ~`L` which is what a bird does. `waypointRadius` and `passedWaypointRange` both
disappear as concepts. `Path.advance()` is still called so `isDone()` and stuck detection keep
working, just driven by the projection instead of by proximity.

Two things to get right:

- **The window is mandatory, the ratchet is not.** The lattice can emit hairpins (two 90 degree turns
  to reverse), and a global nearest-point search snaps the cursor onto the return leg and shortcuts
  it, possibly through geometry. A window of a couple of blocks fixes that on its own, and it does so
  whichever way the mob is moving. Forcing the cursor to be monotone on top of that was tried and is
  wrong: a mob shoved sideways off a diagonal watches its own perpendicular foot slide up the line,
  which is real geometry, and a ratchet then refuses to give that progress back when it flies home.
  It reads "almost arrived" from a dozen blocks out, is told to slow to arrival speed there, and
  creeps until a watchdog kills it. The step cap in 2 is what keeps the cursor honest instead: it
  cannot outrun distance actually flown by more than the ratio corner-cutting can legitimately
  produce (~1.4 at the sharpest lattice corner).
- **Pure pursuit corner-cuts by design**, by up to roughly `L`. That cut must fit inside the clearance
  `BirdNodeEvaluator` guaranteed. Rather than one conservative `L` everywhere, scale it on the node's
  measured enclosure: open air gets a long smooth carrot, a boxed-in cell a short accurate one, since
  the search only certified the cells *on* the line as clear. Being off the line pulls it in further,
  which is what turns a lazy rejoin into an actual correction.

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

**Built, and the prediction below was right.** Raising `FLYING_SPEED` to 1.0 tripled the speed and,
with a fixed 15 deg/tick, tripled the radius to the predicted 0.77 - corners overshot everywhere,
and the corner rule stayed silent throughout because its 90 degree limit (`0.221 b/t`) sits just
above the new top speed (`0.202`). Mystery drift, exactly as called.

**And that was only the horizontal half.** Velocity steering (section 1) was only ever applied to the
heading, so vertical velocity stayed at the mercy of thrust alone: the flight path bends at
`accel/speed`, giving a vertical turn radius of `speed^2/accel`, which at terminal speed is
`speed*drag/(1-drag)` - about **10x the speed in blocks**, whatever the throttle. 0.6 blocks at the
old crawl, 2.0 at full thrust, against a pure pursuit arc that can never be wider than `lookahead/2`
= 0.75. Past roughly `0.075 b/t` the bird simply cannot follow a pitch change.

Observed exactly: on a path 2 blocks out and 5 down, the bird held level, flew off the end, found the
carrot directly beneath it, took an arbitrary heading out of the collapsed horizontal component
(`yawTowards` on numerical residue), flew a 180 degree circle, and only descended once the turn had
bled the speed off. `turnVelocityPitch` steers the velocity's climb angle the same way the yaw half
steers its heading, so both planes turn at `maxYawRate` and the vertical radius is `speed/omega` too.
`maxSpeedForPitchChange` stopped being a square root at the same time.

The horizontal fix inverts which of the two is configured. `BirdFlightConfig.turnRadius` is now the knob and
`FlightEnvelope` derives `omega = maxSpeed / turnRadius`, so the radius is what stays put when speed
moves. Falls out of it: the corner rule's limits become a fixed *fraction* of top speed
(`margin / (r * overshoot)` has no speed in it), so the profile's shape no longer depends on how fast
the bird is, and `maxTurnBins` can be checked against `turnRadius` once instead of per speed.

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
   **In as of 2026-08-02**: body pitch is its own synched field rather than a second use of `xRot`,
   the renderer rotates the body by it, and `TestMobModel` adds it straight back onto the head, which
   cancels it exactly. So the head holds level through a dive and still tracks whatever a look goal
   picked, on the vanilla `LookControl` that `xRot` now belongs to again.
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

**All of this is in as of 2026-08-02**, in `controller/BirdStateMachine`: takeoff and perch landed
2026-08-01 with the gravity ownership the two need, walking joined them, and the flare arrived with
the `FLUTTERING` mode. Cruise is the sections 1-4 controller as before.

The flare came out cheaper than this section expected. Rather than a fifth gait with its own speed
demands on the profile, the mode a bird is in when its feet are off and it has no path to fly does
the job: it pitches along its travel scaled by horizontal speed, so a bird whose momentum has bled
away on the approach is level and beating by construction, with nothing to enter and nothing to
absorb. The same mode covers hops, gap strides and ledges - see `WALK_GAP_BUG.md`.

Feet down is a **state, not a measurement**. Nothing about velocity or `onGround` alone separates a
bird gripping a branch from one hovering an inch over it, so it is a synched boolean the gait
control sets and everything else reads. Three things hang off it:

- the search plans a departure in **any** direction from a grounded start, because a bird on its feet
  carries no airspeed to conserve (`BirdNode.freeHeading`, and `PATHFINDING_NOTES.md` invariant 5).
  Before that, the start heading was seeded from body yaw whatever the mob was doing, and a bird
  parked facing the back of a dead end had no legal horizontal move at all - the two purely vertical
  moves are the only ones exempt from the turn cap - so it climbed straight out of a corridor it
  could have flown down.
- **takeoff turns on the ground first.** The above is only honest if the mob really can leave that
  way, and the flight follower cannot deliver it: it turns at a flying bird's rate, so a path leaving
  behind the mob would be flown as a wide arc through whatever it was perched against. So the ground
  control pivots the mob on the spot to the path's launch heading, and only then is flight allowed to
  start: `BirdFlightNavigation.tick` and `BirdFlightControl.tick` both stand down while
  `isHoldingForLaunch()`. Turning on your feet is free and instant in path terms, and it is what feet
  are for.

- **short hops are walked, not flown.** A destination is only considered for walking from a
  standstill, within `walkMaxDistance` horizontally and `walkMaxRise` vertically, and only if a real
  ground path comes out cheaper in ticks than flying one. Cheaper counts the launch pivot, the spool
  and the descent against the flight, since that overhead is what makes a four-block flight look
  silly, and it multiplies the walk by `walkCostPenalty`, since this bird's MOVEMENT_SPEED is
  actually quicker than its cruise. `controller/WalkOrFly` is that decision and nothing else.

Walking is a **swap, not a blend**: the mob carries the lattice flier and a plain vanilla
`GroundPathNavigation` + `MoveControl`, and installs exactly one pair at a time, in
`customServerAiStep` so a swap never lands between a navigation and the move control it was feeding.
The flight stack did not have to change to accommodate it, because `PerchingFlier` was the whole
contract between the halves and walking is just another way of having your feet down.

Body pitch belongs to the gait too, as a target per gait approached at `maxPitchPerTick` rather than
a value anything writes outright: the flown slope while airborne, level otherwise, and an override
for anything that wants to pose the bird mid-flight. Landing always wins over the override. It used
to be written onto `xRot` by the move control, which meant it shared a field with where the mob was
looking and was only updated on ticks with a path to fly, so a bird that landed mid dive stayed nose
down until it next took off.

`setNoGravity` used to be set in the `BirdTestMob` constructor and again every move control tick, and
never cleared - a bird that can never land is not a bird, and a dead or AI-disabled one floats. It is
now `BirdStateMachine`'s alone: on while airborne, off from the moment it commits to descending, and
forced off by `BirdTestMob.tick` for a mob whose AI is not running at all, which is the case the
gait control cannot see because it rides on `customServerAiStep`.

## 7. Artifact and edge case catalogue

| Artifact | Cause | Prevented by |
|---|---|---|
| 180 on a missed node | acceptance sphere ends up behind the mob | arc-length cursor, no acceptance test |
| Orbiting a node forever | acceptance radius < turn radius | carrot instead of sphere; assert `L > r` |
| Cutting across a hairpin, possibly through blocks | global nearest-point projection | windowed projection |
| Cursor pinned at the apex of a reversal, then walking backwards as the mob flies home | outbound and return legs of a 180 are the same points in space, so first-strictly-better always picks the outbound one | ties in projection offset go to the candidate further along (`PathRuler.PROJECTION_TIE`) |
| Stranded after a shove, creeping at arrival speed from far out | monotone cursor keeps progress the perpendicular foot gave it | cursor may lose ground, capped against distance flown; speed floored while far off the line |
| Several nodes consumed in one tick, heading jumps | `waypointRadius` equals node spacing | arc-length carrot |
| Flying backwards while turning | thrust applied along yaw when the error exceeds 90 deg | zero forward thrust past ~120 deg, or a pivot mode |
| Yaw wobble on a near-straight leg | no deadband on the yaw command | deadband, or the lookahead absorbing it |
| Elevator ascent | vertical axis independent of forward | max climb angle coupling |
| Oscillation at the destination | no absorbing arrival state | explicit flare/perch mode |
| Jerk on replan | new path starts from a stale heading assumption | seed the search from current yaw *and* from a start cell projected slightly ahead, since the search costs a tick and the mob cannot turn instantly |
| Ping-pong on unreachable targets | `PathFinder` returns a best-partial path on budget exhaustion; mob arrives, replans, repeats | check `Path.canReach()`, randomized cooldown, fall back to a random reachable pos (`MoveToTargetSink` is the reference) |
| False "stuck" while hovering or circling | `doStuckDetection` compares 100-tick displacement against `speed*100*0.25`, which a deliberately hovering or slowly circling bird fails | flier-specific stuck rule, or suspend it outside cruise mode |
| Turn-heavy path abandoned mid-flight | per-node timeout budgets from cruise speed; follower slows in turns | budget conservatively or drop the timeout |
| Corner cut into geometry | pure pursuit cuts by up to `L`; planner only guaranteed clearance on the polyline | scale `L` on the node's enclosure so `L` <= the room actually measured there |
| Above the profile through a corner | the cut covers arc faster than ground, so drag sheds less than the planner assumed | scale braking authority by ground covered per block of route |

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
