package net.mehvahdjukaar.sleep_tight.common.network;

import net.mehvahdjukaar.moonlight.api.platform.network.Message;
import net.mehvahdjukaar.moonlight.api.platform.network.NetworkHelper;
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
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
    }

    @Override
    public void handle(NetworkHelper.Context context) {
        SleepTightClient.getPlayer().startSleeping(pos);
    }


}
