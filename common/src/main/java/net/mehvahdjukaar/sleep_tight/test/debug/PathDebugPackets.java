package net.mehvahdjukaar.sleep_tight.test.debug;

import net.mehvahdjukaar.moonlight.api.platform.network.NetworkHelper;
import net.mehvahdjukaar.sleep_tight.test.BirdTestMob;
import net.mehvahdjukaar.sleep_tight.test.controller.BirdFlightConfig;
import net.mehvahdjukaar.sleep_tight.test.controller.BirdMoveControl;
import net.mehvahdjukaar.sleep_tight.test.navigator.BirdPathNavigation;
import net.mehvahdjukaar.sleep_tight.test.throttle.FlightEnvelope;
import net.mehvahdjukaar.sleep_tight.test.throttle.ThrottleProfile;
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
        ThrottleProfile throttle = mob.getNavigation() instanceof BirdPathNavigation birdNavigation
                ? birdNavigation.getThrottleProfile() : null;
        FlightEnvelope envelope = FlightEnvelope.forMob(mob);
        MobDebugInfo mobInfo = buildMobDebugInfo(mob, throttle);
        DebugPath debugPath = DebugPath.of(path, throttle, envelope.maxSpeed());
        sendToAllPlayers(serverLevel,
                new ClientBoundPathDebugMessage(mob.getId(), debugPath, nodeHalfWidth, mobInfo));
    }

    /** Everything the renderer needs to show what the mob is doing right now, not just the plan. */
    private static MobDebugInfo buildMobDebugInfo(BirdTestMob mob, @Nullable ThrottleProfile throttle) {
        PathNavigation navigation = mob.getNavigation();
        MoveControl moveControl = mob.getMoveControl();

        String operation = moveControl instanceof BirdMoveControl birdMoveControl
                ? birdMoveControl.getOperationName() : "?";
        boolean steering = moveControl.hasWanted() && !navigation.isDone();
        Vec3 wantedPos = moveControl.hasWanted()
                ? new Vec3(moveControl.getWantedX(), moveControl.getWantedY(), moveControl.getWantedZ())
                : mob.position();

        double rulerCursor = 0.0;
        double rulerLength = 0.0;
        long timeoutTimer = 0L;
        double timeoutLimit = 0.0;
        int ticksSinceStuckCheck = 0;
        if (navigation instanceof BirdPathNavigation birdNavigation) {
            rulerCursor = birdNavigation.getRulerCursor();
            rulerLength = birdNavigation.getRulerLength();
            timeoutTimer = birdNavigation.getTimeoutTimer();
            timeoutLimit = birdNavigation.getTimeoutLimit();
            ticksSinceStuckCheck = birdNavigation.getTicksSinceStuckCheck();
        }

        Path currentPath = navigation.getPath();
        int nextNodeIndex = currentPath != null ? currentPath.getNextNodeIndex() : 0;
        int nodeCount = currentPath != null ? currentPath.getNodeCount() : 0;

        // the limit under the mob and the tightest one it is about to run into. The second is the
        // one that matters: drag is the only brake, so a corner has to be seen roughly a block out
        double speedLimitNow = throttle != null ? throttle.speedLimitAt(rulerCursor) : -1.0;
        double speedLimitAhead = throttle != null
                ? throttle.speedLimitOver(rulerCursor, BirdFlightConfig.lookahead) : -1.0;

        return new MobDebugInfo(navigation.isStuck(), navigation.isDone(), steering, operation,
                mob.position(), wantedPos, mob.getDeltaMovement(), mob.getYRot(),
                rulerCursor, rulerLength, nextNodeIndex, nodeCount,
                timeoutTimer, timeoutLimit, ticksSinceStuckCheck, speedLimitNow, speedLimitAhead);
    }

    private static void sendToAllPlayers(ServerLevel level, CustomPacketPayload message) {
        for (ServerPlayer player : level.players()) {
            NetworkHelper.sendToClientPlayer(player, message);
        }
    }
}
