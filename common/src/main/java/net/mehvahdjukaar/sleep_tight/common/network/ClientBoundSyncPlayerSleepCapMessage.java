package net.mehvahdjukaar.sleep_tight.common.network;

import net.mehvahdjukaar.moonlight.api.platform.network.ChannelHandler;
import net.mehvahdjukaar.moonlight.api.platform.network.Message;
import net.mehvahdjukaar.sleep_tight.STPlatStuff;
import net.mehvahdjukaar.sleep_tight.SleepTight;
import net.mehvahdjukaar.sleep_tight.SleepTightClient;
import net.mehvahdjukaar.sleep_tight.core.PlayerSleepData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class ClientBoundSyncPlayerSleepCapMessage implements Message {
    @Nullable
    private final UUID lastBedSleptInto;
    private final long insomniaWillElapseTimestamp;
    private final long lastWokenUpTimestamp;
    private final int consecutiveNights;
    private final int homeBedNights;
    private final boolean doubleBed;

    public ClientBoundSyncPlayerSleepCapMessage(FriendlyByteBuf buf) {
        if (buf.readBoolean()) this.lastBedSleptInto = buf.readUUID();
        else lastBedSleptInto = null;
        this.insomniaWillElapseTimestamp = buf.readLong();
        this.lastWokenUpTimestamp = buf.readLong();
        this.consecutiveNights = buf.readInt();
        this.homeBedNights = buf.readInt();
        this.doubleBed = buf.readBoolean();
    }

    public ClientBoundSyncPlayerSleepCapMessage(PlayerSleepData c) {
        this.lastBedSleptInto = c.getLastBedSleptInto();
        this.insomniaWillElapseTimestamp = c.getInsomniaWillElapseTime();
        this.lastWokenUpTimestamp = c.getLastWokenUpTime();
        this.consecutiveNights = c.getConsecutiveNightsSlept();
        this.homeBedNights = c.getNightsSleptInHomeBed();
        this.doubleBed = c.usingDoubleBed();
    }

    public ClientBoundSyncPlayerSleepCapMessage(Player player) {
        this(STPlatStuff.getPlayerSleepData(player));
    }

    @Override
    public void writeToBuffer(FriendlyByteBuf buf) {
        buf.writeBoolean(lastBedSleptInto != null);
        if (lastBedSleptInto != null) buf.writeUUID(lastBedSleptInto);
        buf.writeLong(insomniaWillElapseTimestamp);
        buf.writeLong(lastWokenUpTimestamp);
        buf.writeInt(consecutiveNights);
        buf.writeInt(homeBedNights);
        buf.writeBoolean(doubleBed);
    }

    @Override
    public void handle(ChannelHandler.Context context) {
        Player p = SleepTightClient.getPlayer();
        if (p == null) {
            SleepTight.LOGGER.error("Failed to find local player!");
            return;
        }
        PlayerSleepData data = STPlatStuff.getPlayerSleepData(p);
        data.acceptFromServer(this.lastBedSleptInto, this.insomniaWillElapseTimestamp, this.lastWokenUpTimestamp, this.consecutiveNights,
                this.homeBedNights, this.doubleBed);
    }
}
