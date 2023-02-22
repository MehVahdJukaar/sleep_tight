package net.mehvahdjukaar.sleep_tight.common;

import net.mehvahdjukaar.sleep_tight.client.ClientEvents;
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

public abstract class PlayerSleepCapability {

    @Nullable
    private UUID homeBed = null;
    private long lastNightmareTimestamp = -1;
    private long lastTimeSleptTimestamp = -1;
    private int consecutiveNightsSlept = 0;

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        if (homeBed != null) tag.putUUID("bed_id", homeBed);
        tag.putLong("last_nightmare_time", lastNightmareTimestamp);
        tag.putLong("last_time_slept", lastTimeSleptTimestamp);
        tag.putInt("consecutive_nights", consecutiveNightsSlept);
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        if (tag.contains("bed_id")) this.homeBed = tag.getUUID("bed_id");
        this.lastNightmareTimestamp = tag.getLong("last_nightmare_time");
        this.lastTimeSleptTimestamp = tag.getLong("last_time_slept");
        this.consecutiveNightsSlept = tag.getInt("consecutive_nights");
    }

    public long getInsomniaCooldown(Level level) {
        return CommonConfigs.INSOMNIA_DURATION.get() - (level.getGameTime() - lastNightmareTimestamp);
    }

    //call on boh sides. wont sync. we'll blindly trust the client here
    public void addNightmare(Level level) {
        this.lastNightmareTimestamp = level.getGameTime();
        this.consecutiveNightsSlept = 0;
    }

    public void assignHomeBed(UUID bedId) {
        this.homeBed = bedId;
    }

    public void increaseNightSlept(Level level) {
        long currentStamp = level.getGameTime();
        if (currentStamp - this.lastNightmareTimestamp > CommonConfigs.SLEEP_INTERVAL.get()) {
            this.consecutiveNightsSlept = 0;
        } else {
            this.consecutiveNightsSlept += 1;
        }
        this.lastTimeSleptTimestamp = currentStamp;
    }

    public double getNightmareChance(Player player) {
        int minNights = CommonConfigs.NIGHTMARES_CONSECUTIVE_NIGHTS.get();
        if(true)return 1;
        if (consecutiveNightsSlept < minNights) return 0;
        if (isDreamerEssenceInRange(player.getSleepingPos().get(), player.level)) return 0;
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

    public long getLastNightmareTimestamp() {
        return lastNightmareTimestamp;
    }

    public int getConsecutiveNightsSlept() {
        return consecutiveNightsSlept;
    }

    public long getLastTimeSleptTimestamp() {
        return lastTimeSleptTimestamp;
    }

    public void acceptFromServer(UUID id, long nightmareTime, long sleepTimestamp, int nightSlept) {
        if (lastNightmareTimestamp > nightmareTime) ClientEvents.setupNightmareEffect();
        this.homeBed = id;
        this.lastNightmareTimestamp = nightmareTime;
        this.consecutiveNightsSlept = nightSlept;
        this.lastTimeSleptTimestamp = sleepTimestamp;
    }

    public void syncToClient(ServerPlayer player) {
        NetworkHandler.CHANNEL.sendToClientPlayer(player, new ClientBoundSyncPlayerSleepCapMessage(this));
    }

}
