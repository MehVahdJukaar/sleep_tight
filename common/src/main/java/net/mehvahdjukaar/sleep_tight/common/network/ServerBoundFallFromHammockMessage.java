package net.mehvahdjukaar.sleep_tight.common.network;

import net.mehvahdjukaar.moonlight.api.platform.network.Message;
import net.mehvahdjukaar.moonlight.api.platform.network.NetworkHelper;
import net.mehvahdjukaar.moonlight.api.util.Utils;
import net.mehvahdjukaar.sleep_tight.SleepTight;
import net.mehvahdjukaar.sleep_tight.common.entities.BedEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public class ServerBoundFallFromHammockMessage implements Message {

    public ServerBoundFallFromHammockMessage(FriendlyByteBuf buf) {

    }

    public ServerBoundFallFromHammockMessage() {

    }

    @Override
    public void write(FriendlyByteBuf buf) {

    }

    @Override
    public void handle(NetworkHelper.Context context) {
        Player p = context.getSender();
        if (p.getVehicle() instanceof BedEntity) {
            p.stopRiding();
            p.hurt(p.level().damageSources().fall(), 1);
            if (p instanceof ServerPlayer player) {
                Utils.awardAdvancement(player, SleepTight.res("husbandry/hammock"));
            }
        }
    }
}
