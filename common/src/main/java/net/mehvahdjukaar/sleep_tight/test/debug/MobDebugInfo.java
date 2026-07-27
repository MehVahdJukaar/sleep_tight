package net.mehvahdjukaar.sleep_tight.test.debug;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

/**
 * Live navigation/steering state, the bits of "what is the mob thinking right now" that
 * {@link DebugPath} does not carry because they change every tick and are not part of the
 * search result: whether the navigation considers itself stuck or done, what the move control
 * is currently doing, where it is steering towards, and how far along the {@link
 * net.mehvahdjukaar.sleep_tight.test.navigator.PathRuler} the mob has gotten.
 */
public record MobDebugInfo(boolean stuck, boolean pathDone, String operation, Vec3 wantedPos,
                           Vec3 velocity, double rulerCursor, double rulerLength,
                           int nextNodeIndex, int nodeCount) {

    public static MobDebugInfo read(FriendlyByteBuf buf) {
        return new MobDebugInfo(buf.readBoolean(), buf.readBoolean(), buf.readUtf(),
                readVec3(buf), readVec3(buf), buf.readDouble(), buf.readDouble(),
                buf.readVarInt(), buf.readVarInt());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBoolean(this.stuck);
        buf.writeBoolean(this.pathDone);
        buf.writeUtf(this.operation);
        writeVec3(buf, this.wantedPos);
        writeVec3(buf, this.velocity);
        buf.writeDouble(this.rulerCursor);
        buf.writeDouble(this.rulerLength);
        buf.writeVarInt(this.nextNodeIndex);
        buf.writeVarInt(this.nodeCount);
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