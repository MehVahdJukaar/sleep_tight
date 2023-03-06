package net.mehvahdjukaar.sleep_tight.integration.network;

import net.mehvahdjukaar.moonlight.api.platform.network.ChannelHandler;
import net.mehvahdjukaar.moonlight.api.platform.network.Message;
import net.mehvahdjukaar.sleep_tight.SleepTightClient;
import net.mehvahdjukaar.sleep_tight.client.ClientEvents;
import net.mehvahdjukaar.sleep_tight.common.BedEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

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
