package net.mehvahdjukaar.sleep_tight.test.debug;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.PathType;

/**
 * The parts of a pathfinder {@link Node} the debug renderer actually draws. Everything else on
 * Node is search state that only means something on the server.
 */
public record DebugNode(int x, int y, int z, float costMalus, PathType type) {

    public static DebugNode of(Node node) {
        return new DebugNode(node.x, node.y, node.z, node.costMalus, node.type);
    }

    public static DebugNode read(FriendlyByteBuf buf) {
        return new DebugNode(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                buf.readFloat(), buf.readEnum(PathType.class));
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(this.x);
        buf.writeVarInt(this.y);
        buf.writeVarInt(this.z);
        buf.writeFloat(this.costMalus);
        buf.writeEnum(this.type);
    }

    public BlockPos asBlockPos() {
        return new BlockPos(this.x, this.y, this.z);
    }
}
