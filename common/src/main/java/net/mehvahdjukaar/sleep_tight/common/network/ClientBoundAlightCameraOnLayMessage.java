package net.mehvahdjukaar.sleep_tight.common.network;

import net.mehvahdjukaar.moonlight.api.platform.network.Message;
import net.mehvahdjukaar.sleep_tight.SleepTight;
import net.mehvahdjukaar.sleep_tight.SleepTightClient;
import net.mehvahdjukaar.sleep_tight.common.entities.BedEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Player;

public class ClientBoundAlightCameraOnLayMessage implements Message {
    public static final TypeAndCodec<RegistryFriendlyByteBuf, ClientBoundAlightCameraOnLayMessage> TYPE = Message.makeType(
            SleepTight.res("alight_camera_on_lay"),
            ClientBoundAlightCameraOnLayMessage::new
    );
    private final float yRot;

    public ClientBoundAlightCameraOnLayMessage(FriendlyByteBuf buf) {
        this.yRot = buf.readFloat();
    }

    public ClientBoundAlightCameraOnLayMessage(BedEntity entity) {
        this.yRot = entity.getYRot();
    }

    @Override
    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeFloat(yRot);
    }

    @Override
    public void handle(Context context) {
        Player player = SleepTightClient.getPlayer();
        BedEntity.alignCamera(player, yRot);
    }

    @Override
    public Type<?> type() {
        return TYPE.type();
    }


}
