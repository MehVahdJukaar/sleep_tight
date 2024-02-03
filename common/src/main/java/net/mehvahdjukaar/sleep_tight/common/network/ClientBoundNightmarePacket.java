package net.mehvahdjukaar.sleep_tight.common.network;

import net.mehvahdjukaar.moonlight.api.platform.network.Message;
import net.mehvahdjukaar.moonlight.api.platform.network.NetworkHelper;
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
    public void write(FriendlyByteBuf buf) {
    }

    @Override
    public void handle(NetworkHelper.Context context) {
        Player player = SleepTightClient.getPlayer();
        player.playNotifySound(SleepTight.NIGHTMARE_SOUND.get(), SoundSource.PLAYERS, 1, 1);
    }

}
