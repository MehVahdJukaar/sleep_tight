package net.mehvahdjukaar.sleep_tight.common.network;

import net.mehvahdjukaar.moonlight.api.platform.network.Message;
import net.mehvahdjukaar.sleep_tight.STPlatStuff;
import net.mehvahdjukaar.sleep_tight.SleepTightClient;
import net.mehvahdjukaar.sleep_tight.core.BedData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public class ClientBoundSyncBedCapMessage implements Message {

    private final  BlockPos pos;
    private final UUID id;
    private final boolean hasBedBug;

    public ClientBoundSyncBedCapMessage(FriendlyByteBuf buf) {
        this.id = buf.readUUID();
        this.hasBedBug = buf.readBoolean();
        this.pos = buf.readBlockPos();
    }

    public ClientBoundSyncBedCapMessage(BlockPos pos,BedData data) {
        this.id = data.getId();
        this.hasBedBug = data.isInfested();
        this.pos = pos;
    }

    @Override
    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(this.id);
        buf.writeBoolean(this.hasBedBug);
        buf.writeBlockPos(pos);
    }

    @Override
    public void handle(Context context) {
        Player p = SleepTightClient.getPlayer();
        if (p == null) {
            return;
        }
        BedData data = STPlatStuff.getBedData(p.level(), this.pos);
        if (data != null) {
            data.acceptFromServer(this.id, this.hasBedBug);
        }
    }
}
