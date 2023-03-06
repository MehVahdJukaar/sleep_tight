package net.mehvahdjukaar.sleep_tight.integration.network;

import net.mehvahdjukaar.moonlight.api.platform.network.ChannelHandler;
import net.mehvahdjukaar.moonlight.api.platform.network.Message;
import net.mehvahdjukaar.sleep_tight.SleepTightClient;
import net.mehvahdjukaar.sleep_tight.client.ClientEvents;
import net.mehvahdjukaar.sleep_tight.common.BedEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class ClientBoundRideImmediatelyMessage implements Message {
    private final int id;

    public ClientBoundRideImmediatelyMessage(FriendlyByteBuf buf) {
        this.id = buf.readInt();
    }

    public ClientBoundRideImmediatelyMessage(Entity entity) {
        this.id = entity.getId();
    }

    @Override
    public void writeToBuffer(FriendlyByteBuf buf) {
        buf.writeInt(id);
    }

    @Override
    public void handle(ChannelHandler.Context context) {
        Player player = SleepTightClient.getPlayer();
        Level level = player.level;
        Entity entity = level.getEntity(id);
        if (entity != null) {
            player.startRiding(entity);
            player.setYRot(entity.getYRot());
            player.setYHeadRot(entity.getYRot());
            player.yRotO = player.getYRot();
            player.yHeadRotO = player.yHeadRot;
        }
        if(entity instanceof BedEntity bed){
           ClientEvents. displayRidingMessage(bed);
        }
    }


}
