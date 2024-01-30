package net.mehvahdjukaar.sleep_tight.common.network;

import net.mehvahdjukaar.moonlight.api.platform.network.ChannelHandler;
import net.mehvahdjukaar.moonlight.api.platform.network.Message;
import net.mehvahdjukaar.moonlight.api.util.Utils;
import net.mehvahdjukaar.sleep_tight.SleepTight;
import net.mehvahdjukaar.sleep_tight.common.entities.BedEntity;
import net.minecraft.advancements.Advancement;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;

public class ServerBoundFallFromHammockMessage implements Message {

    public ServerBoundFallFromHammockMessage(FriendlyByteBuf buf){

    }

    public ServerBoundFallFromHammockMessage(){

    }

    @Override
    public void writeToBuffer(FriendlyByteBuf buf) {

    }

    @Override
    public void handle(ChannelHandler.Context context) {
        Player p = context.getSender();
        if(p.getVehicle() instanceof BedEntity){
            p.stopRiding();
            p.hurt(p.level().damageSources().fall(), 1);
            if(p instanceof ServerPlayer player) {
                Utils.awardAdvancement(player, SleepTight.res( "husbandry/hammock"));
            }
        }
    }
}
