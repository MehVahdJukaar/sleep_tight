package net.mehvahdjukaar.sleep_tight.common.network;

import net.mehvahdjukaar.moonlight.api.platform.network.Message;
import net.mehvahdjukaar.moonlight.api.platform.network.NetworkHelper;
import net.mehvahdjukaar.sleep_tight.common.entities.BedEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public class ServerBoundCommitSleepMessage implements Message {

    public ServerBoundCommitSleepMessage(FriendlyByteBuf buf) {

    }

    public ServerBoundCommitSleepMessage() {

    }

    @Override
    public void write(FriendlyByteBuf buf) {

    }

    @Override
    public void handle(NetworkHelper.Context context) {
        if (context.getSender().getVehicle() instanceof BedEntity bed) {
            bed.startSleepingOn((ServerPlayer) context.getSender());
        }
    }
}
