# Fixed: walk mode couldn't cross a 1-block gap

Symptom was: the test bird, walking a ground path, could not clear a 1-block gap that a vanilla
`PathfinderMob` (e.g. a polar bear) crosses without trouble. Never a jump-strength or hitbox problem -
`BirdTestMob` sets no `JUMP_STRENGTH`/step-height overrides, and ground locomotion is unmodified
vanilla `GroundPathNavigation` + vanilla `MoveControl`.

## Root cause

`tickWalking()` read a single false `onGround()` as "walked off an edge" and flipped the mode to
`AIRBORNE` the same tick. Striding over a 1-block hole makes `onGround()` false for the tick or two
the feet are over it - completely normal, a polar bear does it every time - but the flip was not
free. `installLocomotionForMode()` reacted by calling `navigation.stop()` on the ground navigation
and swapping the pair over to the flight one mid-stride, and `tickAirborne()` then set
`noGravity(true)`. The bird was pulled out of ground physics entirely and left hovering over the hole
with no flight path to follow, so it never landed on the far side and the walk was abandoned.

Three modes did this: `PERCHED`, `WALKING` and `LAUNCHING` all watched `onGround()` and all escalated
straight to full flight.

## The fix: `DESCENDING` became `FLUTTERING`

The bug was never really about debounce tolerance. It was that there was no state for *feet off the
ground but not flying*, so losing ground contact for any reason had nowhere to go except the flight
stack.

`DESCENDING` was already most of that state - gravity on, waiting for `onGround` - so it was widened
rather than a sixth mode added. `FLUTTERING` is now every way a bird can be airborne without a path
to fly: a hop, a stride over a gap, a ledge, the block underfoot going away, and the last drop onto a
perch at the end of a flight. It puts out real upward thrust against gravity, so it parachutes rather
than plummets; it pitches along its travel scaled by horizontal speed, so a hop arcs but a straight
drop stays level; and critically it **declines to have an opinion about which locomotion pair is
installed**.

That abstention is the actual fix. `installLocomotionForMode()` reads a three-valued
`Locomotion.WALK/FLY/KEEP` rather than a boolean, so a walk excursion over a gap never swaps pairs,
never stops the navigation, and vanilla's ground pair carries the bird across on its own momentum
exactly the way it carries a polar bear across. On landing the walk is handed straight back:

```java
this.mode = this.mob.getNavigation().isDone() ? Mode.PERCHED : Mode.WALKING;
```

That single line covers all three entries, because the ground pair is the only one ever installed
with an unfinished path while fluttering - a drop out of a flight always has `isDone()` true, since
that is the condition that started it.

The swept-AABB depth probe suggested as candidate fix #2 turned out not to be needed anywhere.

Net effect on the state machine: it stayed five modes wide and *lost* transitions. `AIRBORNE` is now
reachable only deliberately - a flight path being requested, or a launch turn completing - instead of
from three separate implicit escalations.

## The flap-instead-of-jump idea landed with it

The old note here said a vanilla-style leg jump reads wrong for a bird, and that flapping over a gap
would be more believable once the state machine could cross one at all. It came for free: any loss of
ground contact enters `FLUTTERING`, which is wings-out and thrusting, so a bird crossing a gap or
hopping a ledge beats its way over rather than hopping it like a quadruped. Nothing needed
special-casing - the wing spread keys off the same synched grounded flag the mode already mirrors
out, and the thrust is the same quantity the flight control emits.
