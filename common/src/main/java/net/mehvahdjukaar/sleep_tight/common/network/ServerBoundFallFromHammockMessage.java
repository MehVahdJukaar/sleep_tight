package net.mehvahdjukaar.sleep_tight.common.network;

import net.mehvahdjukaar.moonlight.api.platform.PlatHelper;
import net.mehvahdjukaar.moonlight.api.platform.network.Message;
import net.mehvahdjukaar.moonlight.api.util.Utils;
import net.mehvahdjukaar.sleep_tight.SleepTight;
import net.mehvahdjukaar.sleep_tight.common.entities.BedEntity;
import net.minecraft.advancements.Advancement;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public class ServerBoundFallFromHammockMessage implements Message {

    public static final TypeAndCodec<RegistryFriendlyByteBuf, ServerBoundFallFromHammockMessage> TYPE = Message.makeType(
            SleepTight.res("fall_from_hammock"),
            ServerBoundFallFromHammockMessage::new
    );

    public ServerBoundFallFromHammockMessage(RegistryFriendlyByteBuf buf) {

    }

    public ServerBoundFallFromHammockMessage() {

    }

    @Override
    public void write(RegistryFriendlyByteBuf buf) {

    }

    @Override
    public void handle(Context context) {
        Player p = context.getPlayer();
        if (p.getVehicle() instanceof BedEntity) {
            p.stopRiding();
            p.hurt(p.level().damageSources().fall(), 1);

            if (p instanceof ServerPlayer player) {
                Utils.awardAdvancement(player, SleepTight.res("husbandry/hammock"));
            }
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE.type();
    }
}
