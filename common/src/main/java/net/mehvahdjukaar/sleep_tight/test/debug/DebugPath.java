package net.mehvahdjukaar.sleep_tight.test.debug;

import net.mehvahdjukaar.sleep_tight.test.throttle.ThrottleProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Wire form of a {@link Path} for the debug renderer, plus the {@link ThrottleProfile} planned for
 * it. The two travel together because they are two halves of one plan: the path is the line, the
 * profile is how fast the mob is allowed to be along it, and looking at either on its own does not
 * tell you whether the bird is going to fly it.
 * <p>
 * We serialize this ourselves rather than going through {@code Path.writeToStream}: that method
 * silently writes nothing unless the path carries debug data, and vanilla's {@code PathFinder}
 * never attaches any (its DEBUG flag is a compiled-out {@code false}), so the vanilla format is
 * dead weight in a release jar.
 */
public record DebugPath(List<DebugNode> nodes, int nextNodeIndex, BlockPos target, boolean reached,
                        List<DebugNode> openSet, List<DebugNode> closedSet,
                        float envelopeMaxSpeed, float expectedFlightTicks) {

    public static DebugPath of(Path path, @Nullable ThrottleProfile throttle, double envelopeMaxSpeed) {
        List<DebugNode> nodes = new ArrayList<>(path.getNodeCount());
        for (int i = 0; i < path.getNodeCount(); i++) {
            // the profile is built from the path, so the indices line up, but a path replaced under
            // a stale profile would not and the renderer should degrade rather than throw
            float limit = throttle != null && i < throttle.nodeCount()
                    ? (float) throttle.limitAtNode(i) : DebugNode.UNKNOWN_SPEED;
            nodes.add(DebugNode.of(path.getNode(i), limit));
        }
        // only present if the finder was asked to record it, see BirdPathfindingConfig#collectDebugData
        Path.DebugData searchData = path.debugData();
        return new DebugPath(nodes, path.getNextNodeIndex(), path.getTarget(), path.canReach(),
                searchData == null ? List.of() : convert(searchData.openSet()),
                searchData == null ? List.of() : convert(searchData.closedSet()),
                (float) envelopeMaxSpeed,
                throttle == null ? 0.0F : (float) throttle.expectedFlightTicks());
    }

    private static List<DebugNode> convert(Node[] nodes) {
        return Arrays.stream(nodes).map(DebugNode::of).toList();
    }

    public static DebugPath read(FriendlyByteBuf buf) {
        return new DebugPath(buf.readList(DebugNode::read), buf.readVarInt(), buf.readBlockPos(),
                buf.readBoolean(), buf.readList(DebugNode::read), buf.readList(DebugNode::read),
                buf.readFloat(), buf.readFloat());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeCollection(this.nodes, (b, node) -> node.write(b));
        buf.writeVarInt(this.nextNodeIndex);
        buf.writeBlockPos(this.target);
        buf.writeBoolean(this.reached);
        buf.writeCollection(this.openSet, (b, node) -> node.write(b));
        buf.writeCollection(this.closedSet, (b, node) -> node.write(b));
        buf.writeFloat(this.envelopeMaxSpeed);
        buf.writeFloat(this.expectedFlightTicks);
    }
}
