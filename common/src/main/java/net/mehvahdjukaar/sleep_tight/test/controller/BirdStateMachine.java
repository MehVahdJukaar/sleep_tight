package net.mehvahdjukaar.sleep_tight.test.controller;

import net.mehvahdjukaar.sleep_tight.test.throttle.FlightEnvelope;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Which mode the bird is in, and everything that follows from the answer. Section 6 of
 * {@code believable_bird_flight.md} asked for takeoff, cruise, flare and perch; this owns all of
 * them except cruise, which is the flight stack's business.
 * <p>
 * It was called {@code BirdGroundControl} while it only knew about feet, which stopped being true
 * the moment the modes it holds covered being in the air as well. The name matters because it is the
 * thing that decides, per tick, which of the mob's two locomotion pairs is installed, whether
 * gravity is on, where the body is pointed and how hard the wings are working. Nothing else in the
 * package holds an opinion about any of those.
 * <p>
 * Four jobs. It owns whether the bird's feet are <b>down</b>, which is a state and not a
 * measurement: a bird gripping a branch is grounded, one hovering an inch above it is not, and no
 * amount of looking at velocity or at {@code onGround} alone tells the two apart. It owns the
 * <b>launch turn</b>, swinging the mob on the spot to face where the path leaves from before flight
 * is allowed to start. It owns <b>body pitch</b>, which is a target per mode rather than a value
 * anything writes directly, so a bird that lands mid dive straightens out instead of freezing nose
 * down. And through {@link Mode#FLUTTERING} it owns <b>everything that happens between losing the
 * ground and getting it back</b>, which is the difference between a bird and a thrown brick.
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
public class BirdStateMachine {

    public enum Mode {
        /** In the air flying a path. Gravity off, the flight layer is in charge. */
        AIRBORNE,
        /**
         * Feet off but not flying: gravity on and damped, wings working, waiting to land. Every way
         * a bird can be off the ground without a path to fly is this one state - a hop, a stride
         * over a gap, the block under it going away, and the drop onto a perch at the end of a
         * flight. See {@link #tickFluttering()} for why they are worth nothing apart.
         */
        FLUTTERING,
        /** Feet down, standing. */
        PERCHED,
        /** Feet down, walking a ground path. The flight layers are not installed at all. */
        WALKING,
        /** Feet down, turning on the spot to line up with the path. Flight is held off. */
        LAUNCHING
    }

    /** Which locomotion pair a mode wants installed. {@code KEEP} is an abstention, not a default. */
    public enum Locomotion {
        WALK, FLY, KEEP
    }

    private final Mob mob;
    private Mode mode = Mode.AIRBORNE;
    private float launchYaw;

    // degrees, positive nose down, the same convention as xRot used to carry. Approached rather than
    // assigned, so every mode only has to name where it wants the body pointed
    private float bodyPitch;
    // NaN when nothing outside is asking for a pitch, which is the normal case
    private float pitchOverride = Float.NaN;

    public BirdStateMachine(Mob mob) {
        this.mob = mob;
    }

    /** Feet planted, whether standing, walking or mid-pivot. This is what the synched flag mirrors. */
    public boolean isGrounded() {
        return this.mode == Mode.PERCHED || this.mode == Mode.WALKING || this.mode == Mode.LAUNCHING;
    }

    public boolean isWalking() {
        return this.mode == Mode.WALKING;
    }

    /** Feet off with no path to fly. The wings are working; nothing is steering. */
    public boolean isFluttering() {
        return this.mode == Mode.FLUTTERING;
    }

    /** Feet off with a path to fly, which is the one mode where the flight control owns the wings. */
    public boolean isAirborne() {
        return this.mode == Mode.AIRBORNE;
    }

    /**
     * What a fluttering bird's wings put out, in blocks per tick squared. A constant rather than a
     * servo output: there is no path being flown, so there is nothing to servo against. Public
     * because the mob reads the same number to drive the model, and two copies of it would be one
     * too many.
     */
    public static double flutterThrust() {
        return FlightEnvelope.wingPeakThrust() * BirdStateConfig.flutterWingEffort;
    }

    /**
     * Which pair of navigation and move control this mode needs. Fluttering abstains, and that
     * abstention is the whole of the one block gap fix: a stride over a hole loses ground contact
     * for a tick or two, and swapping the locomotion pair over that would stop the walk mid-step.
     * Leaving whatever is installed alone lets vanilla's ground pair carry the bird across exactly
     * the way it carries a polar bear across.
     */
    public Locomotion locomotion() {
        return switch (this.mode) {
            case WALKING -> Locomotion.WALK;
            case FLUTTERING -> Locomotion.KEEP;
            default -> Locomotion.FLY;
        };
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

    /**
     * A flight has been requested. A walk is over, and a flutter becomes the real thing: a bird
     * already in the air and handed a path has nothing left to wait for. Grounded modes are left
     * alone so {@link #requestLaunch} still gets its turn on the spot.
     */
    public void onFlightRequested() {
        if (this.mode == Mode.WALKING) {
            this.mode = Mode.PERCHED;
        } else if (this.mode == Mode.FLUTTERING) {
            this.mode = Mode.AIRBORNE;
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
            case FLUTTERING -> this.tickFluttering();
            case PERCHED -> this.tickPerched();
            case WALKING -> this.tickWalking();
            case LAUNCHING -> this.tickLaunching();
        }
        this.tickBodyPitch();
    }

    private void tickAirborne() {
        this.mob.setNoGravity(true);
        if (BirdStateConfig.perchOnArrival && this.mob.getNavigation().isDone() && this.groundWithinReach()) {
            this.enterFlutter();
        }
    }

    /**
     * Flip to fluttering and put the wings to work the same tick, rather than leaving them idle
     * until the next one comes round to the right switch arm.
     * <p>
     * That tick is not free. This layer runs from {@code serverAiStep}, which vanilla calls before
     * it applies a jump and before {@code travel}, so a mode is only ever told the ground is gone
     * one tick after it went - and a jump's height is decided in its first two or three ticks, while
     * the vertical velocity is still large. Wings that start late start after the part that mattered.
     */
    private void enterFlutter() {
        this.mode = Mode.FLUTTERING;
        this.tickFluttering();
    }

    /**
     * Feet off, wings out, gravity back on. It exists because the test for feet being down is
     * {@code onGround} and nothing can put them down while gravity is off, so something has to
     * commit to falling before the answer is knowable - and because a bird that has lost the ground
     * is not thereby flying. Everything that used to flip straight to {@link Mode#AIRBORNE} on a
     * false {@code onGround} arrives here instead, which is what stops a stride over a gap, a hop,
     * or the block underfoot going away from cancelling whatever the bird was doing.
     * <p>
     * The wings put out real upward thrust against gravity rather than gravity being quietly turned
     * down, which is what makes this the same thing the flight control is doing and not a special
     * case beside it. It buys the parachute: a bird knocked off a ledge beats its way down and lands
     * rather than dropping like a brick, and nothing ever has to decide that a fall has become a
     * flight.
     * <p>
     * Only ever on the way down, which is chicken's rule and is worth the asymmetry. Thrust applied
     * while still rising compounds across the whole climb, and a jump that would peak at a block and
     * a quarter peaks at better than two - at which point jump height is set by how the wings happen
     * to be tuned rather than by {@code JUMP_STRENGTH}, which is the one place anybody would look
     * for it. Wings arrest a fall; legs decide how high you got.
     */
    private void tickFluttering() {
        this.mob.setNoGravity(false);
        if (this.mob.onGround()) {
            // pick the walk back up where it left off. Only the ground pair is ever installed with
            // an unfinished path while this mode lasts, so this cannot read a flight path as a walk
            this.mode = this.mob.getNavigation().isDone() ? Mode.PERCHED : Mode.WALKING;
            return;
        }
        Vec3 velocity = this.mob.getDeltaMovement();
        if (velocity.y < 0.0) {
            this.mob.setDeltaMovement(velocity.add(0.0, flutterThrust(), 0.0));
        }
    }

    private void tickPerched() {
        this.mob.setNoGravity(false);
        if (!this.mob.onGround()) {
            // shoved, or whatever it was standing on is gone
            this.mode = Mode.FLUTTERING;
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
            // mid stride over a gap, off a ledge, or a jump. Not a decision: the flutter keeps this
            // same locomotion pair installed and hands the walk straight back on landing
            this.mode = Mode.FLUTTERING;
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

        float turned = Mth.approachDegrees(this.mob.getYRot(), this.launchYaw, BirdStateConfig.launchTurnPerTick);
        this.mob.setYRot(turned);
        this.mob.yBodyRot = turned;
        if (Math.abs(Mth.degreesDifference(turned, this.launchYaw)) <= BirdStateConfig.launchYawTolerance) {
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
     * Level for anything with its feet down, and the flown slope otherwise.
     * <p>
     * Pitch tracks where the mob is actually going rather than where it is aimed, so a bird still
     * drifting out of the last leg does not point somewhere it is not moving.
     * <p>
     * Fluttering takes the same slope scaled by how fast it is actually going anywhere, which is
     * what tells a hop apart from a drop. A jump has real horizontal speed, so the arc comes out
     * nose up then nose down and reads as a short flight; a bird standing still with the block
     * pulled out from under it has none, so the slope is scaled away to nothing and it stays level
     * and beats its wings instead of pointing at the floor. That is also the flare, which used to be
     * bought by keeping the whole descent rigidly level.
     */
    private float wantedPitch() {
        if (this.mode == Mode.AIRBORNE) {
            return Float.isNaN(this.pitchOverride) ? this.velocitySlope(1.0F)
                    : Mth.clamp(this.pitchOverride, -BirdFlightConfig.maxPitch, BirdFlightConfig.maxPitch);
        }
        if (this.mode == Mode.FLUTTERING) {
            double going = this.mob.getDeltaMovement().horizontalDistance();
            return this.velocitySlope((float) Math.min(1.0, going / BirdStateConfig.flutterPitchSpeedRef));
        }
        return 0.0F;
    }

    /** The angle the momentum is travelling at, weighted and clipped to what the body may hold. */
    private float velocitySlope(float weight) {
        Vec3 velocity = this.mob.getDeltaMovement();
        float slope = (float) -(Mth.atan2(velocity.y, velocity.horizontalDistance()) * Mth.RAD_TO_DEG);
        return Mth.clamp(slope * weight, -BirdFlightConfig.maxPitch, BirdFlightConfig.maxPitch);
    }

    /**
     * Whether there is anything under the mob worth descending onto, within {@code perchProbeDepth}.
     * Swept rather than sampled at that depth, so a one block ledge cannot be missed between ticks.
     * Without this check a path ending in open air would drop gravity on a bird with nothing beneath
     * it, and "arrived" would read as "fell out of the sky".
     */
    private boolean groundWithinReach() {
        AABB swept = this.mob.getBoundingBox().expandTowards(0.0, -BirdStateConfig.perchProbeDepth, 0.0);
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
