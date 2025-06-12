package net.mehvahdjukaar.sleep_tight.common.network;

import net.mehvahdjukaar.moonlight.api.platform.network.Message;
import net.mehvahdjukaar.sleep_tight.SleepTight;
import net.mehvahdjukaar.sleep_tight.SleepTightClient;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;

public class ClientBoundSleepImmediatelyMessage implements Message {

    public static final TypeAndCodec<RegistryFriendlyByteBuf, ClientBoundSleepImmediatelyMessage> TYPE = Message.makeType(
            SleepTight.res("sleep_immediately"),
            ClientBoundSleepImmediatelyMessage::new
    );

    private final BlockPos pos;

    public ClientBoundSleepImmediatelyMessage(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
    }

    public ClientBoundSleepImmediatelyMessage(BlockPos pos) {
        this.pos = pos;
    }

    @Override
    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
    }

    @Override
    public void handle(Context context) {
        SleepTightClient.getPlayer().startSleeping(pos);
    }

    @Override
    public Type<?> type() {
        return TYPE.type();
    }

}
