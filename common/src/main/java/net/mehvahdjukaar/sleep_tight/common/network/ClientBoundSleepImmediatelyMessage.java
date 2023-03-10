package net.mehvahdjukaar.sleep_tight.common.network;

import net.mehvahdjukaar.moonlight.api.platform.network.ChannelHandler;
import net.mehvahdjukaar.moonlight.api.platform.network.Message;
import net.mehvahdjukaar.sleep_tight.SleepTightClient;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

public class ClientBoundSleepImmediatelyMessage implements Message {

    private final BlockPos pos;

    public ClientBoundSleepImmediatelyMessage(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
    }

    public ClientBoundSleepImmediatelyMessage(BlockPos pos) {
        this.pos = pos;
    }

    @Override
    public void writeToBuffer(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
    }

    @Override
    public void handle(ChannelHandler.Context context) {
        SleepTightClient.getPlayer().startSleeping(pos);
    }


}
