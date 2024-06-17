package net.mehvahdjukaar.sleep_tight.common.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.mehvahdjukaar.moonlight.api.platform.network.ChannelHandler;
import net.mehvahdjukaar.moonlight.api.platform.network.Message;
import net.mehvahdjukaar.sleep_tight.SleepTight;
import net.mehvahdjukaar.sleep_tight.SleepTightClient;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.BiomeAmbientSoundsHandler;
import net.minecraft.network.FriendlyByteBuf;
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
        player.playSound(SleepTight.NIGHTMARE_SOUND.get(), 1, 1);

        clientStuff(player);
    }

    @Environment(EnvType.CLIENT)
    public void clientStuff(Player player) {
        for (var s : ((LocalPlayer) player).ambientSoundHandlers) {
            if (s instanceof BiomeAmbientSoundsHandler ba) {
                if (ba.moodiness < 0.9) {
                    //max out moodiness
                    ba.moodiness = 0.9f;
                }
            }
        }
    }

}
