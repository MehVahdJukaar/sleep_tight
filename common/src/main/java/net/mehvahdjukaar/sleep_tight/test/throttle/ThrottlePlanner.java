package net.mehvahdjukaar.sleep_tight.test.throttle;

import net.mehvahdjukaar.sleep_tight.test.pathfinding.BirdNode;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

/**
 * Works out how fast the bird may be at every point of a finished path.
 * <p>
 * The layer between the search, which knows where the bird can go, and the follower, which has to
 * actually fly it. It exists because a turn radius is {@code speed / yawRate}: a corner too tight
 * to fly at cruise is perfectly flyable slower, so the answer to a sharp corner is not to forbid it
 * in the search but to arrive at it slowly. Working that out once per path is both cheaper and more
 * correct than the follower guessing at it every tick from the nodes still ahead of it.
 * <p>
 * Pure geometry and physics: no Level, no Mob, no tick. {@link #plan} takes plain arrays precisely
 * so it can be tested without a game running.
 *
 * <h2>The three passes</h2>
 * Standard velocity profiling, the same shape a CNC or a robot trajectory planner uses.
 * <ol>
 *   <li><b>Local limits.</b> Each node gets the tightest speed its own geometry allows: how sharp
 *       the corner is, how much room there is around it to bulge into, and how steep the climb is.
 *   <li><b>Backwards.</b> Walk from the end and cap each node by what it can still brake down from.
 *       Drag is the only brake available, so this is what stops the profile asking for a slowdown
 *       that physically cannot happen in the distance left.
 *   <li><b>Forwards.</b> Walk from the start and cap each node by what it could accelerate up to.
 *       Stops the profile promising speed the bird cannot reach in time.
 * </ol>
 * One pass each is enough, and the order matters: pass 3 can only ever raise a node's entry speed
 * relative to its predecessor, so it can never invalidate the braking guarantee pass 2 established.
 */
public final class ThrottlePlanner {

    /** Below this share of the envelope's top speed a node counts as "the profile is biting here". */
    static final double UNCONSTRAINED_FRACTION = 0.95;

    // a turn under this is a rounding artifact of the 45 degree heading lattice, not a corner
    private static final double MIN_TURN_ANGLE = Math.toRadians(1.0);
    // guards k(theta) blowing up as the turn approaches a full reversal
    private static final double MAX_TURN_ANGLE = Math.toRadians(175.0);
    private static final double MIN_HORIZONTAL_LEG = 1.0E-4;

    private ThrottlePlanner() {
    }

    /**
     * Convenience wrapper: pulls the node positions and the clearance the search already measured
     * off a finished path. Positions come from the path rather than the raw nodes so the profile is
     * built against the same points the follower will be steering at.
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
        int count = points.length;
        double[] arc = new double[count];
        double[] limit = new double[count];
        if (count == 0) {
            return new ThrottleProfile(new double[]{0.0}, new double[]{0.0});
        }

        for (int i = 1; i < count; i++) {
            arc[i] = arc[i - 1] + points[i].distanceTo(points[i - 1]);
        }

        localLimits(points, enclosure, envelope, limit);
        brakingPass(arc, limit, envelope);
        accelerationPass(arc, limit, envelope);

        return new ThrottleProfile(arc, limit);
    }

    /** Pass 1. What each node's own shape allows, before anything about reaching it is considered. */
    private static void localLimits(Vec3[] points, float[] enclosure, FlightEnvelope envelope, double[] limit) {
        int last = points.length - 1;
        for (int i = 0; i <= last; i++) {
            double speed = envelope.maxSpeed();
            if (i > 0 && i < last) {
                speed = Math.min(speed, cornerLimit(points[i - 1], points[i], points[i + 1],
                        enclosure[i], envelope));
            }
            speed = Math.min(speed, climbLimit(points, i, envelope));
            limit[i] = Mth.clamp(speed, envelope.minSpeed(), envelope.maxSpeed());
        }
        // arrival is the one place the mob is allowed below its cruising floor
        limit[last] = Math.min(limit[last], envelope.arrivalSpeed());
    }

    /**
     * The corner rule, and the reason this class exists.
     * <p>
     * A body turning at rate {@code w} while moving at {@code v} traces a circle of radius
     * {@code v/w}. Fitting that circle into a corner of angle {@code theta} leaves it passing
     * {@code r * (1/cos(theta/2) - 1)} blocks inside the corner point. Turn that around: given how
     * far off the drawn line we are willing to drift, the speed that keeps us inside it is
     * <pre>v = w * margin / (1/cos(theta/2) - 1)</pre>
     * The margin shrinks with how walled in the cell is, since the search only certified the cells
     * on the line as clear and a cut corner leaves that line.
     */
    private static double cornerLimit(Vec3 before, Vec3 at, Vec3 after, float enclosure, FlightEnvelope envelope) {
        double turn = horizontalTurn(before, at, after);
        if (turn < MIN_TURN_ANGLE) {
            return Double.MAX_VALUE;
        }
        double margin = envelope.corridorMargin() * (1.0 - Mth.clamp(enclosure, 0.0F, 1.0F));
        double overshootPerRadius = 1.0 / Math.cos(Math.min(turn, MAX_TURN_ANGLE) / 2.0) - 1.0;
        return envelope.maxYawRate() * margin / overshootPerRadius;
    }

    /** Signed-magnitude heading change between the leg arriving at {@code at} and the one leaving it. */
    private static double horizontalTurn(Vec3 before, Vec3 at, Vec3 after) {
        Vec3 in = at.subtract(before);
        Vec3 out = after.subtract(at);
        if (in.horizontalDistanceSqr() < MIN_HORIZONTAL_LEG || out.horizontalDistanceSqr() < MIN_HORIZONTAL_LEG) {
            // one of the two legs is a pure vertical hop, which keeps the heading. The climb limit
            // is what prices those, there is no corner here to fly around
            return 0.0;
        }
        double delta = Mth.atan2(out.z, out.x) - Mth.atan2(in.z, in.x);
        return Math.abs(wrapRadians(delta));
    }

    /**
     * Steep legs cost speed. Not a physical limit the way the corner rule is: nothing stops the mob
     * climbing fast, but a bird with no forward airspeed is hovering, which is slow and effortful,
     * and pretending otherwise is what makes flight read as an elevator. Below
     * {@code maxClimbAngle} this does nothing at all.
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
            return Double.MAX_VALUE;
        }
        double span = Mth.HALF_PI - envelope.maxClimbAngle();
        double t = span < 1.0E-6 ? 1.0 : Mth.clamp((steepest - envelope.maxClimbAngle()) / span, 0.0, 1.0);
        return envelope.maxSpeed() * Mth.lerp(t, 1.0, envelope.hoverSpeedFactor());
    }

    private static double legPitch(Vec3 from, Vec3 to) {
        Vec3 leg = to.subtract(from);
        return Math.abs(Mth.atan2(leg.y, leg.horizontalDistance()));
    }

    /** Pass 2. Nothing may be going faster than it can shed before the next limit bites. */
    private static void brakingPass(double[] arc, double[] limit, FlightEnvelope envelope) {
        for (int i = limit.length - 2; i >= 0; i--) {
            double legLength = arc[i + 1] - arc[i];
            limit[i] = Math.min(limit[i], envelope.maxEntrySpeed(limit[i + 1], legLength));
        }
    }

    /** Pass 3. Nothing may be going faster than it could have got up to on the way in. */
    private static void accelerationPass(double[] arc, double[] limit, FlightEnvelope envelope) {
        for (int i = 1; i < limit.length; i++) {
            double legLength = arc[i] - arc[i - 1];
            limit[i] = Math.min(limit[i], envelope.speedAfterAccelerating(limit[i - 1], legLength));
        }
    }

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
