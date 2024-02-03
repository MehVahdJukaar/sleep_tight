package net.mehvahdjukaar.sleep_tight.core;

import net.mehvahdjukaar.moonlight.api.platform.network.NetworkHelper;
import net.mehvahdjukaar.sleep_tight.SleepTightPlatformStuff;
import net.mehvahdjukaar.sleep_tight.common.blocks.DreamEssenceBlock;
import net.mehvahdjukaar.sleep_tight.common.blocks.ISleepTightBed;
import net.mehvahdjukaar.sleep_tight.common.network.ClientBoundSyncPlayerSleepCapMessage;
import net.mehvahdjukaar.sleep_tight.common.network.ModMessages;
import net.mehvahdjukaar.sleep_tight.common.tiles.IExtraBedDataProvider;
import net.mehvahdjukaar.sleep_tight.configs.CommonConfigs;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
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

    @Nullable
    private UUID homeBed = null;
    private long insomniaWillElapseTimeStamp = 0;
    private long lastWokenUpTimeStamp = -1;
    private int consecutiveNightsSlept = 0;
    private int nightsSleptInHomeBed = 0;
    private boolean usingDoubleBed = false;

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        if (homeBed != null) {
            tag.putUUID(HOME_BED_NBT, homeBed);
        }
        tag.putLong(INSOMNIA_ELAPSE_NBT, insomniaWillElapseTimeStamp);
        tag.putLong(LAST_TIME_SLEPT_NBT, lastWokenUpTimeStamp);
        tag.putInt(CONSECUTIVE_NIGHTS_NBT, consecutiveNightsSlept);
        tag.putInt(HOME_BED_LEVEL_NBT, nightsSleptInHomeBed);
        tag.putBoolean(USING_DOUBLE_BED_NBT, usingDoubleBed);
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        if (tag.contains(HOME_BED_NBT)) this.homeBed = tag.getUUID(HOME_BED_NBT);
        this.insomniaWillElapseTimeStamp = tag.getLong(INSOMNIA_ELAPSE_NBT);
        this.lastWokenUpTimeStamp = tag.getLong(LAST_TIME_SLEPT_NBT);
        this.consecutiveNightsSlept = tag.getInt("consecutive_nights");
        this.nightsSleptInHomeBed = tag.getInt("home_bed_nights");
        this.usingDoubleBed = tag.getBoolean("using_double_bed");
    }

    public void addInsomnia(Player player, long duration) {
        long gameTime = player.level().getGameTime();
        this.insomniaWillElapseTimeStamp = gameTime + duration;
    }

    public void maybeIncreaseNightsInHomeBed(BedData bed, Player player) {
        var bedId = bed.getId();
        if (bedId.equals(homeBed)) {
            int required = CommonConfigs.HOME_BED_REQUIRED_NIGHTS.get();

            this.nightsSleptInHomeBed = Math.min(required + CommonConfigs.HOME_BED_MAX_LEVEL.get(), nightsSleptInHomeBed + 1);
            if (this.nightsSleptInHomeBed >= required) {
                bed.setHomeBedFor(player);
            }
        } else {
            this.homeBed = bedId;
            this.nightsSleptInHomeBed = 0;
        }
    }

    public void setLasWokenUpTime(Level level) {
        long gameTime = level.getGameTime();
        this.lastWokenUpTimeStamp = gameTime;
    }

    public void increaseConsecutiveNightSleptCounter(Player player) {
        long gameTime = player.level().getGameTime();
        long awakeTime = gameTime - this.lastWokenUpTimeStamp;
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

    //1 max 0 min
    public float getInsomniaCooldown(Player player) {
        //creative are immune
        if (player.getAbilities().instabuild) return 0;
        long currentTime = player.level().getGameTime();
        long timeLeft = insomniaWillElapseTimeStamp - currentTime;
        if (timeLeft < 0) return 0;
        long maxCooldown = insomniaWillElapseTimeStamp - this.lastWokenUpTimeStamp;
        return ((float) timeLeft / maxCooldown);
    }

    public boolean isOnSleepCooldown(Player player) {
        return getInsomniaCooldown(player) > 0;
    }

    public long getInsomniaTimeLeft(Player player) {
        return insomniaWillElapseTimeStamp - player.level().getGameTime();
    }

    public double getNightmareChance(Player player, BlockPos pos) {
        if (player.isCreative()) return 0;
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

    public void acceptFromServer(UUID id, long insominaElapse, long sleepTimestamp, int nightSlept, int homeBedNights, boolean doubleBed) {
        this.homeBed = id;
        this.insomniaWillElapseTimeStamp = insominaElapse;
        this.consecutiveNightsSlept = nightSlept;
        this.lastWokenUpTimeStamp = sleepTimestamp;
        this.nightsSleptInHomeBed = homeBedNights;
        this.usingDoubleBed = doubleBed;
    }

    public void syncToClient(ServerPlayer player) {
        NetworkHelper.sendToClientPlayer(player, new ClientBoundSyncPlayerSleepCapMessage(this));
    }

    public void setConsecutiveNightsSlept(int consecutiveNightsSlept) {
        this.consecutiveNightsSlept = consecutiveNightsSlept;
    }

    public void setNightsSleptInHomeBed(int nightsSleptInHomeBed) {
        this.nightsSleptInHomeBed = nightsSleptInHomeBed;
    }

    public void copyFrom(PlayerSleepData oldData) {
        this.consecutiveNightsSlept = oldData.consecutiveNightsSlept;
        this.homeBed = oldData.homeBed;
        this.nightsSleptInHomeBed = oldData.nightsSleptInHomeBed;
        this.insomniaWillElapseTimeStamp = oldData.insomniaWillElapseTimeStamp;
        this.lastWokenUpTimeStamp = oldData.lastWokenUpTimeStamp;
        this.usingDoubleBed = oldData.usingDoubleBed;
    }

    public int getHomeBedLevel() {
        return Math.max(0, this.nightsSleptInHomeBed - CommonConfigs.HOME_BED_REQUIRED_NIGHTS.get());
    }

    public boolean usingDoubleBed() {
        return usingDoubleBed;
    }

    public void setDoubleBed(boolean doubleBed) {
        this.usingDoubleBed = doubleBed;
    }

    @Nullable
    public static BedData getHomeBedIfHere(Player player, BlockPos pos) {
        PlayerSleepData sleepData = SleepTightPlatformStuff.getPlayerSleepData(player);
        if (player.level().getBlockEntity(pos) instanceof IExtraBedDataProvider bed) {
            BedData bedCap = bed.st_getBedData();
            if (bedCap.getId().equals(sleepData.getHomeBed()) && bedCap.isHomeBedFor(player)) {
                return bedCap;
            }
        }
        return null;
    }
}
