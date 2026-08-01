package net.mehvahdjukaar.sleep_tight.test.debug;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Live navigation/steering state, the bits of "what is the mob thinking right now" that
 * {@link DebugPath} does not carry because they change every tick and are not part of the
 * search result: whether the navigation considers itself stuck or done, where it is steering
 * towards, how far along the {@link net.mehvahdjukaar.sleep_tight.test.navigator.PathRuler} the
 * mob has gotten and how far off the line it is, and the two vanilla watchdog timers that can kill a
 * path out from under it: the
 * 100-tick distance-based stuck check and the per-node timeout (three times the expected travel
 * time for the current node). The two are independent - the timeout clears {@code isStuck} right
 * before it stops the path, so it never shows up as "stuck" even though it gave up just the same.
 * <p>
 * {@code steering} is the exact condition {@code BirdMoveControl.tick()} itself branches on, which
 * includes the ground layer's launch hold: a bird turning on the spot to line up with a fresh path
 * has a waypoint and an unfinished path and is still deliberately not flying. {@code gait} is the
 * phase that hold comes from, and is the first thing to read when a mob will not leave the ground.
 * {@code operation} is the raw vanilla
 * {@code MoveControl.Operation} name and is kept only for reference: neither {@code BirdMoveControl}
 * nor vanilla's own {@code SmoothSwimmingMoveControl} (which uses the same pattern) ever resets it
 * back to {@code WAIT}, so once a mob has been given a single waypoint it reads {@code MOVE_TO}
 * forever - {@code steering} is what actually answers "is it doing something right now."
 */
public record MobDebugInfo(boolean stuck, boolean pathDone, boolean steering, String operation,
                           String gait, float launchYaw,
                           Vec3 mobPos, Vec3 wantedPos, Vec3 velocity, float yRot,
                           double rulerCursor, double rulerLength, double offRoute,
                           int nextNodeIndex, int nodeCount,
                           long timeoutTimer, double timeoutLimit, int ticksSinceStuckCheck,
                           double speedLimitNow, double speedLimitCommanded,
                           int trailEpoch, List<Vec3> trailSamples) {

    public static MobDebugInfo read(FriendlyByteBuf buf) {
        return new MobDebugInfo(buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readUtf(),
                buf.readUtf(), buf.readFloat(),
                readVec3(buf), readVec3(buf), readVec3(buf), buf.readFloat(),
                buf.readDouble(), buf.readDouble(), buf.readDouble(),
                buf.readVarInt(), buf.readVarInt(),
                buf.readVarLong(), buf.readDouble(), buf.readVarInt(),
                buf.readDouble(), buf.readDouble(), buf.readVarInt(), readTrail(buf));
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBoolean(this.stuck);
        buf.writeBoolean(this.pathDone);
        buf.writeBoolean(this.steering);
        buf.writeUtf(this.operation);
        buf.writeUtf(this.gait);
        buf.writeFloat(this.launchYaw);
        writeVec3(buf, this.mobPos);
        writeVec3(buf, this.wantedPos);
        writeVec3(buf, this.velocity);
        buf.writeFloat(this.yRot);
        buf.writeDouble(this.rulerCursor);
        buf.writeDouble(this.rulerLength);
        buf.writeDouble(this.offRoute);
        buf.writeVarInt(this.nextNodeIndex);
        buf.writeVarInt(this.nodeCount);
        buf.writeVarLong(this.timeoutTimer);
        buf.writeDouble(this.timeoutLimit);
        buf.writeVarInt(this.ticksSinceStuckCheck);
        buf.writeDouble(this.speedLimitNow);
        buf.writeDouble(this.speedLimitCommanded);
        buf.writeVarInt(this.trailEpoch);
        buf.writeVarInt(this.trailSamples.size());
        for (Vec3 point : this.trailSamples) {
            writeVec3(buf, point);
        }
    }

    /**
     * How far the velocity vector has fallen behind where the body is pointing, in degrees. The
     * number section 1 of {@code believable_bird_flight.md} is about: thrust only ever pushes along
     * yaw and nothing damps velocity across the body, so in a sustained turn the mob ends up
     * crabbing sideways. Anything much past 20 degrees is visible as a drone sliding through an arc.
     */
    public double sideslipDegrees() {
        if (this.velocity.horizontalDistanceSqr() < 1.0E-8) {
            return 0.0;
        }
        double travelYaw = Math.toDegrees(Mth.atan2(this.velocity.z, this.velocity.x)) - 90.0;
        return Math.abs(Mth.degreesDifference(this.yRot, (float) travelYaw));
    }

    /** Which way the body is pointing, for drawing it against the direction of travel. */
    public Vec3 facing() {
        return Vec3.directionFromRotation(0.0F, this.yRot);
    }

    /** Where the ground layer is swinging the body round to before it lets go of the ground. */
    public Vec3 launchFacing() {
        return Vec3.directionFromRotation(0.0F, this.launchYaw);
    }

    /** How much of the launch turn is left, in degrees. Only meaningful while holding for one. */
    public double launchYawError() {
        return Mth.degreesDifference(this.yRot, this.launchYaw);
    }

    /**
     * True while the ground layer is holding the mob on its feet to line it up with the path.
     * Carried by {@code launchYaw} being a real angle rather than by matching the gait name, so the
     * renderer never has to know what the ground layer calls its phases.
     */
    public boolean holdingForLaunch() {
        return !Float.isNaN(this.launchYaw);
    }

    /** The budget {@code timeoutTimer} gets before {@code timeoutPath()} fires and kills the path. */
    public double timeoutBudget() {
        return this.timeoutLimit * 3.0;
    }

    private static Vec3 readVec3(FriendlyByteBuf buf) {
        return new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
    }

    private static List<Vec3> readTrail(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<Vec3> trail = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            trail.add(readVec3(buf));
        }
        return trail;
    }

    private static void writeVec3(FriendlyByteBuf buf, Vec3 vec) {
        buf.writeDouble(vec.x);
        buf.writeDouble(vec.y);
        buf.writeDouble(vec.z);
    }
}
