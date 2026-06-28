package net.mehvahdjukaar.sleep_tight.common.network;

import net.mehvahdjukaar.moonlight.api.platform.network.ChannelHandler;
import net.mehvahdjukaar.moonlight.api.platform.network.Message;
import net.mehvahdjukaar.moonlight.api.util.Utils;
import net.mehvahdjukaar.sleep_tight.SleepTight;
import net.mehvahdjukaar.sleep_tight.common.entities.BedEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public class ServerBoundFallFromHammockMessage implements Message {

    public ServerBoundFallFromHammockMessage(FriendlyByteBuf buf) {

    }

    public ServerBoundFallFromHammockMessage() {

    }

    @Override
    public void writeToBuffer(FriendlyByteBuf buf) {

    }

    @Override
    public void handle(ChannelHandler.Context context) {
        ServerPlayer p = (ServerPlayer) context.getSender();
        if (p.getVehicle() instanceof BedEntity) {
            p.stopRiding();
            p.hurt(p.level().damageSources().fall(), 1);

            Utils.awardAdvancement(p, SleepTight.res("husbandry/hammock"));
        }
    }
}
