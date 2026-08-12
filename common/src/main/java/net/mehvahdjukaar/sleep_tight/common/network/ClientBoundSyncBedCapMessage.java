package net.mehvahdjukaar.sleep_tight.common.network;

import net.mehvahdjukaar.moonlight.api.platform.network.ChannelHandler;
import net.mehvahdjukaar.moonlight.api.platform.network.Message;
import net.mehvahdjukaar.sleep_tight.STPlatStuff;
import net.mehvahdjukaar.sleep_tight.SleepTightClient;
import net.mehvahdjukaar.sleep_tight.core.BedData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ClientBoundSyncBedCapMessage implements Message {

    private final BlockPos pos;
    private final UUID id;
    private final boolean hasBedBug;
    //this packet is broadcast to everyone around the bed, so we send all levels and let each client pick its own
    private final Map<UUID, Byte> bedLevels;

    public ClientBoundSyncBedCapMessage(FriendlyByteBuf buf) {
        this.id = buf.readUUID();
        this.hasBedBug = buf.readBoolean();
        this.pos = buf.readBlockPos();
        this.bedLevels = buf.readMap(FriendlyByteBuf::readUUID, FriendlyByteBuf::readByte);
    }

    public ClientBoundSyncBedCapMessage(BlockPos pos, BedData data) {
        this.id = data.getId();
        this.hasBedBug = data.isInfested();
        this.pos = pos;
        //getBedLevels is a live view, copy it so encoding can't race a level up
        this.bedLevels = new HashMap<>(data.getBedLevels());
    }

    @Override
    public void writeToBuffer(FriendlyByteBuf buf) {
        buf.writeUUID(this.id);
        buf.writeBoolean(this.hasBedBug);
        buf.writeBlockPos(pos);
        buf.writeMap(this.bedLevels, FriendlyByteBuf::writeUUID, (b, level) -> b.writeByte(level));
    }

    @Override
    public void handle(ChannelHandler.Context context) {
        Player p = SleepTightClient.getPlayer();
        if (p == null) {
            return;
        }
        BedData data = STPlatStuff.getBedDataIfPresent(p.level(), this.pos);
        if (data != null) {
            data.acceptFromServer(this.id, this.hasBedBug, this.bedLevels);
        }
    }
}
