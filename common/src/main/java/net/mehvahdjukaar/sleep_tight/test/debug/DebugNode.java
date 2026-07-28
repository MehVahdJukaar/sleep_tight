package net.mehvahdjukaar.sleep_tight.test.debug;

import net.mehvahdjukaar.sleep_tight.test.pathfinding.BirdNode;
import net.mehvahdjukaar.sleep_tight.test.pathfinding.BirdPathfindingConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.PathType;

/**
 * The parts of a pathfinder {@link Node} the debug renderer actually draws. Everything else on
 * Node is search state that only means something on the server.
 * <p>
 * {@code speedLimit} does not come off the node at all, it comes from the throttle profile planned
 * for the path afterwards, so it is negative for the open and closed sets and for any node the
 * profile did not cover.
 */
public record DebugNode(int x, int y, int z, float costMalus, float clearanceCost, float speedLimit,
                        PathType type) {

    public static final float UNKNOWN_SPEED = -1.0F;

    public static DebugNode of(Node node) {
        return of(node, UNKNOWN_SPEED);
    }

    public static DebugNode of(Node node, float speedLimit) {
        // clearance is charged as an edge cost, so it is not in costMalus and only lattice nodes
        // have it. The node carries the raw 0..1 measurement, the charge is that times wallHugCost
        float clearance = node instanceof BirdNode bird
                ? bird.enclosure * BirdPathfindingConfig.wallHugCost : 0;
        return new DebugNode(node.x, node.y, node.z, node.costMalus, clearance, speedLimit, node.type);
    }

    public static DebugNode read(FriendlyByteBuf buf) {
        return new DebugNode(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readEnum(PathType.class));
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(this.x);
        buf.writeVarInt(this.y);
        buf.writeVarInt(this.z);
        buf.writeFloat(this.costMalus);
        buf.writeFloat(this.clearanceCost);
        buf.writeFloat(this.speedLimit);
        buf.writeEnum(this.type);
    }

    public boolean hasSpeedLimit() {
        return this.speedLimit >= 0.0F;
    }

    public BlockPos asBlockPos() {
        return new BlockPos(this.x, this.y, this.z);
    }
}
