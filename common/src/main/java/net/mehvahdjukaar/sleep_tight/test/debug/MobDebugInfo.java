package net.mehvahdjukaar.sleep_tight.test.debug;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Live navigation/steering state, the bits of "what is the mob thinking right now" that
 * {@link DebugPath} does not carry because they change every tick and are not part of the
 * search result: whether the navigation considers itself stuck or done, where it is steering
 * towards, how far along the {@link net.mehvahdjukaar.sleep_tight.test.navigator.PathRuler} the
 * mob has gotten, and the two vanilla watchdog timers that can kill a path out from under it: the
 * 100-tick distance-based stuck check and the per-node timeout (three times the expected travel
 * time for the current node). The two are independent - the timeout clears {@code isStuck} right
 * before it stops the path, so it never shows up as "stuck" even though it gave up just the same.
 * <p>
 * {@code steering} is {@code hasWanted() && !navigation.isDone()}, i.e. the exact condition
 * {@code BirdMoveControl.tick()} itself branches on. {@code operation} is the raw vanilla
 * {@code MoveControl.Operation} name and is kept only for reference: neither {@code BirdMoveControl}
 * nor vanilla's own {@code SmoothSwimmingMoveControl} (which uses the same pattern) ever resets it
 * back to {@code WAIT}, so once a mob has been given a single waypoint it reads {@code MOVE_TO}
 * forever - {@code steering} is what actually answers "is it doing something right now."
 */
public record MobDebugInfo(boolean stuck, boolean pathDone, boolean steering, String operation,
                           Vec3 mobPos, Vec3 wantedPos, Vec3 velocity, float yRot,
                           double rulerCursor, double rulerLength,
                           int nextNodeIndex, int nodeCount,
                           long timeoutTimer, double timeoutLimit, int ticksSinceStuckCheck,
                           double speedLimitNow, double speedLimitAhead) {

    public static MobDebugInfo read(FriendlyByteBuf buf) {
        return new MobDebugInfo(buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readUtf(),
                readVec3(buf), readVec3(buf), readVec3(buf), buf.readFloat(),
                buf.readDouble(), buf.readDouble(),
                buf.readVarInt(), buf.readVarInt(),
                buf.readVarLong(), buf.readDouble(), buf.readVarInt(),
                buf.readDouble(), buf.readDouble());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBoolean(this.stuck);
        buf.writeBoolean(this.pathDone);
        buf.writeBoolean(this.steering);
        buf.writeUtf(this.operation);
        writeVec3(buf, this.mobPos);
        writeVec3(buf, this.wantedPos);
        writeVec3(buf, this.velocity);
        buf.writeFloat(this.yRot);
        buf.writeDouble(this.rulerCursor);
        buf.writeDouble(this.rulerLength);
        buf.writeVarInt(this.nextNodeIndex);
        buf.writeVarInt(this.nodeCount);
        buf.writeVarLong(this.timeoutTimer);
        buf.writeDouble(this.timeoutLimit);
        buf.writeVarInt(this.ticksSinceStuckCheck);
        buf.writeDouble(this.speedLimitNow);
        buf.writeDouble(this.speedLimitAhead);
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

    /** The budget {@code timeoutTimer} gets before {@code timeoutPath()} fires and kills the path. */
    public double timeoutBudget() {
        return this.timeoutLimit * 3.0;
    }

    private static Vec3 readVec3(FriendlyByteBuf buf) {
        return new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
    }

    private static void writeVec3(FriendlyByteBuf buf, Vec3 vec) {
        buf.writeDouble(vec.x);
        buf.writeDouble(vec.y);
        buf.writeDouble(vec.z);
    }
}
