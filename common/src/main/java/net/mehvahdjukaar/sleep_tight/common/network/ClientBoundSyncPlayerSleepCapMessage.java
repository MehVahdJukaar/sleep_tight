package net.mehvahdjukaar.sleep_tight.common.network;

import net.mehvahdjukaar.moonlight.api.platform.network.ChannelHandler;
import net.mehvahdjukaar.moonlight.api.platform.network.Message;
import net.mehvahdjukaar.sleep_tight.SleepTightClient;
import net.mehvahdjukaar.sleep_tight.SleepTightPlatformStuff;
import net.mehvahdjukaar.sleep_tight.core.PlayerSleepData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class ClientBoundSyncPlayerSleepCapMessage implements Message {
    @Nullable
    private final UUID lastBedSleptInto;
    private final long insomniaCooldown;
    private final long timeSinceLastSlept;
    private final int consecutiveNights;
    private final int homeBedNights;
    private final boolean doubleBed;

    public ClientBoundSyncPlayerSleepCapMessage(FriendlyByteBuf buf) {
        if (buf.readBoolean()) this.lastBedSleptInto = buf.readUUID();
        else lastBedSleptInto = null;
        this.insomniaCooldown = buf.readLong();
        this.timeSinceLastSlept = buf.readLong();
        this.consecutiveNights = buf.readInt();
        this.homeBedNights = buf.readInt();
        this.doubleBed = buf.readBoolean();
    }

    public ClientBoundSyncPlayerSleepCapMessage(PlayerSleepData c) {
        this.lastBedSleptInto = c.getLastBedSleptInto();
        this.insomniaCooldown = c.getInsomniaCooldown();
        this.timeSinceLastSlept = c.getTimeSinceLastSlept();
        this.consecutiveNights = c.getConsecutiveNightsSlept();
        this.homeBedNights = c.getNightsSleptInHomeBed();
        this.doubleBed = c.usingDoubleBed();
    }

    public ClientBoundSyncPlayerSleepCapMessage(Player player) {
        this(SleepTightPlatformStuff.getPlayerSleepData(player));
    }

    @Override
    public void writeToBuffer(FriendlyByteBuf buf) {
        buf.writeBoolean(lastBedSleptInto != null);
        if (lastBedSleptInto != null) buf.writeUUID(lastBedSleptInto);
        buf.writeLong(insomniaCooldown);
        buf.writeLong(timeSinceLastSlept);
        buf.writeInt(consecutiveNights);
        buf.writeInt(homeBedNights);
        buf.writeBoolean(doubleBed);
    }

    @Override
    public void handle(ChannelHandler.Context context) {
        Player p = SleepTightClient.getPlayer();
        if (p == null) {
            return;
        }
        PlayerSleepData data = SleepTightPlatformStuff.getPlayerSleepData(p);
        data.acceptFromServer(this.lastBedSleptInto, this.insomniaCooldown, this.timeSinceLastSlept, this.consecutiveNights,
                this.homeBedNights, this.doubleBed);
    }
}
