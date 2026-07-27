package net.mehvahdjukaar.sleep_tight.common.network;

import net.mehvahdjukaar.moonlight.api.platform.network.NetworkHelper;
import net.mehvahdjukaar.sleep_tight.test.debug.ClientBoundPathDebugMessage;

public class ModNetworking {

    public static void init() {
        NetworkHelper.addNetworkRegistration(ModNetworking::registerMessages, 1);
    }

    private static void registerMessages(NetworkHelper.RegisterMessagesEvent event) {
        event.registerServerBound(ServerBoundCommitSleepMessage.TYPE);
        event.registerServerBound(ServerBoundFallFromHammockMessage.TYPE);
        event.registerBidirectional(AccelerateHammockMessage.TYPE);
        event.registerClientBound(ClientBoundParticleMessage.TYPE);
        event.registerClientBound(ClientBoundAlightCameraOnLayMessage.TYPE);
        event.registerClientBound(ClientBoundSleepImmediatelyMessage.TYPE);
        event.registerClientBound(ClientBoundNightmarePacket.TYPE);
        event.registerClientBound(ClientBoundPathDebugMessage.TYPE);
    }

}
