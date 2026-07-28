package net.mehvahdjukaar.sleep_tight.test.throttle;

import net.mehvahdjukaar.sleep_tight.test.pathfinding.BirdNode;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

/**
 * Turns a path into the same path with a speed limit painted along it.
 * <p>
 * The layer between the search, which knows where the bird can go, and the follower, which has to
 * actually fly it. It exists because a turn radius is {@code speed / yawRate}: a corner too tight to
 * fly at cruise is perfectly flyable slower, so the answer to a sharp corner is not to forbid it in
 * the search but to arrive at it slowly. Working that out once per path is both cheaper and more
 * correct than the follower guessing at it every tick from the nodes still ahead of it.
 * <p>
 * Pure geometry and physics: no Level, no Mob, no tick. {@link #plan} takes plain arrays precisely so
 * it can be tested without a game running.
 * <p>
 * <b>HOW_IT_WORKS.md next to this file is the readable walkthrough</b>, with a worked example.
 * THROTTLE_NOTES.md has the reasoning behind the design. The code below follows the same four steps
 * in the same order.
 */
public final class ThrottlePlanner {

    /** Returned by a rule that has nothing to say about a node, so {@code min} ignores it. */
    private static final double NO_LIMIT = Double.MAX_VALUE;

    // a turn under this is a rounding artifact of the 45 degree heading lattice, not a corner
    private static final double MIN_TURN_ANGLE = Math.toRadians(1.0);
    // guards the corner formula blowing up as a turn approaches a full reversal
    private static final double MAX_TURN_ANGLE = Math.toRadians(175.0);
    private static final double MIN_HORIZONTAL_LEG_SQR = 1.0E-8;

    private ThrottlePlanner() {
    }

    /**
     * Convenience wrapper: pulls the two things the planner needs off a finished path. Positions
     * come from the path rather than the raw nodes so the profile is built against the same points
     * the follower will be steering at. Heading is deliberately not read, see the class doc.
     */
    public static ThrottleProfile fromPath(Path path, Entity entity, FlightEnvelope envelope) {
        int count = path.getNodeCount();
        Vec3[] points = new Vec3[count];
        float[] enclosure = new float[count];
        for (int i = 0; i < count; i++) {
            points[i] = path.getEntityPosAtNode(entity, i);
            // a path that went through trimPath can contain plain vanilla nodes, so this is optional
            enclosure[i] = path.getNode(i) instanceof BirdNode bird ? bird.enclosure : 0.0F;
        }
        return plan(points, enclosure, envelope);
    }

    /**
     * @param points    the path as world positions, one per node
     * @param enclosure per node, how walled in that cell is, 0 for open air and 1 for fully boxed in
     */
    public static ThrottleProfile plan(Vec3[] points, float[] enclosure, FlightEnvelope envelope) {
        if (points.length == 0) {
            return new ThrottleProfile(new double[]{0.0}, new double[]{0.0});
        }

        double[] arc = measureArcLengths(points);
        double[] limit = new double[points.length];

        shapeLimits(points, enclosure, envelope, limit);       // 1: what each node's own shape allows
        brakingLimits(arc, limit, envelope);                   // 2: backwards, can it slow in time
        accelerationLimits(arc, limit, envelope);              // 3: forwards, can it speed up in time

        return new ThrottleProfile(arc, limit);
    }

    // ---- step 0: measure ----

    /** Distance from the first node to each one, so everything downstream can work in arc length. */
    private static double[] measureArcLengths(Vec3[] points) {
        double[] arc = new double[points.length];
        for (int i = 1; i < points.length; i++) {
            arc[i] = arc[i - 1] + points[i].distanceTo(points[i - 1]);
        }
        return arc;
    }

    // ---- pass 1: what each node's own shape allows ----

    /**
     * The tightest speed each node's geometry permits, with no regard yet for whether it can be
     * reached: how sharp the corner is, and how steep the climb is. Passes 2 and 3 handle reachable.
     */
    private static void shapeLimits(Vec3[] points, float[] enclosure, FlightEnvelope envelope, double[] limit) {
        int last = points.length - 1;
        for (int i = 0; i <= last; i++) {
            double speed = envelope.maxSpeed();
            if (i > 0 && i < last) {
                speed = Math.min(speed, cornerLimit(points[i - 1], points[i], points[i + 1], enclosure[i], envelope));
            }
            speed = Math.min(speed, climbLimit(points, i, envelope));
            // a bird that stops dead in mid air looks broken, so a corner costs speed but never all of it
            limit[i] = Mth.clamp(speed, envelope.minSpeed(), envelope.maxSpeed());
        }
        // arrival is the one place allowed under that floor
        limit[last] = Math.min(limit[last], envelope.arrivalSpeed());
    }

    /**
     * The corner rule, and the reason this class exists.
     * <pre>
     *     speedLimit = yawRate * margin / overshootPerRadius(turnAngle)
     * </pre>
     * Read it as: turning at {@code yawRate} while moving at {@code v} traces a circle of radius
     * {@code v/yawRate}, that circle passes {@code radius * overshootPerRadius} blocks inside the
     * corner point, and {@code margin} is how much of that drift we are willing to accept. Solve for
     * {@code v} and this falls out.
     * <p>
     * The margin shrinks with how walled in the cell is, because the search only ever certified the
     * cells <i>on the line</i> as clear, and a cut corner leaves that line.
     */
    private static double cornerLimit(Vec3 before, Vec3 at, Vec3 after, float enclosure, FlightEnvelope envelope) {
        double turnAngle = horizontalTurnAngle(before, at, after);
        if (turnAngle < MIN_TURN_ANGLE) {
            return NO_LIMIT;
        }
        double margin = envelope.corridorMargin() * (1.0 - Mth.clamp(enclosure, 0.0F, 1.0F));
        return envelope.maxYawRate() * margin / overshootPerRadius(turnAngle);
    }

    /**
     * How far a turn of this angle throws the flown arc off the drawn line, per block of turn radius.
     * 0.08 at 45 degrees, 0.41 at 90, 1.61 at 135: gentle corners are nearly free and reversals are
     * ruinous, which is the same ordering the search's turn costs encode by hand.
     */
    private static double overshootPerRadius(double turnAngle) {
        return 1.0 / Math.cos(Math.min(turnAngle, MAX_TURN_ANGLE) / 2.0) - 1.0;
    }

    /** How far the heading changes between the leg arriving at {@code at} and the one leaving it. */
    private static double horizontalTurnAngle(Vec3 before, Vec3 at, Vec3 after) {
        Vec3 arriving = at.subtract(before);
        Vec3 leaving = after.subtract(at);
        if (arriving.horizontalDistanceSqr() < MIN_HORIZONTAL_LEG_SQR
                || leaving.horizontalDistanceSqr() < MIN_HORIZONTAL_LEG_SQR) {
            // one of the two legs is a pure vertical hop, which keeps the heading. There is no corner
            // to fly around here; the climb rule is what prices those
            return 0.0;
        }
        double arrivingYaw = Mth.atan2(arriving.z, arriving.x);
        double leavingYaw = Mth.atan2(leaving.z, leaving.x);
        return Math.abs(wrapRadians(leavingYaw - arrivingYaw));
    }

    /**
     * Steep legs cost speed. Not a physical limit the way the corner rule is: nothing stops the mob
     * climbing fast, but a bird with no forward airspeed is hovering, which is slow and effortful,
     * and pretending otherwise is what makes vertical flight read as an elevator. Scales from full
     * speed at {@code maxClimbAngle} down to {@code hoverSpeedFactor} on a purely vertical move, and
     * does nothing at all below {@code maxClimbAngle}.
     */
    private static double climbLimit(Vec3[] points, int index, FlightEnvelope envelope) {
        double steepest = 0.0;
        if (index > 0) {
            steepest = Math.max(steepest, legPitch(points[index - 1], points[index]));
        }
        if (index < points.length - 1) {
            steepest = Math.max(steepest, legPitch(points[index], points[index + 1]));
        }
        if (steepest <= envelope.maxClimbAngle()) {
            return NO_LIMIT;
        }
        double spanToVertical = Mth.HALF_PI - envelope.maxClimbAngle();
        double howFarPastTheLimit = spanToVertical < 1.0E-6
                ? 1.0 : Mth.clamp((steepest - envelope.maxClimbAngle()) / spanToVertical, 0.0, 1.0);
        return envelope.maxSpeed() * Mth.lerp(howFarPastTheLimit, 1.0, envelope.hoverSpeedFactor());
    }

    private static double legPitch(Vec3 from, Vec3 to) {
        Vec3 leg = to.subtract(from);
        return Math.abs(Mth.atan2(leg.y, leg.horizontalDistance()));
    }

    // ---- pass 2: backwards, can it slow down in time ----

    /**
     * Nothing may be going faster than it can shed before the next limit bites. Walking from the end
     * spreads every slow node backwards into a braking ramp, and since drag is the only brake the
     * mob has and drag is geometric, the entry speed is a closed form rather than a simulation.
     */
    private static void brakingLimits(double[] arc, double[] limit, FlightEnvelope envelope) {
        for (int i = limit.length - 2; i >= 0; i--) {
            double legLength = arc[i + 1] - arc[i];
            limit[i] = Math.min(limit[i], envelope.maxEntrySpeed(limit[i + 1], legLength));
        }
    }

    // ---- pass 3: forwards, can it speed up in time ----

    /**
     * Nothing may be going faster than it could have accelerated up to on the way in. Stops the
     * profile promising cruise speed one block after a corner that forced a crawl.
     * <p>
     * Safe to run after pass 2 and safe to run only once: a body under thrust and below terminal
     * speed never slows down, so this can only ever raise a node's speed relative to its predecessor,
     * and therefore can never invalidate the braking guarantee pass 2 just established.
     * <p>
     * Starts from {@code limit[0]}, the envelope's top speed, rather than from the mob's real current
     * speed. That errs optimistic on purpose: being under the limit is always safe (the follower just
     * accelerates towards it), and keying off live speed would mean replanning every time the mob
     * fell behind.
     */
    private static void accelerationLimits(double[] arc, double[] limit, FlightEnvelope envelope) {
        for (int i = 1; i < limit.length; i++) {
            double legLength = arc[i] - arc[i - 1];
            limit[i] = Math.min(limit[i], envelope.speedAfterAccelerating(limit[i - 1], legLength));
        }
    }

    /** To (-PI, PI], so a turn past the wrap point does not read as a near full circle. */
    private static double wrapRadians(double radians) {
        double wrapped = radians % (2.0 * Math.PI);
        if (wrapped >= Math.PI) {
            wrapped -= 2.0 * Math.PI;
        } else if (wrapped < -Math.PI) {
            wrapped += 2.0 * Math.PI;
        }
        return wrapped;
    }
}
