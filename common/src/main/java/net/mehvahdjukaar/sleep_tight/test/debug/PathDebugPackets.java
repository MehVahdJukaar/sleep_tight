package net.mehvahdjukaar.sleep_tight.test.debug;

import net.mehvahdjukaar.moonlight.api.platform.network.NetworkHelper;
import net.mehvahdjukaar.sleep_tight.test.BirdTestMob;
import net.mehvahdjukaar.sleep_tight.test.controller.BirdFlightControl;
import net.mehvahdjukaar.sleep_tight.test.navigator.BirdFlightNavigation;
import net.mehvahdjukaar.sleep_tight.test.pathfinding.BirdPathFinder.ConsideredMove;
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

import java.util.List;

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
        boolean bird = mob.getNavigation() instanceof BirdFlightNavigation;
        ThrottleProfile throttle = bird
                ? ((BirdFlightNavigation) mob.getNavigation()).getThrottleProfile() : null;
        FlightEnvelope envelope = FlightEnvelope.forMob(mob);
        MobDebugInfo mobInfo = buildMobDebugInfo(mob);
        DebugPath debugPath = DebugPath.of(path, throttle, envelope.maxSpeed());
        sendToAllPlayers(serverLevel,
                new ClientBoundPathDebugMessage(mob.getId(), debugPath, nodeHalfWidth, mobInfo));
    }

    /** Everything the renderer needs to show what the mob is doing right now, not just the plan. */
    private static MobDebugInfo buildMobDebugInfo(BirdTestMob mob) {
        PathNavigation navigation = mob.getNavigation();
        MoveControl moveControl = mob.getMoveControl();

        String operation = moveControl instanceof BirdFlightControl birdMoveControl
                ? birdMoveControl.getOperationName() : "?";
        boolean steering = moveControl.hasWanted() && !navigation.isDone() && !mob.isHoldingForLaunch();
        Vec3 wantedPos = moveControl.hasWanted()
                ? new Vec3(moveControl.getWantedX(), moveControl.getWantedY(), moveControl.getWantedZ())
                : mob.position();

        double rulerCursor = 0.0;
        double rulerLength = 0.0;
        double offRoute = 0.0;
        long timeoutTimer = 0L;
        double timeoutLimit = 0.0;
        int ticksSinceStuckCheck = 0;
        // the profile's limit under the mob, and what the navigation actually commanded there. They
        // differ when the mob is cutting a corner (less braking authority than the profile assumed)
        // or has been shoved clear of the line, so the gap between them is the correction working
        double speedLimitNow = -1.0;
        double speedLimitCommanded = -1.0;
        if (navigation instanceof BirdFlightNavigation birdNavigation) {
            rulerCursor = birdNavigation.getRulerCursor();
            rulerLength = birdNavigation.getRulerLength();
            offRoute = birdNavigation.getOffRoute();
            timeoutTimer = birdNavigation.getTimeoutTimer();
            timeoutLimit = birdNavigation.getTimeoutLimit();
            ticksSinceStuckCheck = birdNavigation.getTicksSinceStuckCheck();
            speedLimitNow = orUnknown(birdNavigation.getProfiledSpeedLimit());
            speedLimitCommanded = orUnknown(birdNavigation.getSpeedLimit());
        }

        Path currentPath = navigation.getPath();
        int nextNodeIndex = currentPath != null ? currentPath.getNextNodeIndex() : 0;
        int nodeCount = currentPath != null ? currentPath.getNodeCount() : 0;

        return new MobDebugInfo(navigation.isStuck(), navigation.isDone(), steering, operation,
                mob.getModeName(), mob.getLaunchYaw(),
                mob.position(), wantedPos, mob.getDeltaMovement(), mob.getYRot(),
                rulerCursor, rulerLength, offRoute, nextNodeIndex, nodeCount,
                timeoutTimer, timeoutLimit, ticksSinceStuckCheck, speedLimitNow, speedLimitCommanded,
                mob.getDebugTrail().epoch(), mob.getDebugTrail().drainPending());
    }

    /** The navigation reports an unbounded limit when there is no profile; the renderer wants -1. */
    private static double orUnknown(double speedLimit) {
        return speedLimit == Double.MAX_VALUE ? -1.0 : speedLimit;
    }

    private static void sendToAllPlayers(ServerLevel level, CustomPacketPayload message) {
        for (ServerPlayer player : level.players()) {
            NetworkHelper.sendToClientPlayer(player, message);
        }
    }
}
