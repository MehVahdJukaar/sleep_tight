package net.mehvahdjukaar.sleep_tight.test.controller;

import net.mehvahdjukaar.sleep_tight.test.navigator.BirdPathNavigation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The ground half of the bird's locomotion: when its feet are down, and what has to happen before
 * they leave the ground again. Section 6 of {@code believable_bird_flight.md} calls for takeoff and
 * perch gaits; this is both of them, and the place a walking navigation will eventually be driven
 * from.
 * <p>
 * Two jobs. It owns whether the bird is <b>perched</b>, which is a state and not a measurement: a
 * bird gripping a branch is perched, one hovering an inch above it is not, and no amount of looking
 * at velocity or at {@code onGround} alone tells the two apart. And it owns the <b>launch turn</b>,
 * swinging the mob on the spot to face where the path leaves from before flight is allowed to start.
 * <p>
 * The launch turn is the reason the search may plan a departure in any direction at all. A perched
 * bird carries no airspeed, so {@code BirdNodeEvaluator} lifts the turn cap on the first step and
 * lets the path leave whichever way is cheapest. Something then has to make that true on the mob,
 * because the flight follower cannot: it turns at a flying bird's rate, and a path that leaves
 * behind the mob would be flown as a wide arc through whatever the bird was perched against. Turning
 * on the ground first is free, instant in path terms, and is what feet are for. It lives here rather
 * than in {@link BirdMoveControl} because it is a ground manoeuvre, and when the walking navigation
 * arrives it is the layer that will already own the mob's facing.
 * <p>
 * Gravity is this class's alone. It used to be switched on in the mob's constructor and again every
 * move control tick and never cleared, which is how a bird ends up unable to land.
 */
public class BirdGroundControl {

    private enum Phase {
        /** In the air, flying or hovering. Gravity off, the flight layer is in charge. */
        AIRBORNE,
        /** Arrived over something landable, gravity back on, dropping the last bit onto it. */
        DESCENDING,
        /** Feet down. */
        PERCHED,
        /** Feet down, turning on the spot to line up with the path. Flight is held off. */
        LAUNCHING
    }

    private final Mob mob;
    private Phase phase = Phase.AIRBORNE;
    private float launchYaw;

    public BirdGroundControl(Mob mob) {
        this.mob = mob;
    }

    /** Feet planted, whether or not it is mid-pivot. This is what the synched flag mirrors. */
    public boolean isPerched() {
        return this.phase == Phase.PERCHED || this.phase == Phase.LAUNCHING;
    }

    public boolean isHoldingForLaunch() {
        return this.phase == Phase.LAUNCHING;
    }

    /**
     * Line the mob up with a path before it flies it. Ignored while airborne, where the flight
     * follower turns towards the first carrot itself and there is nothing to stand on anyway.
     */
    public void requestLaunch(float launchYaw) {
        // NaN would make the alignment test below permanently false and strand the bird on the
        // ground with a path it is never allowed to fly
        if (!this.isPerched() || Float.isNaN(launchYaw)) {
            return;
        }
        this.launchYaw = launchYaw;
        this.phase = Phase.LAUNCHING;
    }

    /** The path went away before the mob got off the ground. Stay put rather than launching at it. */
    public void cancelLaunch() {
        if (this.phase == Phase.LAUNCHING) {
            this.phase = Phase.PERCHED;
        }
    }

    /**
     * Ticked from the mob's {@code customServerAiStep}, which is the one slot that runs after the
     * navigation and before the move control. That order is what lets a launch requested this tick
     * hold off the same tick's steering.
     */
    public void tick() {
        switch (this.phase) {
            case AIRBORNE -> this.tickAirborne();
            case DESCENDING -> this.tickDescending();
            case PERCHED -> this.tickPerched();
            case LAUNCHING -> this.tickLaunching();
        }
    }

    private void tickAirborne() {
        this.mob.setNoGravity(true);
        if (BirdGroundConfig.perchOnArrival && this.mob.getNavigation().isDone() && this.groundWithinReach()) {
            this.phase = Phase.DESCENDING;
        }
    }

    /**
     * The only phase that is a transient rather than a state. It exists because the test for feet
     * being down is {@code onGround}, and nothing can put them down while gravity is off, so
     * something has to commit to falling before the answer is knowable.
     */
    private void tickDescending() {
        this.mob.setNoGravity(false);
        if (!this.mob.getNavigation().isDone() || !this.groundWithinReach()) {
            this.phase = Phase.AIRBORNE;
        } else if (this.mob.onGround()) {
            this.phase = Phase.PERCHED;
        }
    }

    private void tickPerched() {
        this.mob.setNoGravity(false);
        if (!this.mob.onGround()) {
            // shoved, or whatever it was standing on is gone
            this.phase = Phase.AIRBORNE;
        }
    }

    private void tickLaunching() {
        this.mob.setNoGravity(false);
        if (!this.mob.onGround()) {
            this.phase = Phase.AIRBORNE;
            return;
        }
        if (this.mob.getNavigation().isDone()) {
            this.phase = Phase.PERCHED;
            return;
        }
        // a pivot is not a step: nothing should slide out from under the mob while it happens
        Vec3 velocity = this.mob.getDeltaMovement();
        this.mob.setDeltaMovement(0.0, velocity.y, 0.0);

        float turned = Mth.approachDegrees(this.mob.getYRot(), this.launchYaw, BirdGroundConfig.launchTurnPerTick);
        this.mob.setYRot(turned);
        this.mob.yBodyRot = turned;
        if (Math.abs(Mth.degreesDifference(turned, this.launchYaw)) <= BirdGroundConfig.launchYawTolerance) {
            this.phase = Phase.AIRBORNE;
        }
    }

    /**
     * Whether there is anything under the mob worth descending onto, within {@code perchProbeDepth}.
     * Swept rather than sampled at that depth, so a one block ledge cannot be missed between ticks.
     * Without this check a path ending in open air would drop gravity on a bird with nothing beneath
     * it, and "arrived" would read as "fell out of the sky".
     */
    private boolean groundWithinReach() {
        AABB swept = this.mob.getBoundingBox().expandTowards(0.0, -BirdGroundConfig.perchProbeDepth, 0.0);
        return !this.mob.level().noCollision(this.mob, swept);
    }

    /** Names the phase in the debug overlay. */
    public String getPhaseName() {
        return this.phase.name();
    }

    /**
     * The heading being turned to, or {@code NaN} when no launch is in progress. The debug renderer
     * draws it against the body's own facing, which is the only way to see a launch turn happen
     * rather than infer it from the mob having sat still for a moment.
     */
    public float getLaunchYaw() {
        return this.phase == Phase.LAUNCHING ? this.launchYaw : Float.NaN;
    }
}
