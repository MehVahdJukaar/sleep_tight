package net.mehvahdjukaar.sleep_tight.test.debug;

import net.mehvahdjukaar.moonlight.api.platform.network.NetworkHelper;
import net.mehvahdjukaar.sleep_tight.test.BirdTestMob;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.pathfinder.Path;
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
        sendToAllPlayers(serverLevel, new ClientBoundPathDebugMessage(mob.getId(), DebugPath.of(path), nodeHalfWidth));
    }

    private static void sendToAllPlayers(ServerLevel level, CustomPacketPayload message) {
        for (ServerPlayer player : level.players()) {
            NetworkHelper.sendToClientPlayer(player, message);
        }
    }
}
