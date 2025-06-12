package net.mehvahdjukaar.sleep_tight.common.network;

import net.mehvahdjukaar.moonlight.api.platform.network.Message;
import net.mehvahdjukaar.sleep_tight.SleepTight;
import net.mehvahdjukaar.sleep_tight.common.entities.BedEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public class ServerBoundCommitSleepMessage implements Message {

    public static final TypeAndCodec<RegistryFriendlyByteBuf, ServerBoundCommitSleepMessage> TYPE = Message.makeType(
            SleepTight.res("commit_sleep"),
            ServerBoundCommitSleepMessage::new
    );

    public ServerBoundCommitSleepMessage(FriendlyByteBuf buf) {

    }

    public ServerBoundCommitSleepMessage() {
    }

    @Override
    public void write(RegistryFriendlyByteBuf buf) {

    }

    @Override
    public void handle(Context context) {
        if (context.getPlayer().getVehicle() instanceof BedEntity bed) {
            bed.startSleepingOn((ServerPlayer) context.getPlayer());
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE.type();
    }
}
