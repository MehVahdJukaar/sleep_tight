package net.mehvahdjukaar.sleep_tight.common.network;

import net.mehvahdjukaar.moonlight.api.platform.network.Message;
import net.mehvahdjukaar.moonlight.api.platform.network.NetworkHelper;
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
    public void write(FriendlyByteBuf buf) {
        buf.writeFloat(yRot);
    }

    @Override
    public void handle(NetworkHelper.Context context) {
        Player player = SleepTightClient.getPlayer();
        BedEntity.alignCamera(player, yRot);
    }


}
