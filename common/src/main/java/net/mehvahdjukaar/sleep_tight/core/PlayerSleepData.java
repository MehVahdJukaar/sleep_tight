package net.mehvahdjukaar.sleep_tight.core;

import net.mehvahdjukaar.sleep_tight.common.blocks.DreamEssenceBlock;
import net.mehvahdjukaar.sleep_tight.common.blocks.ISleepTightBed;
import net.mehvahdjukaar.sleep_tight.common.network.ClientBoundSyncPlayerSleepCapMessage;
import net.mehvahdjukaar.sleep_tight.common.network.NetworkHandler;
import net.mehvahdjukaar.sleep_tight.configs.CommonConfigs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

//ideally only data associated with a player here. Does contain some logic...
public abstract class PlayerSleepData {

    protected static final String HOME_BED_NBT = "home_bed_id";
    protected static final String INSOMNIA_ELAPSE_NBT = "insomnia_elapses_at";
    protected static final String LAST_TIME_SLEPT_NBT = "last_time_slept";
    protected static final String CONSECUTIVE_NIGHTS_NBT = "consecutive_nights";
    protected static final String HOME_BED_LEVEL_NBT = "home_bed_nights";
    protected static final String USING_DOUBLE_BED_NBT = "using_double_bed";
    protected static final String LAST_KNOWN_TIME_NBT = "last_known_time";

    @Nullable
    private UUID homeBed = null; //last bed slept into

    private long insomniaWillElapseTimeStamp = 0; //need to be timestamps otherwise it wont work whe player logs off or sets the time
    private long lastWokenUpTimeStamp = -1;
    private long lastKnownTimeStamp = 0;  //keeps track of last seen time to prevent time skips in the past and reset cooldowns if it happens to prevent infinite cooldowns

    private int consecutiveNightsSlept = 0;
    private int nightsSleptInSameBed = 0;
    private boolean usingDoubleBed = false;

    public CompoundTag serializeNBT(HolderLookup.Provider reg) {
        CompoundTag tag = new CompoundTag();
        if (homeBed != null) {
            tag.putUUID(HOME_BED_NBT, homeBed);
        }
        tag.putLong(INSOMNIA_ELAPSE_NBT, insomniaWillElapseTimeStamp);
        tag.putLong(LAST_TIME_SLEPT_NBT, lastWokenUpTimeStamp);
        tag.putInt(CONSECUTIVE_NIGHTS_NBT, consecutiveNightsSlept);
        tag.putInt(HOME_BED_LEVEL_NBT, nightsSleptInSameBed);
        tag.putBoolean(USING_DOUBLE_BED_NBT, usingDoubleBed);
        tag.putLong(LAST_KNOWN_TIME_NBT, lastKnownTimeStamp);
        return tag;
    }

    public void deserializeNBT(HolderLookup.Provider reg, CompoundTag tag) {
        if (tag.contains(HOME_BED_NBT)) this.homeBed = tag.getUUID(HOME_BED_NBT);
        this.insomniaWillElapseTimeStamp = tag.getLong(INSOMNIA_ELAPSE_NBT);
        this.lastWokenUpTimeStamp = tag.getLong(LAST_TIME_SLEPT_NBT);
        this.consecutiveNightsSlept = tag.getInt(CONSECUTIVE_NIGHTS_NBT);
        this.nightsSleptInSameBed = tag.getInt(HOME_BED_LEVEL_NBT);
        this.usingDoubleBed = tag.getBoolean(USING_DOUBLE_BED_NBT);
        this.lastKnownTimeStamp = tag.getLong(LAST_KNOWN_TIME_NBT);
    }

    public void tick(ServerPlayer player) {
        long gameTime = player.level().getDayTime();
        if (gameTime < lastKnownTimeStamp) {
            if (isOnSleepCooldown(player)) {
                player.displayClientMessage(Component.translatable("message.sleep_tight.time_skipped"), false);
            }
            //reset cooldowns if time has gone back
            this.insomniaWillElapseTimeStamp = 0;
            this.lastWokenUpTimeStamp = -1;
            this.lastKnownTimeStamp = gameTime;
            syncToClient(player);
        }
    }

    public void setInsomniaCooldown(Player player, long duration) {
        long gameTime = player.level().getDayTime();
        this.insomniaWillElapseTimeStamp = gameTime + duration;

        this.lastKnownTimeStamp = gameTime;
    }

    public void setLasWokenUpTime(Level level) {
        long gameTime = level.getDayTime();
        this.lastWokenUpTimeStamp = gameTime;

        this.lastKnownTimeStamp = gameTime;
    }

    public void increaseNightSleptInThisBed(BedData bed, Player player) {
        if (this.isHomeBed(bed)) {
            int required = CommonConfigs.HOME_BED_REWARD_REQUIRED_NIGHTS.get();

            this.nightsSleptInSameBed++;
            if (this.nightsSleptInSameBed >= required) {
                bed.onHomeBedActivated(player);
            }
        } else {
            if (nightsSleptInSameBed != 0) {
                player.displayClientMessage(Component.translatable("message.sleep_tight.home_bed_lost"), false);
            }
            this.setLastSleptInto(bed);
            this.nightsSleptInSameBed = 0;
        }
    }

    public void increaseConsecutiveNightSleptCounter(Player player) {
        long dayTime = player.level().getDayTime();
        long awakeTime = dayTime - this.lastWokenUpTimeStamp;
        if (awakeTime > CommonConfigs.SLEEP_INTERVAL.get()) {
            //reset when hasn't slept for a while
            this.consecutiveNightsSlept = 0;
        } else {
            this.consecutiveNightsSlept += 1;
        }
    }

    public void resetConsecutiveNightSleptCounter() {
        consecutiveNightsSlept = 0;
    }

    public long getInsomniaCooldown(Player player) {
        if (player.getAbilities().instabuild) return 0;
        return insomniaWillElapseTimeStamp - player.level().getDayTime();
    }

    public float getInsomniaCooldownPercentage(Player player) {
        //creative are immune
        long timeLeft = getInsomniaCooldown(player);
        if (timeLeft < 0) return 0;
        long maxCooldown = insomniaWillElapseTimeStamp - this.lastWokenUpTimeStamp;
        return ((float) timeLeft / maxCooldown);
    }

    public boolean isOnSleepCooldown(Player player) {
        if (player.getAbilities().instabuild) return false;
        return getInsomniaCooldown(player) > 0;
    }

    public double getNightmareChance(Player player, BlockPos pos) {
        if (player.getAbilities().instabuild) return 0;

        int minNights = CommonConfigs.NIGHTMARES_CONSECUTIVE_NIGHTS.get();
        if (consecutiveNightsSlept < minNights) return 0;
        if (DreamEssenceBlock.isInRange(player.blockPosition(), player.level())) return 0;
        BlockState state = player.level().getBlockState(pos);
        if (state.getBlock() instanceof ISleepTightBed bed) {
            if (!bed.st_canCauseNightmares()) return 0;
        }

        return CommonConfigs.NIGHTMARE_CHANCE_INCREMENT_PER_NIGHT.get()
                * (consecutiveNightsSlept - minNights - 1);
    }

    @Nullable
    public UUID getLastBedSleptInto() {
        return homeBed;
    }

    public int getConsecutiveNightsSlept() {
        return consecutiveNightsSlept;
    }

    public int getNightsSleptInHomeBed() {
        return nightsSleptInSameBed;
    }

    public long getLastWokenUpTime() {
        return lastWokenUpTimeStamp;
    }

    public long getInsomniaWillElapseTime() {
        return insomniaWillElapseTimeStamp;
    }

    public void acceptFromServer(UUID id, long insominaElapse, long sleepTimestamp, int nightSlept, int homeBedNights, boolean doubleBed) {
        this.homeBed = id;
        this.insomniaWillElapseTimeStamp = insominaElapse;
        this.consecutiveNightsSlept = nightSlept;
        this.lastWokenUpTimeStamp = sleepTimestamp;
        this.nightsSleptInSameBed = homeBedNights;
        this.usingDoubleBed = doubleBed;
    }

    public void syncToClient(ServerPlayer player) {
        NetworkHandler.CHANNEL.sendToClientPlayer(player, new ClientBoundSyncPlayerSleepCapMessage(this));
    }

    public void setConsecutiveNightsSlept(int consecutiveNightsSlept) {
        this.consecutiveNightsSlept = consecutiveNightsSlept;
    }

    public void setNightsSleptInHomeBed(int nightsSleptInHomeBed) {
        this.nightsSleptInSameBed = nightsSleptInHomeBed;
    }

    public void copyFrom(PlayerSleepData oldData) {
        this.consecutiveNightsSlept = oldData.consecutiveNightsSlept;
        this.homeBed = oldData.homeBed;
        this.nightsSleptInSameBed = oldData.nightsSleptInSameBed;
        this.insomniaWillElapseTimeStamp = oldData.insomniaWillElapseTimeStamp;
        this.lastWokenUpTimeStamp = oldData.lastWokenUpTimeStamp;
        this.usingDoubleBed = oldData.usingDoubleBed;
    }

    public int getHomeBedLevel() {
        return Mth.clamp( this.nightsSleptInSameBed - CommonConfigs.HOME_BED_REWARD_REQUIRED_NIGHTS.get(), 0, CommonConfigs.HOME_BED_MAX_LEVEL.get());
    }

    public boolean usingDoubleBed() {
        return usingDoubleBed;
    }

    public void setDoubleBed(boolean doubleBed) {
        this.usingDoubleBed = doubleBed;
    }

    public boolean isHomeBed(@Nullable BedData bedData) {
        return bedData != null && bedData.getId().equals(this.getLastBedSleptInto());
    }

    public void setLastSleptInto(BedData data) {
        this.homeBed = data.getId();
    }
}
