package net.mehvahdjukaar.sleep_tight.network;

import net.mehvahdjukaar.moonlight.api.platform.network.ChannelHandler;
import net.mehvahdjukaar.moonlight.api.platform.network.Message;
import net.mehvahdjukaar.sleep_tight.common.BedEntity;
import net.minecraft.network.FriendlyByteBuf;

public class ServerBoundCommitSleepMessage implements Message {

    public ServerBoundCommitSleepMessage(FriendlyByteBuf buf){

    }

    public ServerBoundCommitSleepMessage(){

    }

    @Override
    public void writeToBuffer(FriendlyByteBuf buf) {

    }

    @Override
    public void handle(ChannelHandler.Context context) {
        if(context.getSender().getVehicle() instanceof BedEntity bed){
            bed.startSleepingOn(context.getSender());
        }
    }
}
