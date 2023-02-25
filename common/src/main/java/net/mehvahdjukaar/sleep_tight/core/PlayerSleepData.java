package net.mehvahdjukaar.sleep_tight.core;

import net.mehvahdjukaar.sleep_tight.common.DreamerEssenceTargetEntity;
import net.mehvahdjukaar.sleep_tight.configs.CommonConfigs;
import net.mehvahdjukaar.sleep_tight.network.ClientBoundSyncPlayerSleepCapMessage;
import net.mehvahdjukaar.sleep_tight.network.NetworkHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.UUID;

//ideally only data associated with a player here. Does contain some logic...
public abstract class PlayerSleepData {

    @Nullable
    private UUID homeBed = null;
    private long insomniaWillElapseTimeStamp = 0;
    private long lastWokenUpTimeStamp = -1;
    private int consecutiveNightsSlept = 0;
    private int nightsSleptInHomeBed = 0;

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        if (homeBed != null) tag.putUUID("home_bed_id", homeBed);
        tag.putLong("insomnia_elapses_at", insomniaWillElapseTimeStamp);
        tag.putLong("last_time_slept", lastWokenUpTimeStamp);
        tag.putInt("consecutive_nights", consecutiveNightsSlept);
        tag.putInt("home_bed_nights", nightsSleptInHomeBed);
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        if (tag.contains("bed_id")) this.homeBed = tag.getUUID("home_bed_id");
        this.insomniaWillElapseTimeStamp = tag.getLong("insomnia_elapses_at");
        this.lastWokenUpTimeStamp = tag.getLong("last_time_slept");
        this.consecutiveNightsSlept = tag.getInt("consecutive_nights");
        this.nightsSleptInHomeBed = tag.getInt("home_bed_nights");
    }

    public void addInsomnia(Player player, long duration) {
        long gameTime = player.level.getGameTime();
        this.insomniaWillElapseTimeStamp = gameTime + duration;
        this.consecutiveNightsSlept = 0;

        this.lastWokenUpTimeStamp = gameTime;
    }

    public void onNightSleptInto(BedData bed, Player player) {
        long gameTime = player.level.getGameTime();
        long awakeTime = gameTime - this.lastWokenUpTimeStamp;
        if (awakeTime > CommonConfigs.SLEEP_INTERVAL.get()) {
            //reset when hasn't slept for a while
            this.consecutiveNightsSlept = 0;
        } else {
            this.consecutiveNightsSlept += 1;
        }
        this.lastWokenUpTimeStamp = gameTime;


        var bedId = bed.getId();
        if (bedId.equals(homeBed)) {
            this.nightsSleptInHomeBed++;
            if (this.nightsSleptInHomeBed >= CommonConfigs.HOME_BED_REQUIRED_NIGHTS.get()) {
                bed.setHomeBedFor(player);
            }
        } else {
            this.homeBed = bedId;
            this.nightsSleptInHomeBed = 0;
        }
    }

    //1 max 0 min
    public float getInsomniaCooldown(Level level) {
        long currentTime = level.getGameTime();
        long timeLeft = insomniaWillElapseTimeStamp - currentTime;
        if (timeLeft < 0) return 0;
        long amountAwake = currentTime - this.lastWokenUpTimeStamp;
        return 1 - (((float) (amountAwake)) / timeLeft);
    }

    public double getNightmareChance(Player player) {
        int minNights = CommonConfigs.NIGHTMARES_CONSECUTIVE_NIGHTS.get();
        if (consecutiveNightsSlept < minNights) return 0;
        if (isDreamerEssenceInRange(player.blockPosition(), player.level)) return 0;
        else return CommonConfigs.NIGHTMARE_CHANCE_INCREMENT_PER_NIGHT.get()
                * (consecutiveNightsSlept - minNights - 1);
    }

    public static boolean isDreamerEssenceInRange(BlockPos pos, Level level) {
        return !level.getEntitiesOfClass(DreamerEssenceTargetEntity.class,
                new AABB(pos).inflate(5)).isEmpty();
    }

    @Nullable
    public UUID getHomeBed() {
        return homeBed;
    }

    public long getInsomniaWillElapseTimeStamp() {
        return insomniaWillElapseTimeStamp;
    }

    public int getConsecutiveNightsSlept() {
        return consecutiveNightsSlept;
    }

    public int getNightsSleptInHomeBed() {
        return nightsSleptInHomeBed;
    }

    public long getLastWokenUpTimeStamp() {
        return lastWokenUpTimeStamp;
    }

    public void acceptFromServer(UUID id, long insominaElapse, long sleepTimestamp, int nightSlept, int homeBedNights) {
        this.homeBed = id;
        this.insomniaWillElapseTimeStamp = insominaElapse;
        this.consecutiveNightsSlept = nightSlept;
        this.lastWokenUpTimeStamp = sleepTimestamp;
        this.nightsSleptInHomeBed = homeBedNights;
    }

    public void syncToClient(ServerPlayer player) {
        NetworkHandler.CHANNEL.sendToClientPlayer(player, new ClientBoundSyncPlayerSleepCapMessage(this));
    }

}
