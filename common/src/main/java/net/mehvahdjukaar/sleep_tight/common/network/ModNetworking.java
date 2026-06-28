package net.mehvahdjukaar.sleep_tight.common.network;

import net.mehvahdjukaar.moonlight.api.platform.network.ChannelHandler;
import net.mehvahdjukaar.moonlight.api.platform.network.NetworkDir;
import net.mehvahdjukaar.sleep_tight.SleepTight;

public class ModNetworking {

    public static final ChannelHandler CHANNEL = ChannelHandler.createChannel(SleepTight.MOD_ID, () -> 1);

    public static void init() {
        registerMessages();
    }

    private static void registerMessages() {
        CHANNEL.register(NetworkDir.PLAY_TO_SERVER, ServerBoundCommitSleepMessage.class, ServerBoundCommitSleepMessage::new);
        CHANNEL.register(NetworkDir.PLAY_TO_SERVER, ServerBoundFallFromHammockMessage.class, ServerBoundFallFromHammockMessage::new);
        CHANNEL.register(NetworkDir.BOTH, AccelerateHammockMessage.class, AccelerateHammockMessage::new);
        CHANNEL.register(NetworkDir.PLAY_TO_CLIENT, ClientBoundParticleMessage.class, ClientBoundParticleMessage::new);
        CHANNEL.register(NetworkDir.PLAY_TO_CLIENT, ClientBoundAlightCameraOnLayMessage.class, ClientBoundAlightCameraOnLayMessage::new);
        CHANNEL.register(NetworkDir.PLAY_TO_CLIENT, ClientBoundSleepImmediatelyMessage.class, ClientBoundSleepImmediatelyMessage::new);
        CHANNEL.register(NetworkDir.PLAY_TO_CLIENT, ClientBoundNightmarePacket.class, ClientBoundNightmarePacket::new);
        CHANNEL.register(NetworkDir.PLAY_TO_CLIENT, ClientBoundSyncPlayerSleepCapMessage.class, ClientBoundSyncPlayerSleepCapMessage::new);
        CHANNEL.register(NetworkDir.PLAY_TO_CLIENT, ClientBoundSyncBedCapMessage.class, ClientBoundSyncBedCapMessage::new);
    }

}
