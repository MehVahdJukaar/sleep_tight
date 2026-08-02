# Known issue: walk mode can't cross a 1-block gap (unresolved)

Symptom: the test bird, walking a ground path, cannot clear a 1-block gap that a vanilla
`PathfinderMob` (e.g. a polar bear) crosses without trouble. Not a jump-strength or hitbox problem -
`BirdTestMob` sets no `JUMP_STRENGTH`/step-height overrides, and ground locomotion is unmodified
vanilla `GroundPathNavigation` + vanilla `MoveControl` (see `navigator/BirdWalkNavigation.java`,
`controller/BirdTestMob.java:67,88`). The custom `BirdNodeEvaluator` is flight-only and never wired
into the ground navigator.

## Root cause: `BirdGroundControl.tickWalking()`

`controller/BirdGroundControl.java:181-189`:

```java
private void tickWalking() {
    this.mob.setNoGravity(false);
    if (!this.mob.onGround()) {
        // walked off an edge, or got shoved. Back to the flight pair, which can catch it
        this.mode = Mode.AIRBORNE;
    } else if (this.mob.getNavigation().isDone()) {
        this.mode = Mode.PERCHED;
    }
}
```

Striding over a 1-block gap means `onGround()` reads false for the tick(s) the bird's feet are over
the hole - completely normal, a polar bear does the same thing every time it crosses one. Here that
single false reading is read as "walked off an edge" and the mode flips to `AIRBORNE` immediately,
same tick.

That flip is not free: `BirdTestMob.installLocomotionForMode()` (`BirdTestMob.java:179-187`) reacts
to the mode change by calling `this.navigation.stop()` on the ground navigation and swapping the
navigation/move-control pair over to the flight ones, mid-stride. `tickAirborne()`
(`BirdGroundControl.java:147-152`) then sets `noGravity(true)`. So instead of the bird's existing
horizontal momentum carrying it across the gap under ordinary gravity - which is all a vanilla mob
ever does - it gets pulled out of ground physics entirely and left hovering over the hole with no
flight path to follow. It never lands on the far side; the walk is simply abandoned.

Vanilla `PathfinderMob`s have no such watcher on `onGround()`. A momentary loss of ground contact
while striding over a gap is a non-event for them; `LivingEntity.travel`/`Entity.move` just keep
resolving gravity and momentum as normal and the mob's own speed carries it across.

## Candidate fixes

1. **Debounce**: tolerate a few ticks of `!onGround()` before committing to `AIRBORNE` from
   `WALKING`. Simple, but the right tolerance is an arbitrary constant that will misbehave at
   different walk speeds / gap widths.
2. **Depth probe instead of raw `onGround()`**: reuse the existing `groundWithinReach()` pattern
   (`BirdGroundControl.java:250-253`, already used by `tickDescending()`) to only bail to `AIRBORNE`
   when there is genuinely nothing below within reach, not just because this tick's collision flag
   happens to be false. Preferred - matches an existing pattern in the same class rather than adding
   a new tunable.

Not yet implemented.

## Idea, not a fix: flap instead of jump

Separate from the bug above - once ground mode can cross a gap at all, a vanilla-style leg jump
reads wrong for a bird anyway. It would be more believable for the bird to flap up and over a gap
(or a ledge) instead of hopping it like a quadruped: a short, low hop into a brief `AIRBORNE`
hand-off, wingbeats carrying it the rest of the way, rather than legs alone. Worth keeping in mind
once the state-machine fix above lands, since the fix changes exactly the transition this would
hook into.