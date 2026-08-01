package net.mehvahdjukaar.sleep_tight.test.debug;

import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Where the mob has actually been, sampled once per tick on the server so the planned line can be
 * compared against the one that was really flown.
 * <p>
 * Only the samples taken since the last packet go over the wire; the client stitches the whole
 * flight together itself and keeps it for as long as it keeps the path it belongs to. Resending
 * the full history every packet would cost several kB per player per packet and would force a
 * length cap, which is what loses the start of a long flight. {@link #epoch} is what tells the
 * client the history it has is still the same flight: it changes when a new path is issued, and a
 * client that sees a new epoch throws its copy away.
 * <p>
 * Nothing bounds the length, so sampling has to stop when the flight does - see
 * {@code BirdTestMob.tick}. A mob left hovering on a finished path would otherwise keep feeding
 * drift into it forever.
 */
public class MobTrail {

    // pending only grows between packets (five ticks), so this only ever bites if nothing is
    // draining it at all
    private static final int MAX_PENDING = 400;

    // a hovering mob would otherwise spend its whole buffer on the same point
    private static final double MIN_STEP_SQR = 1.0E-6;

    private final List<Vec3> pending = new ArrayList<>();
    private Vec3 lastSample;
    private int epoch;

    public void sample(Vec3 pos) {
        if (this.lastSample != null && this.lastSample.distanceToSqr(pos) < MIN_STEP_SQR) return;
        this.lastSample = pos;
        this.pending.add(pos);
        if (this.pending.size() > MAX_PENDING) {
            this.pending.subList(0, this.pending.size() - MAX_PENDING).clear();
        }
    }

    /** Starts a new flight: the client drops whatever it had drawn for this mob. */
    public void reset() {
        this.pending.clear();
        this.lastSample = null;
        this.epoch++;
    }

    public int epoch() {
        return this.epoch;
    }

    /** Oldest first. Handed to the network thread, so the buffer itself is never shared. */
    public List<Vec3> drainPending() {
        List<Vec3> drained = new ArrayList<>(this.pending);
        this.pending.clear();
        return drained;
    }
}
