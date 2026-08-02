package net.mehvahdjukaar.sleep_tight.test.controller;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Which mode the bird is in, and everything that follows from the answer. Section 6 of
 * {@code believable_bird_flight.md} asked for takeoff, cruise, flare and perch; this owns all of
 * them except cruise, which is the flight stack's business, and the flare, which is still missing.
 * <p>
 * Three jobs. It owns whether the bird's feet are <b>down</b>, which is a state and not a
 * measurement: a bird gripping a branch is grounded, one hovering an inch above it is not, and no
 * amount of looking at velocity or at {@code onGround} alone tells the two apart. It owns the
 * <b>launch turn</b>, swinging the mob on the spot to face where the path leaves from before flight
 * is allowed to start. And it owns <b>body pitch</b>, which is a target per mode rather than a value
 * anything writes directly, so a bird that lands mid dive straightens out instead of freezing nose
 * down.
 * <p>
 * The launch turn is the reason the search may plan a departure in any direction at all. A grounded
 * bird carries no airspeed, so {@code BirdNodeEvaluator} lifts the turn cap on the first step and
 * lets the path leave whichever way is cheapest. Something then has to make that true on the mob,
 * because the flight follower cannot: it turns at a flying bird's rate, and a path that leaves
 * behind the mob would be flown as a wide arc through whatever the bird was perched against. Turning
 * on the ground first is free, instant in path terms, and is what feet are for. It lives here rather
 * than in {@link BirdFlightControl} because it is a ground manoeuvre, and this is the layer that owns
 * the mob's facing whenever its feet are down.
 * <p>
 * Gravity is this class's alone. It used to be switched on in the mob's constructor and again every
 * move control tick and never cleared, which is how a bird ends up unable to land.
 */
public class BirdGroundControl {

    public enum Mode {
        /** In the air, flying or hovering. Gravity off, the flight layer is in charge. */
        AIRBORNE,
        /** Arrived over something landable, gravity back on, dropping the last bit onto it. */
        DESCENDING,
        /** Feet down, standing. */
        PERCHED,
        /** Feet down, walking a ground path. The flight layers are not installed at all. */
        WALKING,
        /** Feet down, turning on the spot to line up with the path. Flight is held off. */
        LAUNCHING
    }

    private final Mob mob;
    private Mode mode = Mode.AIRBORNE;
    private float launchYaw;

    // degrees, positive nose down, the same convention as xRot used to carry. Approached rather than
    // assigned, so every mode only has to name where it wants the body pointed
    private float bodyPitch;
    // NaN when nothing outside is asking for a pitch, which is the normal case
    private float pitchOverride = Float.NaN;

    public BirdGroundControl(Mob mob) {
        this.mob = mob;
    }

    /** Feet planted, whether standing, walking or mid-pivot. This is what the synched flag mirrors. */
    public boolean isGrounded() {
        return this.mode == Mode.PERCHED || this.mode == Mode.WALKING || this.mode == Mode.LAUNCHING;
    }

    public boolean isWalking() {
        return this.mode == Mode.WALKING;
    }

    public boolean isHoldingForLaunch() {
        return this.mode == Mode.LAUNCHING;
    }

    /** Where the body is pointed, in degrees, positive nose down. */
    public float bodyPitch() {
        return this.bodyPitch;
    }

    /**
     * Points the body somewhere other than along its flight until cleared. Only honoured in the air:
     * a landing always straightens out, so an override left set can never leave a perched bird
     * standing on its beak.
     */
    public void setPitchOverride(float degrees) {
        this.pitchOverride = degrees;
    }

    public void clearPitchOverride() {
        this.pitchOverride = Float.NaN;
    }

    /**
     * Line the mob up with a path before it flies it. Ignored while airborne, where the flight
     * follower turns towards the first carrot itself and there is nothing to stand on anyway.
     */
    public void requestLaunch(float launchYaw) {
        // NaN would make the alignment test below permanently false and strand the bird on the
        // ground with a path it is never allowed to fly
        if (!this.isGrounded() || Float.isNaN(launchYaw)) {
            return;
        }
        this.launchYaw = launchYaw;
        this.mode = Mode.LAUNCHING;
    }

    /** The path went away before the mob got off the ground. Stay put rather than launching at it. */
    public void cancelLaunch() {
        if (this.mode == Mode.LAUNCHING) {
            this.mode = Mode.PERCHED;
        }
    }

    /**
     * Start walking a ground path. The mob swaps the locomotion pair over on the strength of this,
     * so nothing about it is allowed to be conditional on where the walk is going.
     */
    public void beginWalk() {
        if (this.isGrounded()) {
            this.mode = Mode.WALKING;
        }
    }

    /** A flight has been requested, so whatever walk was happening is over. */
    public void endWalk() {
        if (this.mode == Mode.WALKING) {
            this.mode = Mode.PERCHED;
        }
    }

    /**
     * Ticked from the mob's {@code customServerAiStep}, which is the one slot that runs after the
     * navigation and before the move control. That order is what lets a launch requested this tick
     * hold off the same tick's steering.
     */
    public void tick() {
        switch (this.mode) {
            case AIRBORNE -> this.tickAirborne();
            case DESCENDING -> this.tickDescending();
            case PERCHED -> this.tickPerched();
            case WALKING -> this.tickWalking();
            case LAUNCHING -> this.tickLaunching();
        }
        this.tickBodyPitch();
    }

    private void tickAirborne() {
        this.mob.setNoGravity(true);
        if (BirdGroundConfig.perchOnArrival && this.mob.getNavigation().isDone() && this.groundWithinReach()) {
            this.mode = Mode.DESCENDING;
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
            this.mode = Mode.AIRBORNE;
        } else if (this.mob.onGround()) {
            this.mode = Mode.PERCHED;
        }
    }

    private void tickPerched() {
        this.mob.setNoGravity(false);
        if (!this.mob.onGround()) {
            // shoved, or whatever it was standing on is gone
            this.mode = Mode.AIRBORNE;
        }
    }

    /**
     * Walking is the one mode with nothing to steer: vanilla's ground navigation and move control are
     * installed while it lasts and they need no help. All this does is watch for the two ways it can
     * end.
     */
    private void tickWalking() {
        this.mob.setNoGravity(false);
        if (!this.mob.onGround()) {
            // walked off an edge, or got shoved. Back to the flight pair, which can catch it
            this.mode = Mode.AIRBORNE;
        } else if (this.mob.getNavigation().isDone()) {
            this.mode = Mode.PERCHED;
        }
    }

    private void tickLaunching() {
        this.mob.setNoGravity(false);
        if (!this.mob.onGround()) {
            this.mode = Mode.AIRBORNE;
            return;
        }
        if (this.mob.getNavigation().isDone()) {
            this.mode = Mode.PERCHED;
            return;
        }
        // a pivot is not a step: nothing should slide out from under the mob while it happens
        Vec3 velocity = this.mob.getDeltaMovement();
        this.mob.setDeltaMovement(0.0, velocity.y, 0.0);

        float turned = Mth.approachDegrees(this.mob.getYRot(), this.launchYaw, BirdGroundConfig.launchTurnPerTick);
        this.mob.setYRot(turned);
        this.mob.yBodyRot = turned;
        if (Math.abs(Mth.degreesDifference(turned, this.launchYaw)) <= BirdGroundConfig.launchYawTolerance) {
            this.mode = Mode.AIRBORNE;
        }
    }

    /**
     * One rate-limited approach to whatever this mode wants the body pointed at, which is what makes
     * a landing straighten the bird out smoothly rather than leaving it stuck at the pitch it last
     * flew. It used to be written straight onto {@code xRot} by the move control, which meant it was
     * only ever updated on ticks with a path to fly, and shared a field with where the mob is looking.
     */
    private void tickBodyPitch() {
        this.bodyPitch = Mth.approachDegrees(this.bodyPitch, this.wantedPitch(),
                BirdFlightConfig.maxPitchPerTick);
    }

    /**
     * Level for anything with its feet down or about to have them, and the flown slope otherwise.
     * <p>
     * Pitch tracks where the mob is actually going rather than where it is aimed, so a bird still
     * drifting out of the last leg does not point somewhere it is not moving. Descending is level on
     * purpose and is where the flare goes when it lands: pointing the body along a gravity-driven
     * drop reads as falling, which is precisely what a landing should not look like.
     */
    private float wantedPitch() {
        if (this.mode != Mode.AIRBORNE) {
            return 0.0F;
        }
        if (!Float.isNaN(this.pitchOverride)) {
            return Mth.clamp(this.pitchOverride, -BirdFlightConfig.maxPitch, BirdFlightConfig.maxPitch);
        }
        Vec3 velocity = this.mob.getDeltaMovement();
        float slope = (float) -(Mth.atan2(velocity.y, velocity.horizontalDistance()) * Mth.RAD_TO_DEG);
        return Mth.clamp(slope, -BirdFlightConfig.maxPitch, BirdFlightConfig.maxPitch);
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

    /** Names the mode in the debug overlay. */
    public String getModeName() {
        return this.mode.name();
    }

    /**
     * The heading being turned to, or {@code NaN} when no launch is in progress. The debug renderer
     * draws it against the body's own facing, which is the only way to see a launch turn happen
     * rather than infer it from the mob having sat still for a moment.
     */
    public float getLaunchYaw() {
        return this.mode == Mode.LAUNCHING ? this.launchYaw : Float.NaN;
    }
}
