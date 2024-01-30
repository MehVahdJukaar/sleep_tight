package net.mehvahdjukaar.sleep_tight.common.network;

import net.mehvahdjukaar.moonlight.api.platform.network.ChannelHandler;
import net.mehvahdjukaar.moonlight.api.platform.network.NetworkDir;
import net.mehvahdjukaar.sleep_tight.SleepTight;

public class NetworkHandler {
    public static final ChannelHandler CHANNEL = ChannelHandler.builder(SleepTight.MOD_ID)
            .register(NetworkDir.PLAY_TO_SERVER, ServerBoundCommitSleepMessage.class, ServerBoundCommitSleepMessage::new)
            .register(NetworkDir.PLAY_TO_SERVER, ServerBoundFallFromHammockMessage.class, ServerBoundFallFromHammockMessage::new)
            .register(NetworkDir.BOTH, AccelerateHammockMessage.class, AccelerateHammockMessage::new)
            .register(NetworkDir.PLAY_TO_CLIENT, ClientBoundSyncPlayerSleepCapMessage.class, ClientBoundSyncPlayerSleepCapMessage::new)
            .register(NetworkDir.PLAY_TO_CLIENT, ClientBoundParticleMessage.class, ClientBoundParticleMessage::new)
            .register(NetworkDir.PLAY_TO_CLIENT, ClientBoundAlightCameraOnLayMessage.class, ClientBoundAlightCameraOnLayMessage::new)
            .register(NetworkDir.PLAY_TO_CLIENT, ClientBoundSleepImmediatelyMessage.class, ClientBoundSleepImmediatelyMessage::new)
            .register(NetworkDir.PLAY_TO_CLIENT, ClientBoundNightmarePacket.class, ClientBoundNightmarePacket::new)
            .build();

    public static void init() {
    }

}
