package net.mehvahdjukaar.sleep_tight.common.network;

import net.mehvahdjukaar.moonlight.api.platform.network.ChannelHandler;
import net.mehvahdjukaar.moonlight.api.platform.network.Message;
import net.mehvahdjukaar.sleep_tight.SleepTight;
import net.mehvahdjukaar.sleep_tight.SleepTightClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

public class ClientBoundNightmarePacket implements Message {

    public ClientBoundNightmarePacket(FriendlyByteBuf buf) {
    }

    public ClientBoundNightmarePacket() {
    }

    @Override
    public void writeToBuffer(FriendlyByteBuf buf) {
    }

    @Override
    public void handle(ChannelHandler.Context context) {
        Player player = SleepTightClient.getPlayer();
        player.playNotifySound(SleepTight.NIGHTMARE_SOUND.get(), SoundSource.PLAYERS, 1, 1);
    }

}
