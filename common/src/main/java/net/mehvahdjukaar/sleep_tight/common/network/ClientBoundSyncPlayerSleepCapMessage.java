package net.mehvahdjukaar.sleep_tight.common.network;

import net.mehvahdjukaar.moonlight.api.platform.network.Message;
import net.mehvahdjukaar.sleep_tight.SleepTight;
import net.mehvahdjukaar.sleep_tight.SleepTightClient;
import net.mehvahdjukaar.sleep_tight.SleepTightPlatformStuff;
import net.mehvahdjukaar.sleep_tight.core.PlayerSleepData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class ClientBoundSyncPlayerSleepCapMessage implements Message {

    public static final TypeAndCodec<RegistryFriendlyByteBuf, ClientBoundSyncPlayerSleepCapMessage> TYPE = Message.makeType(
            SleepTight.res("sync_player_sleep_cap"),
            ClientBoundSyncPlayerSleepCapMessage::new
    );
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
        this(SleepTightPlatformStuff.getPlayerSleepData(player));
    }

    @Override
    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeBoolean(lastBedSleptInto != null);
        if (lastBedSleptInto != null) buf.writeUUID(lastBedSleptInto);
        buf.writeLong(insomniaWillElapseTimestamp);
        buf.writeLong(lastWokenUpTimestamp);
        buf.writeInt(consecutiveNights);
        buf.writeInt(homeBedNights);
        buf.writeBoolean(doubleBed);
    }

    @Override
    public void handle(Context context) {
        Player p = SleepTightClient.getPlayer();
        if (p == null) {
            return;
        }
        PlayerSleepData data = SleepTightPlatformStuff.getPlayerSleepData(p);
        data.acceptFromServer(this.lastBedSleptInto, this.insomniaWillElapseTimestamp, this.lastWokenUpTimestamp, this.consecutiveNights,
                this.homeBedNights, this.doubleBed);
    }

    @Override
    public @NotNull Type<?> type() {
        return TYPE.type();
    }
}
