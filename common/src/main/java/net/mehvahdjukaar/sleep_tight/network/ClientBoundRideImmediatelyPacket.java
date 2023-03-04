package net.mehvahdjukaar.sleep_tight.network;

import net.mehvahdjukaar.moonlight.api.platform.network.ChannelHandler;
import net.mehvahdjukaar.moonlight.api.platform.network.Message;
import net.mehvahdjukaar.sleep_tight.SleepTightClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class ClientBoundRideImmediatelyPacket implements Message {
    private final int id;

    public ClientBoundRideImmediatelyPacket(FriendlyByteBuf buf) {
        this.id = buf.readInt();
    }

    public ClientBoundRideImmediatelyPacket(Entity entity) {
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
        player.startRiding(entity);
        player.setYRot(entity.getYRot());
        player.setYHeadRot(entity.getYRot());
        player.yRotO = player.getYRot();
        player.yHeadRotO = player.yHeadRot;
    }


}
