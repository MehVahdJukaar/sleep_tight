package net.mehvahdjukaar.sleep_tight.test.throttle;

import net.minecraft.util.Mth;

/**
 * A speed limit for every point along a path, indexed by arc length rather than by node index
 * because arc length is what the follower's cursor is. Produced by {@link ThrottlePlanner} once when
 * the path is adopted, then only read.
 * <p>
 * This is the contract between the planning layers and the follower: the path says where to fly,
 * this says how fast that is allowed to be, and the follower's only job is to stay under it. Nothing
 * here knows about the mob or the world.
 * <p>
 * See HOW_IT_WORKS.md next to this file for how the numbers are arrived at.
 */
public final class ThrottleProfile {

    // parallel arrays, one entry per path node: how far along, and how fast is allowed there
    private final double[] arc;
    private final double[] limit;
    private final double expectedTicks;

    ThrottleProfile(double[] arc, double[] limit) {
        this.arc = arc;
        this.limit = limit;
        this.expectedTicks = sumFlightTime(arc, limit);
    }

    /** Each leg at the mean of its two end speeds. Good enough: the ramps between them are linear. */
    private static double sumFlightTime(double[] arc, double[] limit) {
        double ticks = 0.0;
        for (int i = 0; i < arc.length - 1; i++) {
            double meanSpeed = 0.5 * (limit[i] + limit[i + 1]);
            if (meanSpeed > 1.0E-6) {
                ticks += (arc[i + 1] - arc[i]) / meanSpeed;
            }
        }
        return ticks;
    }

    public int nodeCount() {
        return this.arc.length;
    }

    public double length() {
        return this.arc[this.arc.length - 1];
    }

    public double arcAtNode(int index) {
        return this.arc[Mth.clamp(index, 0, this.arc.length - 1)];
    }

    public double limitAtNode(int index) {
        return this.limit[Mth.clamp(index, 0, this.limit.length - 1)];
    }

    /** Interpolated between the two nodes bracketing this arc length, so the ramps are continuous. */
    public double speedLimitAt(double distance) {
        int last = this.arc.length - 1;
        double clamped = Mth.clamp(distance, 0.0, this.arc[last]);
        int i = this.nodeAtOrBefore(clamped);
        if (i >= last) {
            return this.limit[last];
        }
        double legLength = this.arc[i + 1] - this.arc[i];
        if (legLength < 1.0E-9) {
            return Math.min(this.limit[i], this.limit[i + 1]);
        }
        return Mth.lerp((clamped - this.arc[i]) / legLength, this.limit[i], this.limit[i + 1]);
    }

    /**
     * How long flying this path should take at the profiled speeds. Vanilla's per-node timeout
     * budgets from cruise speed and so fires spuriously once the mob starts slowing for corners;
     * this is the number that watchdog should be using instead.
     */
    public double expectedFlightTicks() {
        return this.expectedTicks;
    }

    /** Index of the last node at or before this arc length. Binary search, the arc array is sorted. */
    private int nodeAtOrBefore(double distance) {
        int low = 0;
        int high = this.arc.length - 1;
        while (low < high) {
            int mid = (low + high + 1) >>> 1;
            if (this.arc[mid] <= distance) {
                low = mid;
            } else {
                high = mid - 1;
            }
        }
        return low;
    }
}
