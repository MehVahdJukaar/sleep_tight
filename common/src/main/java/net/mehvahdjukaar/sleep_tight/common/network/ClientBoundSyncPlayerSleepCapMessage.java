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
    private final long dayDeadline;
    private final long gameDeadline;
    private final long lastKnownDayTime;
    private final long lastWokenUpTimestamp;
    private final int consecutiveNights;
    private final int homeBedNights;
    private final boolean doubleBed;

    public ClientBoundSyncPlayerSleepCapMessage(FriendlyByteBuf buf) {
        if (buf.readBoolean()) this.lastBedSleptInto = buf.readUUID();
        else this.lastBedSleptInto = null;
        this.dayDeadline = buf.readLong();
        this.gameDeadline = buf.readLong();
        this.lastKnownDayTime = buf.readLong();
        this.lastWokenUpTimestamp = buf.readLong();
        this.consecutiveNights = buf.readInt();
        this.homeBedNights = buf.readInt();
        this.doubleBed = buf.readBoolean();
    }

    public ClientBoundSyncPlayerSleepCapMessage(PlayerSleepData c) {
        this.lastBedSleptInto = c.getLastBedSleptInto();
        this.dayDeadline = c.getInsomnia().dayDeadline();
        this.gameDeadline = c.getInsomnia().gameDeadline();
        this.lastKnownDayTime = c.getInsomnia().lastKnownDayTime();
        this.lastWokenUpTimestamp = c.getLastWokenUpTime();
        this.consecutiveNights = c.getConsecutiveNightsSlept();
        this.homeBedNights = c.getNightsSleptInHomeBed();
        this.doubleBed = c.usingDoubleBed();
    }

    @Override
    public void writeToBuffer(FriendlyByteBuf buf) {
        buf.writeBoolean(lastBedSleptInto != null);
        if (lastBedSleptInto != null) buf.writeUUID(lastBedSleptInto);
        buf.writeLong(dayDeadline);
        buf.writeLong(gameDeadline);
        buf.writeLong(lastKnownDayTime);
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
        data.acceptFromServer(this.lastBedSleptInto, this.dayDeadline, this.gameDeadline, this.lastKnownDayTime,
                this.lastWokenUpTimestamp, this.consecutiveNights, this.homeBedNights, this.doubleBed);
    }
}
