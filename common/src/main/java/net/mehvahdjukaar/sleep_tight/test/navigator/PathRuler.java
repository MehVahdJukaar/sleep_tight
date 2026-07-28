package net.mehvahdjukaar.sleep_tight.test.navigator;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

/**
 * Arc length view of a {@link Path}, plus a cursor that only ever moves forward along it.
 * <p>
 * The point is to stop treating a path as a queue of spheres to be inside of. A turn limited flier
 * regularly misses a node on the outside of an arc, and once the node is behind it the commanded
 * heading reverses. Here the mob is projected onto the curve instead, so missing a node stops being
 * an event, and the follower aims at a point a fixed distance further along, which keeps the
 * commanded heading continuous and rounds corners off by roughly that distance.
 * <p>
 * Pure geometry: it knows nothing about the mob beyond where it is. Node positions are read once,
 * since the search is finished by the time a path exists and the shape never changes afterwards.
 */
public class PathRuler {

    private final Vec3[] points;
    // arc length from the first node to node i, so distanceAt[0] is always 0
    private final double[] distanceAt;

    private double cursor;
    // leg the cursor currently sits on, kept around so neither lookup has to rescan from the start
    private int segment;

    public PathRuler(Path path, Entity entity) {
        int count = path.getNodeCount();
        this.points = new Vec3[count];
        this.distanceAt = new double[count];
        for (int i = 0; i < count; i++) {
            this.points[i] = path.getEntityPosAtNode(entity, i);
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

    /**
     * Slides the cursor to the projection of {@code pos} onto the path, looking only at the stretch
     * between the cursor and {@code window} further along. Forward only is what keeps the cursor
     * monotone, and the window is what stops it snapping onto the return leg of a hairpin and
     * shortcutting it, which the lattice is perfectly capable of emitting.
     */
    public void advanceCursorTo(Vec3 pos, double window) {
        double limit = this.cursor + window;
        double closestDistance = this.cursor;
        double closestOffsetSqr = Double.MAX_VALUE;

        for (int i = this.segment; i < this.points.length - 1 && this.distanceAt[i] <= limit; i++) {
            Vec3 start = this.points[i];
            Vec3 leg = this.points[i + 1].subtract(start);
            double legLengthSqr = leg.lengthSqr();
            if (legLengthSqr < 1.0E-9) {
                continue;
            }
            double legLength = Math.sqrt(legLengthSqr);
            // never project behind where the cursor already is, even on the leg it sits on
            double lowest = Mth.clamp((this.cursor - this.distanceAt[i]) / legLength, 0.0, 1.0);
            double along = Mth.clamp(pos.subtract(start).dot(leg) / legLengthSqr, lowest, 1.0);
            double offsetSqr = start.add(leg.scale(along)).distanceToSqr(pos);
            if (offsetSqr < closestOffsetSqr) {
                closestOffsetSqr = offsetSqr;
                closestDistance = this.distanceAt[i] + along * legLength;
            }
        }

        this.cursor = closestDistance;
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
        int i = this.segment;
        while (i < last - 1 && this.distanceAt[i + 1] <= clamped) {
            i++;
        }
        if (i >= last) {
            return this.points[last];
        }
        double legLength = this.distanceAt[i + 1] - this.distanceAt[i];
        double along = legLength < 1.0E-9 ? 0.0 : (clamped - this.distanceAt[i]) / legLength;
        return this.points[i].add(this.points[i + 1].subtract(this.points[i]).scale(along));
    }

    /**
     * Index of the first node the cursor has not passed, so the path's own node index can be kept in
     * step with it. Reaching the node count is what makes {@code Path.isDone} fire, so the cursor
     * running off the end of the ruler is what ends the path.
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
