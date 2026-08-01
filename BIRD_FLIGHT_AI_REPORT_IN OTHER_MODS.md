# Bird Flight AI in Alex's Mobs - How It Stays Believable

Analysis of the "bird line" mobs in Alex's Mobs (1.21 branch): **Crow, Blue Jay, Bald Eagle,
Seagull, Toucan, Potoo, Hummingbird**. All paths below are relative to
`src/main/java/com/github/alexthe666/alexsmobs/`.

The interesting thing about these mobs is that almost none of the believability comes from
pathfinding. It comes from a small set of tricks layered on top of a deliberately dumb
steering controller. This report walks through the architecture, then the specific tricks,
then what breaks.

---

## 1. The core pattern: two mobs in one entity

Every bird except the Hummingbird is really two locomotion systems that get hot-swapped at
runtime. `switchNavigator(boolean onLand)` replaces both `moveControl` and `navigation`
whenever the synched `FLYING` flag flips:

```java
// entity/EntityCrow.java:152
private void switchNavigator(boolean onLand) {
    if (onLand) {
        this.moveControl = new MoveControl(this);
        this.navigation = new GroundPathNavigation(this, level());
        this.isLandNavigator = true;
    } else {
        this.moveControl = new FlightMoveController(this, 0.7F, false);
        this.navigation = new DirectPathNavigator(this, level());
        this.isLandNavigator = false;
    }
}
```

The swap is driven from `tick()`, server-side only, and is idempotent-guarded by
`isLandNavigator` so it only allocates on an actual state change:

```java
// entity/EntityCrow.java:305
if (!this.level().isClientSide) {
    final boolean isFlying = isFlying();
    if (isFlying && this.isLandNavigator)   switchNavigator(false);
    if (!isFlying && !this.isLandNavigator) switchNavigator(true);
    if (isFlying) {
        timeFlying++;
        this.setNoGravity(true);
        ...
    } else {
        timeFlying = 0;
        this.setNoGravity(false);
    }
}
```

Two consequences worth noting:

- **Gravity is a mode, not a force.** A flying bird has `noGravity = true`; it does not fight
  gravity, it simply opts out of it. All vertical motion while airborne is authored by the
  move controller. Landing is `setNoGravity(false)` plus a fall.
- **`timeFlying` is the master clock.** It resets to 0 on every landing and is the single
  input that all the "how long has this bird been up" decisions read. This is what gives
  flight a duration budget rather than being a permanent state.

Fall damage is uniformly cancelled (`causeFallDamage` returns false, `checkFallDamage` is
empty), so the "cut gravity and drop" landing is safe by construction.

### Per-species configuration

| Mob | Air move control | Accel factor | Y-support | Air navigation | Flight budget (ticks) |
|---|---|---|---|---|---|
| Crow | `FlightMoveController(0.7f, false)` | 0.05 × 0.7 | no | `DirectPathNavigator` | climb < 50, wander < 200 |
| Blue Jay | `FlightMoveController(1.0f, false)` | 0.05 × 1.0 | no | `DirectPathNavigator` | < 200 |
| Toucan | `FlightMoveController(0.6f, false, true)` | 0.05 × 0.6 | **yes** | `DirectPathNavigator` | < 200 |
| Potoo | `FlightMoveController(0.6f, false, true)` | 0.05 × 0.6 | **yes** | `AdvancedPathNavigateNoTeleport` (FLYING) | perch-driven |
| Seagull | private `MoveHelper` | 0.03 | no | `DirectPathNavigator` | < 400 |
| Bald Eagle | private `MoveHelper` | 0.05 | no | `DirectPathNavigator` | < 500 |
| Hummingbird | `FlightMoveController(1.5f)` | 0.05 × 1.5 | no | vanilla `FlyingPathNavigation` | always airborne |

The Potoo is the only bird that does real A* in the air. Everything else navigates by
steering only.

---

## 2. `FlightMoveController` - steering, not pathing

`entity/ai/FlightMoveController.java` is ~70 lines and is the whole flight physics model:

```java
public void tick() {
    if (this.operation == MoveControl.Operation.MOVE_TO) {
        Vec3 vector3d = new Vec3(wantedX - parentEntity.getX(), ...);
        double d0 = vector3d.length();
        if (d0 < parentEntity.getBoundingBox().getSize()) {
            this.operation = MoveControl.Operation.WAIT;
            parentEntity.setDeltaMovement(parentEntity.getDeltaMovement().scale(0.5D));
        } else {
            parentEntity.setDeltaMovement(parentEntity.getDeltaMovement()
                .add(vector3d.scale(this.speedModifier * speedGeneral * 0.05D / d0)));
            ...
        }
    }
}
```

Three design decisions in there carry most of the believability:

**It accumulates into velocity instead of setting it.** Each tick adds a small fixed-magnitude
nudge (`0.05 × speedGeneral`) along the unit vector toward the goal. Existing momentum is
never cleared. That means a bird that wants to reverse direction has to bleed off its old
velocity first, which produces wide arcing turns and overshoot for free - no explicit turning
radius, no spline, no easing curve. Birds cannot pivot in place, and that alone reads as
"flying" rather than "hovering drone".

**Arrival is a soft brake, not a stop.** When within one bounding-box size of the goal the
operation flips to `WAIT` and velocity is halved *once*. The bird coasts through the
waypoint, decaying. Because goals are re-issued while the drag continues, birds drift and
settle instead of snapping.

**Yaw is derived from velocity, not from the goal.** `setYRot(-atan2(dx, dz))` reads the
*current delta movement*, and `yBodyRot` is slaved to it. The bird always faces where it is
actually going, including during the overshoot. Combined with momentum this yields visible
banking into turns without any dedicated roll code. `shouldLookAtTarget` (false for every
idle-wandering bird) is the opt-out used by hunting mobs that need to face prey while
strafing.

**`needsYSupport`** (Toucan, Potoo) adds a separate vertical term scaled by the mob's own
`getSpeed()` and clamped `Mth.clamp(d1, -1, 1)`:

```java
parentEntity.setDeltaMovement(parentEntity.getDeltaMovement()
    .add(0.0D, getSpeed() * speedGeneral * Mth.clamp(d1, -1, 1) * 0.6F, 0.0D));
```

These two are canopy birds with slow horizontal speed (0.6) that still need authority to
climb out of a tree. Without it, a slow bird trying to gain 15 blocks of altitude spends
most of its nudge budget on the Y component and crawls forward.

The Seagull and Bald Eagle roll their own copies (`EntitySeagull.MoveHelper`,
`EntityBaldEagle.MoveHelper`) with a tighter arrival radius (`d5 < 0.3` instead of bounding
box size) and, for the gull, a lower accel of `0.03`. A gull is deliberately the floatiest
bird in the set.

### `DirectPathNavigator` - the deliberate no-op

```java
// entity/ai/DirectPathNavigator.java
public void tick() { ++this.tick; }

public boolean moveTo(double x, double y, double z, double speedIn) {
    mob.getMoveControl().setWantedPosition(x, y, z, speedIn);
    return true;
}
```

It extends `GroundPathNavigation` but discards pathfinding entirely: every `moveTo` becomes
a steering goal, and `isDone()` is never satisfied through a path. This exists so that goals
written against the vanilla `PathNavigation` API (`FollowParentGoal`, `TemptGoal`,
`BreedGoal`, `PanicGoal`) keep working while the bird is airborne - they just silently
become steering requests.

The cost is that **airborne birds do not avoid obstacles by pathing**. Obstacle avoidance is
pushed entirely into destination selection, which is the next section, and into a couple of
reactive escapes (`if (eagle.horizontalCollision && !eagle.onGround()) stop();` in the
eagle's and gull's wander goals - hit a wall, abandon the goal, pick a new destination next
tick).

---

## 3. Destination selection is where the intelligence lives

Every bird has a matched pair of `getBlockInViewAway(Vec3 fleePos, float radiusAdd)` and
`getBlockGrounding(Vec3 fleePos)`. They are the only place the world geometry is consulted.

```java
// entity/EntityCrow.java:520
public Vec3 getBlockInViewAway(Vec3 fleePos, float radiusAdd) {
    final float radius = 3.15F * -3 - this.getRandom().nextInt(24) - radiusAdd;
    final float angle = getAngle1();                       // body yaw ± up to 1 rad of noise
    final double extraX = radius * Mth.sin(Mth.PI + angle);
    final double extraZ = radius * Mth.cos(angle);
    final BlockPos radialPos = new BlockPos((int)(fleePos.x() + extraX), 0, (int)(fleePos.z() + extraZ));
    final BlockPos ground = getCrowGround(radialPos);      // vertical column scan
    final int distFromGround = (int) this.getY() - ground.getY();

    final BlockPos newPos;
    if (distFromGround > 8) {
        newPos = ground.above(4 + this.getRandom().nextInt(10));
    } else {
        newPos = ground.above(this.getRandom().nextInt(6) + 1);
    }
    if (!this.isTargetBlocked(Vec3.atCenterOf(newPos)) && this.distanceToSqr(Vec3.atCenterOf(newPos)) > 1) {
        return Vec3.atCenterOf(newPos);
    }
    return null;
}
```

Four properties:

1. **The candidate is offset from the bird's current facing, not from a random compass
   direction.** `getAngle1()` seeds from `yBodyRot` and adds `PI` plus up to a radian of
   noise, so the destination lands in a cone *behind or beside* the current heading with
   bounded jitter. Consecutive waypoints are correlated, which is what makes a wandering bird
   look like it has intent instead of jittering.

2. **Altitude is relative to the ground below the candidate, not absolute.** The column scan
   (`getCrowGround`) walks down from the bird's own Y to the first solid or fluid block, then
   the target is placed N blocks *above that*. Birds therefore follow terrain - they climb
   over a hill rather than flying into it, and they descend into a valley. This is the single
   biggest reason flight looks natural in hilly terrain despite there being no pathfinder.

3. **The N is bimodal.** If the bird is already high (`distFromGround > 8`) it picks a
   cruising altitude (crow: 4-13); if low, it hugs (1-6). Seagull uses 8-11 vs 8-11 (always
   high), Toucan 8-11 vs 18-23 (canopy), Eagle 7-16 vs 4-10. The species' flight envelope is
   expressed purely as two integer ranges.

4. **Every candidate is line-of-sight validated** via `isTargetBlocked` (a `ClipContext`
   raycast from the bird's eyes). Returning `null` on failure causes `canUse()` to return
   false, and the goal just doesn't start this tick. This is the substitute for obstacle
   avoidance: don't avoid walls, only ever aim at points you can already see.

`getBlockGrounding` is the mirrored version used when the bird's flight budget is exhausted -
it scans down from the bird's *own* column and returns a point at ground level, which is what
triggers the descent-and-land sequence.

Species-specific tweaks in the same functions:

- **Seagull** (`getSeagullGround`) scans *up* through fluid first, so a gull's "ground" over
  the ocean is the water surface. Gulls therefore cruise at a fixed height above the waves
  instead of trying to descend into the sea.
- **Toucan / Blue Jay / Potoo** special-case leaves: `if (level().getBlockState(ground).is(BlockTags.LEAVES)) newPos = ground.above(1 + random.nextInt(3));`. Over canopy they fly *just above the treetops* rather than at the height they would pick over open ground.
- **Toucan** additionally pushes off vines it is standing on while flying, with a yaw-aligned
  kick: `setDeltaMovement(add(-sin(f)*0.2F, 0.4F, cos(f)*0.2F))` (`EntityToucan.java:293`).

---

## 4. The takeoff / landing economy

Believability here is mostly about *not* flying constantly. Each idle goal (`AIWalkIdle`,
`AIWanderIdle`, `AIFlyIdle` - the same goal rewritten per mob) gates on three stacked
randoms:

```java
// entity/EntityCrow.java:646 - AIWalkIdle.canUse()
if (this.crow.getRandom().nextInt(30) != 0 && !crow.isFlying()) return false;   // takeoff gate
if (this.crow.onGround()) {
    this.flightTarget = random.nextBoolean();                                    // 50% fly vs walk
} else {
    this.flightTarget = random.nextInt(5) > 0 && crow.timeFlying < 200;          // 80% stay up, hard cap
}
```

Read the three lines as: *how often do I consider moving at all* / *if grounded, do I hop or
fly* / *if airborne, do I continue or start landing*.

| Mob | Consider-move gate | Takeoff chance when grounded | Stay-airborne chance | Hard cap |
|---|---|---|---|---|
| Crow | 1/30 | 50% | 80% | 200 |
| Blue Jay | 1/45 | 50% | 80% | 200 |
| Toucan | 1/45 | ~17% (1/6) | 80% | 200 |
| Seagull | 1/20 | 10% | 80% | 400 |
| Bald Eagle | 1/15 | 50% | ~86% (6/7) | 700 (wander re-aims at 500) |

The asymmetry is doing real work. The toucan rarely takes off but stays up once it does
(a canopy bird making long inter-tree hops). The seagull almost never takes off from the
ground but has double the airborne budget of a corvid (a soarer). The eagle has the loosest
gates and the longest budget.

Landing is not a single event but a chain of overlapping conditions, all of which appear
in every bird:

```java
if (!flightTarget && isFlying() && crow.onGround())              crow.setFlying(false);
if (isFlying() && crow.onGround() && crow.timeFlying > 10)       crow.setFlying(false);
```

and in `tick()` on the eagle/gull, a stricter version that also treats "there is a block
under me" as landed:

```java
if (isFlying() && (!level().isEmptyBlock(eagle.getBlockPosBelowThatAffectsMyMovement())
        || eagle.onGround()) && !eagle.isInWaterOrBubble() && eagle.timeFlying > 30) {
    eagle.setFlying(false);
}
```

The `timeFlying > 10` / `> 30` guard is important: it prevents a bird that just took off from
instantly re-landing because it hasn't cleared the block it launched from. That grace period
is the difference between "takes off" and "twitches".

There are also hard overrides that force flight regardless of the dice - `isOverWater()` /
`isOverWaterOrVoid()` sets `flightTarget = true` unconditionally in every bird's
`getPosition()`. A bird will never choose to land on water or into the void.

---

## 5. Species-specific flight behaviours

### Soaring circles (Bald Eagle, Seagull)

Both keep an `orbitPos`/`orbitDist`/`orbitClockwise` triple. Once airborne, there is a 1/6
chance per goal evaluation to latch an orbit:

```java
// entity/EntityBaldEagle.java:968
if (orbitResetCooldown == 0 && random.nextInt(6) == 0) {
    orbitResetCooldown = 400;
    eagle.orbitPos = eagle.blockPosition();
    eagle.orbitDist = 4 + random.nextInt(5);
    eagle.orbitClockwise = random.nextBoolean();
    maxOrbitTime = (int)(360 + 360 * random.nextFloat());
}
```

While latched, `getPosition()` returns a point on the circle instead of a wander target:

```java
private Vec3 getOrbitVec(Vec3 vector3d, float gatheringCircleDist) {
    final float angle = (Maths.STARTING_ANGLE * this.orbitDist * (orbitClockwise ? -tickCount : tickCount));
    ...
}
```

The angle advances with `tickCount` scaled by `orbitDist` (degrees per tick), so a wider
orbit sweeps faster in angle and the bird's linear speed stays roughly constant. The
destination is a *moving point ahead of the bird on the circle*, which combined with the
momentum-based steering produces a genuine banked circle rather than a polygon.

When the orbit expires, `orbitResetCooldown` is set **negative** (`-400 - random.nextInt(400)`)
and only counts back up toward zero. A negative cooldown is a lockout: the bird cannot start
another orbit for 400-800 ticks. This is what stops eagles from soaring in circles
permanently.

### Circling before committing (Crow raiding crops)

`entity/ai/CrowAICircleCrops.java` is a two-phase `MoveToBlockGoal`. Phase one is a 200-tick
circle above the target crop at 8°/tick (`Maths.EIGHT_STARTING_ANGLE`, so a 45-tick orbit),
radius 1-3, height 1-3 above:

```java
if (circlePhase) {
    this.tryTicks = 0;
    BlockPos circlePos = getVultureCirclePos(blockpos);
    if (circlePos != null) {
        crow.setFlying(true);
        crow.getMoveControl().setWantedPosition(circlePos.getX() + 0.5D, ..., 0.7F);
    }
    circlingTime++;
    if (circlingTime > 200) { circlingTime = 0; circlePhase = false; }
}
```

Only after the circle does it drop to the normal ground approach, land, peck five times, and
destroy the crop. The circle serves no mechanical purpose - it is pure telegraphing, and it
is the reason crows raiding a farm read as deliberate rather than as a mob teleporting onto
your wheat.

Tamed crows do the same thing around their `perchPos` via `getGatheringVec` when set to the
"gather" command.

### Perching (Potoo)

The Potoo is the outlier: it uses a real flying A* navigator
(`AdvancedPathNavigateNoTeleport` in `FLYING` mode) with a custom `isStableDestination` that
requires a non-air block two below, and `setCanFloat(false)`.

Its `AIPerch` goal has three stages: fly to a validated perch block (`POTOO_PERCHES` tag,
approached from a specific `Direction`), snap on contact, then *stay* snapped:

```java
// entity/EntityPotoo.java:701
if (EntityPotoo.this.getBlockPosBelowThatAffectsMyMovement().equals(perch)) {
    EntityPotoo.this.setDeltaMovement(Vec3.ZERO);
    EntityPotoo.this.setPerching(true);
    EntityPotoo.this.setFlying(false);
    EntityPotoo.this.setPerchPos(perch);
    EntityPotoo.this.setPerchDirection(perchDirection);
    EntityPotoo.this.getNavigation().stop();
}
```

`slideTowardsPerch()` (called from `tick()` every tick while perched) is a small
proportional controller that keeps the bird glued to the correct *side* of the perch block
and rotates all three yaw fields to face outward:

```java
Vec3 onBlock = block.add(getPerchDirection().getStepX() * 0.35F, 0F, getPerchDirection().getStepZ() * 0.35F);
Vec3 diff = onBlock.subtract(this.position());
float f = (float) diff.length();
float f1 = f > 1F ? 0.25F : f * 0.1F;      // proportional below 1 block, capped above
this.setDeltaMovement(this.getDeltaMovement().add(diff.normalize().scale(f1)));
```

The `f > 1F ? 0.25F : f * 0.1F` split is a nice touch - constant approach speed far away,
proportional (and therefore non-overshooting) close in. Path recalculation is also throttled
to once every 30-60 ticks (`pathRecalcTime`) rather than every tick, which both saves CPU and
stops the approach from jittering.

Perching is abandoned if the block stops being valid or the bird drifts more than 1.5 blocks
away, and `perchCooldown` (120-1320 ticks) prevents immediate re-perching on `stop()`.

### Hovering (Hummingbird)

The Hummingbird never lands - `setFlying(true)` and `setNoGravity(true)` are unconditional in
`tick()`. It is the only bird using vanilla `FlyingPathNavigation`, with an
`isStableDestination` override requiring solid ground two blocks below (so it hovers *over*
things rather than in open sky).

Two details make hovering read correctly:

```java
// entity/EntityHummingbird.java:250
if (!this.onGround() && vector3d.y < 0.0D) {
    this.setDeltaMovement(vector3d.multiply(1.0D, 0.4D, 1.0D));
}
...
if (this.getDeltaMovement().lengthSqr() < 1.0E-7D) hummingStill++; else hummingStill = 0;
```

Downward velocity is damped to 40% every tick, so the bird sinks only very slowly and appears
to hold station. And `hummingStill` counts ticks of near-zero movement - `HummingbirdAIWander`
requires `hummingStill > 10` before it will even consider a new destination, which enforces a
visible hover pause between darts. Combined with `FLYING_SPEED 7.0` and a 1.5 accel factor,
the result is the stop-start motion the real animal has.

`HummingbirdAIWander.getRandomLocation()` also anchors to the feeder block if one is known
rather than to the bird's own position, so a hummingbird with a feeder patrols *around the
feeder* instead of drifting away.

---

## 6. The presentation layer

The AI would not read as birdlike without the client-side interpolation that sits on top.
Every bird keeps `prevX`/`X` pairs of animation scalars, ticked server- and client-side, and
the models lerp them by `partialTick`.

**Pitch from vertical velocity** - no separate pitch controller exists:

```java
// entity/EntityBlueJay.java:200
float yMov = (float) this.getDeltaMovement().y;
this.birdPitch = yMov * 2 * -(float) Mth.RAD_TO_DEG;
```

```java
// client/model/ModelBaldEagle.java:228
float birdPitch = entity.prevBirdPitch + (entity.birdPitch - entity.prevBirdPitch) * partialTicks;
this.body.rotateAngleX += birdPitch * flyProgress * 0.2F * Mth.DEG_TO_RAD;
```

The body tilts because the bird is climbing or diving, which is exactly the causal direction
in a real bird. Scaling by `flyProgress` means the tilt fades out as the bird transitions to
its grounded pose.

**Flap rate from vertical velocity** - the crucial one. Birds flap when gaining altitude and
hold their wings out when sinking:

```java
// entity/EntityBlueJay.java:202
if (yMov >= 0)            { if (flapAmount < 1F) flapAmount += 0.25F; }
else if (yMov < -0.07F)   { if (flapAmount > 0)  flapAmount -= 0.25F; }
```

The Seagull adds a turn term, so a banking gull also beats its wings:

```java
// entity/EntitySeagull.java:291
if (absYaw > 8)        flapAmount = Math.min(1F, flapAmount + 0.1F);
else if (yMot < 0.0F)  flapAmount = Math.min(-yMot * 0.2F, 1F);
else                   flapAmount -= Math.min(flapAmount, 0.05F);
```

The Eagle goes further with a dedicated `swoopProgress` that folds the wings back during a
dive, and the model uses it to *suppress* the flap:

```java
// client/model/ModelBaldEagle.java:167
float flapAmount = (lerped flap) * flyProgress * 0.2F * (5F - swoopProgress) * 0.2F;
```

so a swooping eagle's wings lock into a swept pose (`progressRotationPrev(wingL, swoopProgress, rad(60), rad(50), 0, 5F)`) and stop beating entirely.

**Idle head movement in flight** - the Seagull drives a synched `flightLookYaw` on the
server with its own easing and a randomized dwell:

```java
// entity/EntitySeagull.java:314
if (flightLookCooldown == 0 && this.random.nextInt(4) == 0 && lookYawDist < 0.5F) {
    targetFlightLookYaw = Mth.clamp(random.nextFloat() * 120F - 60, -60, 60);
    flightLookCooldown = 3 + random.nextInt(15);
}
if (this.getFlightLookYaw() < this.targetFlightLookYaw && lookYawDist > 0.5F)
    this.setFlightLookYaw(this.getFlightLookYaw() + Math.min(lookYawDist, 4F));
```

Capped at 4°/tick so the head turn is smooth, applied in `ModelSeagull` as
`head.rotateAngleY += toRadians(getFlightLookYaw()) * flyProgress * 0.2F`. A gliding gull
scanning the water below is entirely this one field.

**`flyProgress` as a blend weight** - every bird ramps a 0..5 counter on the flying flag and
the models use it as the interpolation factor between the grounded and airborne poses (legs
tuck, body pitches forward, wings deploy). Because it takes 5 ticks each way, takeoff and
landing have a visible transition instead of a pop.

---

## 7. What's fragile

Worth knowing if this pattern gets borrowed:

- **No airborne obstacle avoidance.** Everything rests on the `isTargetBlocked` raycast at
  destination-selection time. A bird that picks a valid target and then drifts (momentum,
  overshoot, another goal interrupting) can and does clip into terrain. The only recovery is
  the `horizontalCollision` check in the eagle/gull wander goals; the crow, jay, toucan and
  potoo have no equivalent and rely on being small.

- **Navigator swapping allocates.** `switchNavigator` constructs a fresh `MoveControl` and
  `PathNavigation` on every transition. With the guard it's rare, but a bird oscillating on
  the takeoff/landing boundary will churn objects. The `timeFlying > 10` grace period is
  partly a mitigation for this.

- **`DirectPathNavigator.isDone()` is never true.** Any vanilla goal that terminates on
  "navigation finished" will never terminate while the bird is airborne. Each mob works
  around it with an explicit `distanceToSqr(x, y, z) > N` check in `canContinueToUse()`, and
  the thresholds are hand-tuned per species (crow/toucan 2, jay/gull/eagle 5). Getting this
  wrong produces a bird that either never releases the goal or releases it every tick.

- **Numbers are inlined, not configured.** Radii, altitude bands, flight budgets and dice
  gates are literals scattered across seven near-duplicate copies of the same idle goal. The
  species feel genuinely different, but there is no single place to see or tune the flight
  envelope, and a fix to one bird's landing logic has to be replicated six times. The
  duplication is visible in the code: the Potoo and Blue Jay both call their ground-scan
  helper `getToucanGround`.

---

## 8. Transferable takeaways

If the goal is believable flight without a flight pathfinder:

1. **Accumulate into velocity; never assign it.** Momentum alone buys arcs, overshoot and
   banking with no extra code.
2. **Derive yaw from actual velocity, not from the goal vector.** The mob then always faces
   where it is going, including while it is failing to turn fast enough.
3. **Put terrain awareness in destination selection, not in steering.** A downward column
   scan plus "N blocks above whatever I found" makes flight follow terrain for the cost of
   one loop.
4. **Bias the next waypoint off the current heading.** Correlated waypoints look like intent;
   uniform random ones look like noise.
5. **Give flight a budget.** A `timeFlying` counter that resets on landing, plus a
   probability of continuing, produces natural-length flights without scripting them.
6. **Drive the animation from the physics you already have.** Pitch from `deltaMovement.y`,
   flap rate from climb/descent and turn rate. It stays in sync for free and is causally
   correct.
7. **Add a telegraph.** The crow's 200-tick circle over a crop and the eagle's soaring orbit
   are mechanically pointless and do more for perceived intelligence than any of the
   pathfinding would have.
