package net.mehvahdjukaar.sleep_tight.common.network;

import net.mehvahdjukaar.moonlight.api.platform.network.ChannelHandler;
import net.mehvahdjukaar.moonlight.api.platform.network.NetworkDir;
import net.mehvahdjukaar.sleep_tight.SleepTight;

public class NetworkHandler {
    public static final ChannelHandler CHANNEL = ChannelHandler.createChannel(SleepTight.res("network"));

    public static void registerMessages() {
        CHANNEL.register(NetworkDir.PLAY_TO_SERVER, ServerBoundCommitSleepMessage.class, ServerBoundCommitSleepMessage::new);
        CHANNEL.register(NetworkDir.PLAY_TO_SERVER, ServerBoundFallFromHammockMessage.class, ServerBoundFallFromHammockMessage::new);
        CHANNEL.register(NetworkDir.BOTH, AccelerateHammockMessage.class, AccelerateHammockMessage::new);
        CHANNEL.register(NetworkDir.PLAY_TO_CLIENT, ClientBoundSyncPlayerSleepCapMessage.class, ClientBoundSyncPlayerSleepCapMessage::new);
        CHANNEL.register(NetworkDir.PLAY_TO_CLIENT, ClientBoundParticleMessage.class, ClientBoundParticleMessage::new);
        CHANNEL.register(NetworkDir.PLAY_TO_CLIENT, ClientBoundRideImmediatelyMessage.class, ClientBoundRideImmediatelyMessage::new);
        CHANNEL.register(NetworkDir.PLAY_TO_CLIENT, ClientBoundSleepImmediatelyMessage.class, ClientBoundSleepImmediatelyMessage::new);
        CHANNEL.register(NetworkDir.PLAY_TO_CLIENT, ClientBoundNightmarePacket.class, ClientBoundNightmarePacket::new);
    }

}
