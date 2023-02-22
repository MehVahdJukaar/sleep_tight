package net.mehvahdjukaar.sleep_tight.network;

import net.mehvahdjukaar.moonlight.api.platform.network.ChannelHandler;
import net.mehvahdjukaar.moonlight.api.platform.network.Message;
import net.mehvahdjukaar.sleep_tight.SleepTightClient;
import net.mehvahdjukaar.sleep_tight.SleepTightPlatformStuff;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.UUID;

public class ClientBoundSyncPlayerSleepCapMessage implements Message {
    @Nullable
    private final UUID id;
    private final long time;

    public ClientBoundSyncPlayerSleepCapMessage(FriendlyByteBuf buf) {
        if (buf.readBoolean()) this.id = buf.readUUID();
        else id = null;
        this.time = buf.readLong();
    }

    public ClientBoundSyncPlayerSleepCapMessage(Player player) {
        var c = SleepTightPlatformStuff.getPlayerBedCap(player);
        this.id = c.getHomeBed();
        this.time = c.getLastNightmareTimestamp();
    }

    @Override
    public void writeToBuffer(FriendlyByteBuf buf) {
        buf.writeBoolean(id != null);
        if (id != null) buf.writeUUID(id);
        buf.writeLong(time);
    }

    @Override
    public void handle(ChannelHandler.Context context) {
        Player p = SleepTightClient.getPlayer();
        var c = SleepTightPlatformStuff.getPlayerBedCap(p);
        c.acceptFromServer(this.id, this.time);
    }
}
