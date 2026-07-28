package net.mehvahdjukaar.sleep_tight.test.throttle;

import net.minecraft.util.Mth;

/**
 * A speed limit for every point along a path, as arc length rather than node index. Produced by
 * {@link ThrottlePlanner} once when the path is adopted, then only read.
 * <p>
 * This is the contract between the two planning layers and the follower: the path says where to
 * fly, this says how fast that is allowed to be, and the follower's only job is to stay under it.
 * Nothing here knows about the mob or the world.
 */
public final class ThrottleProfile {

    private final double[] arc;
    private final double[] limit;
    private final double expectedTicks;
    private final int constrainedNodes;

    ThrottleProfile(double[] arc, double[] limit) {
        this.arc = arc;
        this.limit = limit;
        double ticks = 0.0;
        int constrained = 0;
        for (int i = 0; i < arc.length - 1; i++) {
            double legLength = arc[i + 1] - arc[i];
            double meanSpeed = 0.5 * (limit[i] + limit[i + 1]);
            if (meanSpeed > 1.0E-6) {
                ticks += legLength / meanSpeed;
            }
        }
        for (double v : limit) {
            if (v < ThrottlePlanner.UNCONSTRAINED_FRACTION * maxOf(limit)) {
                constrained++;
            }
        }
        this.expectedTicks = ticks;
        this.constrainedNodes = constrained;
    }

    private static double maxOf(double[] values) {
        double max = 0.0;
        for (double v : values) {
            max = Math.max(max, v);
        }
        return max;
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
        int i = this.segmentAt(clamped);
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
     * The tightest limit anywhere in the next {@code window} blocks. The follower needs this rather
     * than the limit under its feet: it can only shed speed by coasting, so it has to start slowing
     * about a block before a corner rather than on top of it.
     */
    public double speedLimitOver(double from, double window) {
        double end = from + window;
        double tightest = Math.min(this.speedLimitAt(from), this.speedLimitAt(end));
        for (int i = this.segmentAt(Math.max(from, 0.0)); i < this.arc.length && this.arc[i] <= end; i++) {
            if (this.arc[i] >= from) {
                tightest = Math.min(tightest, this.limit[i]);
            }
        }
        return tightest;
    }

    /**
     * How long flying this path should take at the profiled speeds. Vanilla's per-node timeout
     * budgets from cruise speed and so fires spuriously once the mob starts slowing for corners;
     * this is the number that watchdog should be using instead.
     */
    public double expectedFlightTicks() {
        return this.expectedTicks;
    }

    /** How many nodes are held meaningfully below the envelope's top speed, for the debug overlay. */
    public int constrainedNodes() {
        return this.constrainedNodes;
    }

    private int segmentAt(double distance) {
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
