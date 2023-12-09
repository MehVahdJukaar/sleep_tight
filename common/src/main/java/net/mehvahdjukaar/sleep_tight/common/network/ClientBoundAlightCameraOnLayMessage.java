package net.mehvahdjukaar.sleep_tight.common.network;

import net.mehvahdjukaar.moonlight.api.platform.network.ChannelHandler;
import net.mehvahdjukaar.moonlight.api.platform.network.Message;
import net.mehvahdjukaar.sleep_tight.SleepTightClient;
import net.mehvahdjukaar.sleep_tight.common.entities.BedEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;

public class ClientBoundAlightCameraOnLayMessage implements Message {
    private final float yRot;

    public ClientBoundAlightCameraOnLayMessage(FriendlyByteBuf buf) {
        this.yRot = buf.readFloat();
    }

    public ClientBoundAlightCameraOnLayMessage(BedEntity entity) {
        this.yRot = entity.getYRot();
    }

    @Override
    public void writeToBuffer(FriendlyByteBuf buf) {
        buf.writeFloat(yRot);
    }

    @Override
    public void handle(ChannelHandler.Context context) {
        Player player = SleepTightClient.getPlayer();
        BedEntity.alignCamera(player, yRot);
    }


}
