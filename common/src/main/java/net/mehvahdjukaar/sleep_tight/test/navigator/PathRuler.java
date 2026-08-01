package net.mehvahdjukaar.sleep_tight.test.navigator;

import net.mehvahdjukaar.sleep_tight.test.pathfinding.BirdNode;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Arc length view of a {@link Path}, plus a cursor tracking where the mob projects onto it.
 * <p>
 * The point is to stop treating a path as a queue of spheres to be inside of. A turn limited flier
 * regularly misses a node on the outside of an arc, and once the node is behind it the commanded
 * heading reverses. Here the mob is projected onto the curve instead, so missing a node stops being
 * an event, and the follower aims at a point further along, which keeps the commanded heading
 * continuous and rounds corners off by roughly that distance.
 * <p>
 * Besides the cursor it measures the two things the follower's policy is driven by: how far off the
 * line the mob is, and how much of the drawn line it is rounding off, see {@link #groundPerArc()}.
 * <p>
 * Pure geometry: it knows nothing about the mob beyond where it is. Node positions are read once,
 * since the search is finished by the time a path exists and the shape never changes afterwards.
 */
public class PathRuler {

    /**
     * Ceiling on how much the cursor may move in a tick, either way, as a multiple of the ground
     * actually covered. Rounding a corner off legitimately advances the cursor faster than the mob
     * flies, since it takes the short way across while the foot of the perpendicular walks both
     * legs, but only by so much: on a 90 degree corner, the sharpest the lattice allows, that ratio
     * tops out around 1.4.
     */
    private static final double MAX_ARC_PER_GROUND = 2.0;

    private final Vec3[] points;
    // how walled in each node is, 0 open air through 1 fully boxed in. Same source the throttle
    // planner prices corners from, read here so the follower can size its aim by it
    private final float[] enclosure;
    // arc length from the first node to node i, so distanceAt[0] is always 0
    private final double[] distanceAt;

    private double cursor;
    // leg the cursor currently sits on, kept around so neither lookup has to rescan from the start
    private int segment;
    private double offRoute;
    // last tick's movement along the line and through the air
    private double arcRate;
    private double groundRate;
    @Nullable
    private Vec3 lastPosition;

    public PathRuler(Path path, Entity entity) {
        int count = path.getNodeCount();
        this.points = new Vec3[count];
        this.enclosure = new float[count];
        this.distanceAt = new double[count];
        for (int i = 0; i < count; i++) {
            this.points[i] = path.getEntityPosAtNode(entity, i);
            // a path that went through trimPath can contain plain vanilla nodes, so this is optional
            this.enclosure[i] = path.getNode(i) instanceof BirdNode bird ? bird.enclosure : 0.0F;
            if (i > 0) {
                this.distanceAt[i] = this.distanceAt[i - 1] + this.points[i].distanceTo(this.points[i - 1]);
            }
        }
    }

    public double length() {
        return this.distanceAt[this.distanceAt.length - 1];
    }

    public double cursor() {
        return this.cursor;
    }

    /** Arc length still ahead of the cursor. Zero once the mob has projected past the last node. */
    public double remaining() {
        return this.length() - this.cursor;
    }

    /** How far the mob is from the drawn line, in blocks. Near zero on a route being flown exactly. */
    public double offRoute() {
        return this.offRoute;
    }

    /**
     * How much ground the mob covered last tick per block of route it got through, capped at one.
     * <p>
     * One for a mob tracing the line, less for one cutting a corner: the cursor sweeps along both
     * legs while the mob takes the short way across. That gap is what makes the profile's braking
     * ramps optimistic, since drag only sheds speed per block actually flown, so this is the factor
     * the follower's real braking authority has to be scaled by.
     */
    public double groundPerArc() {
        return this.arcRate > 1.0E-6 ? Math.min(1.0, this.groundRate / this.arcRate) : 1.0;
    }

    /**
     * Slides the cursor to the projection of {@code pos} onto the path, considering only the stretch
     * within {@code window} either side of it. The window is what stops the cursor snapping onto the
     * return leg of a hairpin and shortcutting it, which the lattice is perfectly capable of
     * emitting.
     * <p>
     * The cursor is allowed to run backwards. Ratcheting it forward looks like the safe choice and
     * is not: a mob shoved sideways off a diagonal watches its own perpendicular foot slide up the
     * line, which is real geometry rather than cheating, but a ratchet then refuses to give that
     * progress back when it flies home. It ends up reading "almost arrived" from a dozen blocks out,
     * is told to slow to arrival speed there, and creeps until a watchdog kills it. What keeps the
     * cursor honest instead is the step cap: neither direction may outrun the ground covered by more
     * than {@link #MAX_ARC_PER_GROUND}.
     */
    public void advanceCursorTo(Vec3 pos, double window) {
        double lowest = this.cursor - window;
        double highest = this.cursor + window;
        double closestDistance = this.cursor;
        double closestOffsetSqr = Double.MAX_VALUE;

        int first = this.segment;
        while (first > 0 && this.distanceAt[first] > lowest) {
            first--;
        }
        for (int i = first; i < this.points.length - 1 && this.distanceAt[i] <= highest; i++) {
            Vec3 start = this.points[i];
            Vec3 leg = this.points[i + 1].subtract(start);
            double legLengthSqr = leg.lengthSqr();
            if (legLengthSqr < 1.0E-9) {
                continue;
            }
            double along = Mth.clamp(pos.subtract(start).dot(leg) / legLengthSqr, 0.0, 1.0);
            double offsetSqr = start.add(leg.scale(along)).distanceToSqr(pos);
            if (offsetSqr < closestOffsetSqr) {
                closestOffsetSqr = offsetSqr;
                closestDistance = this.distanceAt[i] + along * Math.sqrt(legLengthSqr);
            }
        }

        this.groundRate = this.lastPosition == null ? 0.0 : pos.distanceTo(this.lastPosition);
        this.lastPosition = pos;
        double maxStep = this.groundRate * MAX_ARC_PER_GROUND;
        double moved = Mth.clamp(closestDistance, this.cursor - maxStep, this.cursor + maxStep);
        this.arcRate = moved - this.cursor;
        this.cursor = moved;
        this.offRoute = closestOffsetSqr == Double.MAX_VALUE
                ? pos.distanceTo(this.pointAt(this.cursor)) : Math.sqrt(closestOffsetSqr);
        this.resyncSegment();
    }

    private void resyncSegment() {
        while (this.segment > 0 && this.distanceAt[this.segment] > this.cursor) {
            this.segment--;
        }
        while (this.segment < this.points.length - 2 && this.distanceAt[this.segment + 1] <= this.cursor) {
            this.segment++;
        }
    }

    /** The point the follower should aim at: {@code lookahead} further along the path than the cursor. */
    public Vec3 lookaheadPoint(double lookahead) {
        return this.pointAt(this.cursor + lookahead);
    }

    public Vec3 pointAt(double distance) {
        int last = this.points.length - 1;
        double clamped = Mth.clamp(distance, 0.0, this.length());
        int i = this.segmentAt(clamped);
        if (i >= last) {
            return this.points[last];
        }
        double legLength = this.distanceAt[i + 1] - this.distanceAt[i];
        double along = legLength < 1.0E-9 ? 0.0 : (clamped - this.distanceAt[i]) / legLength;
        return this.points[i].add(this.points[i + 1].subtract(this.points[i]).scale(along));
    }

    /**
     * How walled in the path is at this arc length, 0 for open air and 1 for fully boxed in. The dial
     * the follower reads to decide how much freedom it has to round off the line it was given: the
     * search only ever certified the cells on the line as clear, so leaving it is only safe where
     * there is measured room to leave it into.
     */
    public double enclosureAt(double distance) {
        int last = this.points.length - 1;
        double clamped = Mth.clamp(distance, 0.0, this.length());
        int i = this.segmentAt(clamped);
        if (i >= last) {
            return this.enclosure[last];
        }
        double legLength = this.distanceAt[i + 1] - this.distanceAt[i];
        double along = legLength < 1.0E-9 ? 0.0 : (clamped - this.distanceAt[i]) / legLength;
        return Mth.lerp(along, this.enclosure[i], this.enclosure[i + 1]);
    }

    /** Index of the leg containing this arc length. Binary search, the array is sorted. */
    private int segmentAt(double distance) {
        int low = 0;
        int high = this.points.length - 1;
        while (low < high) {
            int mid = (low + high + 1) >>> 1;
            if (this.distanceAt[mid] <= distance) {
                low = mid;
            } else {
                high = mid - 1;
            }
        }
        return low;
    }

    /**
     * Index of the first node the cursor has not passed, so the path's own node index can be kept in
     * step with it. Rederived from the cursor rather than only ever incremented, so a mob that has
     * genuinely lost ground goes back to chasing the node it lost instead of holding a claim on the
     * far end of a route it never flew. Reaching the node count is what makes {@code Path.isDone}
     * fire, so the cursor running off the end of the ruler is what ends the path.
     */
    public int nextNodeIndex() {
        for (int i = 0; i < this.distanceAt.length; i++) {
            if (this.distanceAt[i] > this.cursor) {
                return i;
            }
        }
        return this.distanceAt.length;
    }
}
