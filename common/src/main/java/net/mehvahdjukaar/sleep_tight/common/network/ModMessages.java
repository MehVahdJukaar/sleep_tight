package net.mehvahdjukaar.sleep_tight.common.network;

import net.mehvahdjukaar.moonlight.api.platform.network.NetworkDir;
import net.mehvahdjukaar.moonlight.api.platform.network.NetworkHelper;
import net.mehvahdjukaar.sleep_tight.SleepTight;

public class ModMessages {

    public static void init() {
        NetworkHelper.addRegistration(SleepTight.MOD_ID, event -> event
                .register(NetworkDir.SERVERBOUND, ServerBoundCommitSleepMessage.class, ServerBoundCommitSleepMessage::new)
                .register(NetworkDir.SERVERBOUND, ServerBoundFallFromHammockMessage.class, ServerBoundFallFromHammockMessage::new)
                .register(NetworkDir.BOTH, AccelerateHammockMessage.class, AccelerateHammockMessage::new)
                .register(NetworkDir.CLIENTBOUND, ClientBoundSyncPlayerSleepCapMessage.class, ClientBoundSyncPlayerSleepCapMessage::new)
                .register(NetworkDir.CLIENTBOUND, ClientBoundParticleMessage.class, ClientBoundParticleMessage::new)
                .register(NetworkDir.CLIENTBOUND, ClientBoundAlightCameraOnLayMessage.class, ClientBoundAlightCameraOnLayMessage::new)
                .register(NetworkDir.CLIENTBOUND, ClientBoundSleepImmediatelyMessage.class, ClientBoundSleepImmediatelyMessage::new)
                .register(NetworkDir.CLIENTBOUND, ClientBoundNightmarePacket.class, ClientBoundNightmarePacket::new));
    }

}
