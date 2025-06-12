package net.mehvahdjukaar.sleep_tight.common.network;

import net.mehvahdjukaar.moonlight.api.platform.network.Message;
import net.mehvahdjukaar.sleep_tight.SleepTight;
import net.mehvahdjukaar.sleep_tight.SleepTightClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

public class ClientBoundNightmarePacket implements Message {

    public static final TypeAndCodec<RegistryFriendlyByteBuf, ClientBoundNightmarePacket> TYPE = Message.makeType(
            SleepTight.res("nightmare"),
            ClientBoundNightmarePacket::new
    );


    public ClientBoundNightmarePacket(FriendlyByteBuf buf) {
    }

    public ClientBoundNightmarePacket() {
    }

    @Override
    public void write(RegistryFriendlyByteBuf buf) {
    }

    @Override
    public void handle(Context context) {
        Player player = SleepTightClient.getPlayer();
        player.playNotifySound(SleepTight.NIGHTMARE_SOUND.get(), SoundSource.PLAYERS, 1, 1);
    }

    @Override
    public Type<?> type() {
        return TYPE.type();
    }

}
