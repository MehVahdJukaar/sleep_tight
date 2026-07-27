package net.mehvahdjukaar.sleep_tight.test.debug;

import net.mehvahdjukaar.moonlight.api.platform.network.NetworkHelper;
import net.mehvahdjukaar.sleep_tight.test.BirdTestMob;
import net.mehvahdjukaar.sleep_tight.test.controller.BirdMoveControl;
import net.mehvahdjukaar.sleep_tight.test.navigator.BirdPathNavigation;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Our side of the debug channel, standing in for vanilla's DebugPackets (whose bodies are stripped
 * to empty stubs in the release jar).
 * <p>
 * Deliberately typed to {@link BirdTestMob}: this exists to watch the lattice pathfinder, not to draw
 * paths for every mob on the server.
 */
public class PathDebugPackets {

    public static void sendPathFindingPacket(BirdTestMob mob, @Nullable Path path, float nodeHalfWidth) {
        if (path == null || !(mob.level() instanceof ServerLevel serverLevel)) return;
        MobDebugInfo mobInfo = buildMobDebugInfo(mob);
        sendToAllPlayers(serverLevel,
                new ClientBoundPathDebugMessage(mob.getId(), DebugPath.of(path), nodeHalfWidth, mobInfo));
    }

    /** Everything the renderer needs to show what the mob is doing right now, not just the plan. */
    private static MobDebugInfo buildMobDebugInfo(BirdTestMob mob) {
        PathNavigation navigation = mob.getNavigation();
        MoveControl moveControl = mob.getMoveControl();

        String operation = moveControl instanceof BirdMoveControl birdMoveControl
                ? birdMoveControl.getOperationName() : "?";
        Vec3 wantedPos = moveControl.hasWanted()
                ? new Vec3(moveControl.getWantedX(), moveControl.getWantedY(), moveControl.getWantedZ())
                : mob.position();

        double rulerCursor = 0.0;
        double rulerLength = 0.0;
        if (navigation instanceof BirdPathNavigation birdNavigation) {
            rulerCursor = birdNavigation.getRulerCursor();
            rulerLength = birdNavigation.getRulerLength();
        }

        Path currentPath = navigation.getPath();
        int nextNodeIndex = currentPath != null ? currentPath.getNextNodeIndex() : 0;
        int nodeCount = currentPath != null ? currentPath.getNodeCount() : 0;

        return new MobDebugInfo(navigation.isStuck(), navigation.isDone(), operation, wantedPos,
                mob.getDeltaMovement(), rulerCursor, rulerLength, nextNodeIndex, nodeCount);
    }

    private static void sendToAllPlayers(ServerLevel level, CustomPacketPayload message) {
        for (ServerPlayer player : level.players()) {
            NetworkHelper.sendToClientPlayer(player, message);
        }
    }
}
