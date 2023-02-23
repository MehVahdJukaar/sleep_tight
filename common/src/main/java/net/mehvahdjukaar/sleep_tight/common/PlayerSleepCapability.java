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
    private long lastNightmareTimestamp = -999999;
    private long lastTimeSleptTimestamp = -1;
    private int consecutiveNightsSlept = 0;
    private int nightsSleptInHomeBed = 0;

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        if (homeBed != null) tag.putUUID("home_bed_id", homeBed);
        tag.putLong("last_nightmare_time", lastNightmareTimestamp);
        tag.putLong("last_time_slept", lastTimeSleptTimestamp);
        tag.putInt("consecutive_nights", consecutiveNightsSlept);
        tag.putInt("home_bed_nights", nightsSleptInHomeBed);
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        if (tag.contains("bed_id")) this.homeBed = tag.getUUID("home_bed_id");
        this.lastNightmareTimestamp = tag.getLong("last_nightmare_time");
        this.lastTimeSleptTimestamp = tag.getLong("last_time_slept");
        this.consecutiveNightsSlept = tag.getInt("consecutive_nights");
        this.nightsSleptInHomeBed = tag.getInt("home_bed_nights");
    }

    public void addNightmare(Level level) {
        this.lastNightmareTimestamp = level.getGameTime();
        this.consecutiveNightsSlept = 0;
    }

    public void onNightSleptInto(BedCapability bed, Player player) {
        long currentStamp = player.level.getGameTime();
        if (currentStamp - this.lastNightmareTimestamp > CommonConfigs.SLEEP_INTERVAL.get()) {
            //reset when had nightmare
            this.consecutiveNightsSlept = 0;
        } else {
            this.consecutiveNightsSlept += 1;
        }
        this.lastTimeSleptTimestamp = currentStamp;


        var bedId = bed.getId();
        if(bedId.equals(homeBed)){
            this.nightsSleptInHomeBed++;
            if(this.nightsSleptInHomeBed>=CommonConfigs.HOME_BED_REQUIRED_NIGHTS.get()){
                bed.setHomeBedFor(player);
            }
        }else {
            this.homeBed = bedId;
            this.nightsSleptInHomeBed = 0;
        }
    }

    //1 max 0 min
    public float getInsomniaCooldown(Level level) {
        long dur = CommonConfigs.INSOMNIA_DURATION.get();
        return (dur - (level.getGameTime() - lastNightmareTimestamp))/(float)dur -1;
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

    public long getLastNightmareTimestamp() {
        return lastNightmareTimestamp;
    }

    public int getConsecutiveNightsSlept() {
        return consecutiveNightsSlept;
    }

    public int getNightsSleptInHomeBed() {
        return nightsSleptInHomeBed;
    }

    public long getLastTimeSleptTimestamp() {
        return lastTimeSleptTimestamp;
    }

    public void acceptFromServer(UUID id, long nightmareTime, long sleepTimestamp, int nightSlept, int homeBedNights) {
        if (lastNightmareTimestamp > nightmareTime) ClientEvents.setupNightmareEffect();
        this.homeBed = id;
        this.lastNightmareTimestamp = nightmareTime;
        this.consecutiveNightsSlept = nightSlept;
        this.lastTimeSleptTimestamp = sleepTimestamp;
        this.nightsSleptInHomeBed = homeBedNights;
    }

    public void syncToClient(ServerPlayer player) {
        NetworkHandler.CHANNEL.sendToClientPlayer(player, new ClientBoundSyncPlayerSleepCapMessage(this));
    }

}
