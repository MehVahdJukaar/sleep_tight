package net.mehvahdjukaar.sleep_tight.test.controller;

import net.mehvahdjukaar.sleep_tight.test.navigator.BirdFlightNavigation;
import net.mehvahdjukaar.sleep_tight.test.throttle.FlightEnvelope;
import net.mehvahdjukaar.sleep_tight.test.throttle.ThrottleProfile;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.phys.Vec3;

/**
 * The bird's one and only steering, replacing vanilla's
 * {@link net.minecraft.world.entity.ai.control.FlyingMoveControl}, which snaps yaw up to 90 degrees
 * per tick and applies full thrust until it is within half a millimetre of the waypoint.
 * <p>
 * Pure pursuit: it aims at a point some way further along the path and flies at it. Chasing a point
 * ahead rounds off the corner between here and there, so the lattice's 45 degree blockiness comes out
 * as a curve rather than a sequence of pivots. The usual objection to pure pursuit is that it has no
 * idea how fast it should be going, so it arrives at corners too quickly and swings wide; that does
 * not apply here, because speed is not this layer's decision at all. The {@link ThrottleProfile} has
 * already worked out what every point of the path may be taken at and this only has to stay under it.
 * <p>
 * Three jobs, and only three. Point the body at the carrot at a limited rate; bring the momentum
 * round with it in both planes, since {@code travel()} only ever pushes along yaw and would
 * otherwise leave the velocity decaying along the old heading and the old climb angle; and hold
 * whatever speed it was told it may hold.
 * <p>
 * It holds no history at all: where it is aimed and how fast it may go are handed down fresh every
 * tick by {@link BirdFlightNavigation}, so a mob that gets shoved recovers from wherever it lands
 * instead of being confused by it. {@link #wingThrust} is this tick's answer kept where the entity
 * can read it, not state. The one policy it owns is {@link #lookahead}, how much of the drawn line
 * it is willing to round off, because that is a steering decision and nothing else.
 * <p>
 * This shape was picked by flying three controls over the same routes in the {@code PathfindingTest}
 * lab: against the fixed-carrot version it used to be, it holds the corridor to 0.05 blocks rather
 * than 0.27 and halves the excursion above the profile. What the game forces on top of the lab's
 * version is the yaw-only steering and the split throttle below, since {@code travel()} pushes along
 * yaw and {@code yya} and never along pitch. That translation was measured as free.
 * <p>
 * Sections 1, 2 and 6 of {@code believable_bird_flight.md} are all in now: velocity steering here,
 * arc-length pure pursuit in {@link net.mehvahdjukaar.sleep_tight.test.navigator.PathRuler}, and an
 * absorbing arrival in {@code BirdFlightNavigation.followThePath}. Section 6's mode machine is half
 * built: takeoff, perch, walking and the flare all live in {@link BirdStateMachine}, which also
 * owns {@code noGravity} and the body's pitch now, so this class no longer touches any of it.
 * Banking from section 5 is still missing.
 * <p>
 * What got deleted, because the profile subsumes all of it: the per-tick walk over the remaining
 * path nodes, the stopping-distance brake computed from it, and the separate slow-down-in-turns
 * factor. If the bird still cuts a corner now, that is the planner's numbers being wrong rather than
 * a second slowdown fighting the first, which is the whole point of testing it this way.
 */
public class BirdFlightControl extends MoveControl {

    // the carrot always sits some way ahead, so this only ever catches a degenerate direction, never
    // arrival. Not vanilla's MIN_SPEED_SQR, which is an acceptance sphere and has no meaning here
    private static final double MIN_DIRECTION_LENGTH = 1.0E-4;

    // below this much horizontal spread there is no heading in the carrot to steer to, only the
    // numerical residue of one, and yawTowards would hand back an arbitrary angle. A bird that has
    // ended up directly over its target should hold the heading it has and go down, not pick a
    // direction out of the rounding error and fly a circle looking for one
    private static final double MIN_HORIZONTAL_AIM_SQR = 1.0E-2;

    // what the wings put out on the last tick, in blocks per tick squared. Kept only so the entity
    // can read it back out: it is what the model flaps off, and a flying bird and a fluttering one
    // have to hand over the same quantity for that to work
    private double wingThrust;

    public BirdFlightControl(Mob mob) {
        super(mob);
    }

    /** {@code operation}'s type is protected on the vanilla class, so name-only is what escapes. */
    public String getOperationName() {
        return this.operation.name();
    }

    /**
     * How far ahead of the mob, in blocks along the path, this control wants its carrot. The
     * navigation asks every tick and resolves the point itself, because turning a distance into a
     * position is path geometry; what is decided here is the policy.
     * <p>
     * Two terms. The aim is interpolated on how walled in the path is here, so open air gets a long,
     * smooth carrot and a tight cell gets a short, accurate one: rounding the drawn line off means
     * leaving cells the search certified as clear, and enclosure is the measurement of how much room
     * there is to leave them into. On top of that, being off the line pulls the carrot in, so a mob
     * that has been shoved corrects rather than rejoining fifty blocks later.
     *
     * @param enclosure 0 in open air through 1 fully boxed in, at the mob's current point
     * @param offRoute  how far the mob is from the line, in blocks
     */
    public double lookahead(double enclosure, double offRoute) {
        double room = Mth.lerp(Mth.clamp(enclosure, 0.0, 1.0),
                BirdFlightConfig.openAirLookahead, BirdFlightConfig.enclosedLookahead);
        double pulledIn = room - offRoute * BirdFlightConfig.offRouteRecoveryGain;
        return Mth.clamp(pulledIn, BirdFlightConfig.enclosedLookahead, BirdFlightConfig.openAirLookahead);
    }

    /**
     * The yaw a mob has to hold to travel along a horizontal direction, in MC's convention where 0
     * faces +Z. Shared with {@link BirdStateMachine}'s launch turn through the navigation, so the
     * heading the bird turns to on the ground and the one it steers to in the air are the same
     * number by construction.
     */
    public static float yawTowards(double dx, double dz) {
        return (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
    }

    @Override
    public void tick() {
        // the navigation is the authority on whether we are still going somewhere: nothing ever
        // clears MoveControl.operation back to WAIT for us. Same approach as SmoothSwimmingMoveControl
        if (!this.hasWanted() || this.mob.getNavigation().isDone() || this.isHeldOnGround()) {
            this.coast();
            return;
        }

        Vec3 toCarrot = new Vec3(this.wantedX - this.mob.getX(),
                this.wantedY - this.mob.getY(), this.wantedZ - this.mob.getZ());
        if (toCarrot.length() < MIN_DIRECTION_LENGTH) {
            this.coast();
            return;
        }

        // one envelope for the whole tick, so the rate the body turns at and the speed it is held to
        // cannot come from two different snapshots of the config
        FlightEnvelope envelope = this.envelope();
        float yawBefore = this.mob.getYRot();
        // a carrot straight overhead or straight underfoot has no heading in it, so keep the one we
        // have rather than steering to whatever the residue says
        if (toCarrot.horizontalDistanceSqr() > MIN_HORIZONTAL_AIM_SQR) {
            this.steerYaw(toCarrot.x, toCarrot.z, envelope);
        }
        this.turnVelocityWithBody(Mth.degreesDifference(yawBefore, this.mob.getYRot()));
        this.turnVelocityPitch(toCarrot, envelope);
        this.applyThrust(toCarrot, envelope);
    }

    /**
     * The ground layer is turning the mob on the spot to line up with the path it was just handed.
     * Steering has to stay out of the way until it is done: thrusting mid-pivot is the bird sliding
     * off its perch sideways, which is the thing turning on the ground was there to avoid.
     */
    private boolean isHeldOnGround() {
        return this.mob.getNavigation() instanceof BirdFlightNavigation navigation
                && navigation.isHeldOnGround();
    }

    /** What the wings are putting out this tick, in blocks per tick squared. */
    public double wingThrust() {
        return this.wingThrust;
    }

    /** Cut thrust and let drag do the rest. Leaves {@code speed} alone, stuck detection reads it. */
    private void coast() {
        this.wingThrust = 0.0;
        this.mob.setXxa(0.0F);
        this.mob.setYya(0.0F);
        this.mob.setZza(0.0F);
    }

    /**
     * Turns towards the carrot at the envelope's rate. Off the envelope rather than the config
     * because the planner priced this path's corners against that exact number, and a follower
     * turning at some other rate is the planner drawing corners it cannot fly.
     */
    private void steerYaw(double dx, double dz, FlightEnvelope envelope) {
        float wantedYaw = yawTowards(dx, dz);
        float maxTurn = (float) (envelope.maxYawRate() * Mth.RAD_TO_DEG);
        this.mob.setYRot(this.rotlerp(this.mob.getYRot(), wantedYaw, maxTurn));
        // a bird's body points where it flies. Left alone, BodyRotationControl lags the turn by
        // several ticks and the model reads as sliding sideways through the arc
        this.mob.yBodyRot = this.mob.getYRot();
    }

    /**
     * Brings the horizontal velocity round with the body by the yaw just applied. Thrust alone
     * cannot turn a flier: {@code travel()} pushes along the new yaw but leaves the old velocity to
     * decay at 0.91 a tick, an 11 tick time constant, so the velocity ends up far behind the body
     * and the mob crabs through the arc. Only the direction is changed, never the magnitude, so
     * thrust and drag still own the speed and the turn radius comes out as {@code speed / yawRate}.
     */
    private void turnVelocityWithBody(float degrees) {
        float radians = degrees * BirdFlightConfig.velocitySteerFraction * Mth.DEG_TO_RAD;
        float cos = Mth.cos(radians);
        float sin = Mth.sin(radians);
        Vec3 velocity = this.mob.getDeltaMovement();
        this.mob.setDeltaMovement(
                velocity.x * cos - velocity.z * sin, velocity.y, velocity.x * sin + velocity.z * cos);
    }

    /**
     * The vertical half of {@link #turnVelocityWithBody}, and the reason a fast bird flew straight
     * through every dive.
     * <p>
     * Nothing in {@code travel()} ever rotates vertical velocity: it is only ever changed by thrust
     * against drag, which bends the flight path at {@code accel/speed} and so gives a vertical turn
     * radius of {@code speed^2/accel}. At terminal speed {@code accel} is {@code speed*(1-drag)/drag},
     * so that radius is about ten times the speed in blocks however the throttle is set - 0.6 blocks
     * at the old crawl, 2 at full thrust. Pure pursuit cannot ask for an arc wider than half its
     * lookahead, so past about 0.075 blocks a tick the bird physically stops being able to follow a
     * pitch change, holds level, flies off the end of the path and only then finds its way down.
     * <p>
     * Steering the velocity's pitch the way the yaw half steers its heading puts the vertical radius
     * back at {@code speed/yawRate}, the same as the horizontal one, so a turn costs the same room
     * whichever plane it happens in. The magnitude is untouched, so thrust and drag still own speed.
     */
    private void turnVelocityPitch(Vec3 toCarrot, FlightEnvelope envelope) {
        Vec3 velocity = this.mob.getDeltaMovement();
        double speed = velocity.length();
        double heading = velocity.horizontalDistance();
        if (speed < MIN_DIRECTION_LENGTH) {
            return;
        }
        double current = Mth.atan2(velocity.y, heading);
        double wanted = Mth.atan2(toCarrot.y, toCarrot.horizontalDistance());
        double maxStep = envelope.maxYawRate() * BirdFlightConfig.velocitySteerFraction;
        double pitch = current + Mth.clamp(wanted - current, -maxStep, maxStep);
        double vertical = Math.sin(pitch) * speed;
        if (heading < MIN_DIRECTION_LENGTH) {
            // going straight up or down, so there is no horizontal direction to preserve. Leave the
            // horizontal alone and let thrust rebuild it rather than inventing a heading here
            this.mob.setDeltaMovement(velocity.x, vertical, velocity.z);
            return;
        }
        double scale = Math.cos(pitch) * speed / heading;
        this.mob.setDeltaMovement(velocity.x * scale, vertical, velocity.z * scale);
    }

    /**
     * One throttle, split between forward and climb by the slope of the line to the carrot, so the
     * mob flies at the angle the path was drawn at.
     * <p>
     * The two axes share a throttle rather than running their own controllers because
     * {@code moveRelative} normalises {@code (xxa, yya, zza)} when it is longer than 1 and scales it
     * by a flat constant. Feeding it the unit direction to the carrot puts the thrust exactly along
     * that line, and with equal drag on both axes the velocity settles along it too.
     */
    private void applyThrust(Vec3 toCarrot, FlightEnvelope envelope) {
        double throttle = this.throttleFor(envelope);
        Vec3 direction = toCarrot.normalize();
        this.mob.setSpeed((float) (throttle * direction.horizontalDistance()));
        this.mob.setYya((float) (throttle * direction.y));
    }

    /**
     * A servo rather than a switch: {@link FlightEnvelope#thrustToReach} asks for exactly the thrust
     * that lands on the commanded speed after this tick's drag, and by construction can never leave
     * the mob above it. That is the whole controller, with no gain to tune - the old feed-forward
     * plus proportional pair was the same thing with the gain guessed at, and it sat above the
     * profile through most corners because a nudge takes the drag time constant to bite.
     * <p>
     * Being over the limit needs no special case: the thrust needed goes negative, clamps to zero,
     * and coasting is the hardest this mob can brake.
     * <p>
     * What the servo is allowed to ask for is speed dependent, which is where takeoff gets its
     * punch - see {@link FlightEnvelope#thrustCapAt}. The answer is kept on the way past because it
     * is also what the wings are doing.
     */
    private double throttleFor(FlightEnvelope envelope) {
        double speed = this.mob.getDeltaMovement().length();
        this.wingThrust = envelope.thrustToReach(this.targetSpeed(envelope), speed);
        return Mth.clamp(envelope.throttleForThrust(this.wingThrust), 0.0, envelope.maxThrottle());
    }

    /**
     * Whatever the navigation says is allowed here, under the mob's own ceiling. The navigation owns
     * this rather than the control because it is the only layer that can see the profile, the cursor
     * and how far off the line the mob has ended up, and all three go into the answer.
     * <p>
     * With no path in flight this falls back to plain cruising, which means no arrival braking. That
     * is fine for the test rig and is exactly what should be visible as a difference.
     */
    private double targetSpeed(FlightEnvelope envelope) {
        double ceiling = envelope.maxSpeed() * this.speedModifier;
        return this.mob.getNavigation() instanceof BirdFlightNavigation navigation
                ? Math.min(ceiling, navigation.getSpeedLimit()) : ceiling;
    }

    /** The one the current path was planned against, so plan and flight cannot drift apart. */
    private FlightEnvelope envelope() {
        if (this.mob.getNavigation() instanceof BirdFlightNavigation navigation) {
            FlightEnvelope planned = navigation.getFlightEnvelope();
            if (planned != null) {
                return planned;
            }
        }
        return FlightEnvelope.forMob(this.mob);
    }
}
