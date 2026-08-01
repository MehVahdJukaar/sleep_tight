package net.mehvahdjukaar.sleep_tight.test.debug;

import net.mehvahdjukaar.sleep_tight.test.pathfinding.BirdNode;
import net.mehvahdjukaar.sleep_tight.test.pathfinding.BirdPathfindingConfig;
import net.mehvahdjukaar.sleep_tight.test.pathfinding.EdgeCost;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.PathType;
import org.jetbrains.annotations.Nullable;

/**
 * The parts of a pathfinder {@link Node} the debug renderer actually draws. Everything else on
 * Node is search state that only means something on the server.
 * <p>
 * {@code speedLimit} does not come off the node at all, it comes from the throttle profile planned
 * for the path afterwards, so it is negative for the open and closed sets and for any node the
 * profile did not cover.
 */
public record DebugNode(int x, int y, int z, float costMalus, float clearanceCost, float speedLimit,
                        PathType type, EdgeCost edgeCost) {

    public static final float UNKNOWN_SPEED = -1.0F;

    public static DebugNode of(Node node) {
        return of(node, UNKNOWN_SPEED, null);
    }

    /**
     * {@code next} is the successor the search settled on, so the node can report what that step
     * cost it. Null for the last node of a path and for the open and closed sets, which are single
     * states rather than steps.
     */
    public static DebugNode of(Node node, float speedLimit, @Nullable Node next) {
        // clearance is charged as an edge cost, so it is not in costMalus and only lattice nodes
        // have it. The node carries the raw 0..1 measurement, the charge is that times wallHugCost
        float clearance = node instanceof BirdNode bird
                ? bird.enclosure * BirdPathfindingConfig.wallHugCost : 0;
        EdgeCost edgeCost = next != null ? EdgeCost.between(node, next) : EdgeCost.NONE;
        return new DebugNode(node.x, node.y, node.z, node.costMalus, clearance, speedLimit, node.type, edgeCost);
    }

    /**
     * A move that was offered at a path node. The edge cost is what reaching this cell would have
     * cost, i.e. the number the chosen step was compared against, so it is the price of arriving
     * here rather than of leaving as it is on a path node.
     */
    public static DebugNode considered(Node from, Node to) {
        return new DebugNode(to.x, to.y, to.z, to.costMalus, 0, UNKNOWN_SPEED, to.type,
                EdgeCost.between(from, to));
    }

    public static DebugNode read(FriendlyByteBuf buf) {
        return new DebugNode(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readEnum(PathType.class),
                new EdgeCost(buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat()));
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(this.x);
        buf.writeVarInt(this.y);
        buf.writeVarInt(this.z);
        buf.writeFloat(this.costMalus);
        buf.writeFloat(this.clearanceCost);
        buf.writeFloat(this.speedLimit);
        buf.writeEnum(this.type);
        buf.writeFloat(this.edgeCost.distance());
        buf.writeFloat(this.edgeCost.malus());
        buf.writeFloat(this.edgeCost.clearance());
        buf.writeFloat(this.edgeCost.vertical());
        buf.writeFloat(this.edgeCost.turn());
    }

    public boolean hasSpeedLimit() {
        return this.speedLimit >= 0.0F;
    }

    public BlockPos asBlockPos() {
        return new BlockPos(this.x, this.y, this.z);
    }
}
