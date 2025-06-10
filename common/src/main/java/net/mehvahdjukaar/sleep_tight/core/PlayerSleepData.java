package net.mehvahdjukaar.sleep_tight.core;

import net.mehvahdjukaar.sleep_tight.SleepTightPlatformStuff;
import net.mehvahdjukaar.sleep_tight.common.blocks.DreamEssenceBlock;
import net.mehvahdjukaar.sleep_tight.common.blocks.ISleepTightBed;
import net.mehvahdjukaar.sleep_tight.common.network.ClientBoundSyncPlayerSleepCapMessage;
import net.mehvahdjukaar.sleep_tight.common.network.NetworkHandler;
import net.mehvahdjukaar.sleep_tight.configs.CommonConfigs;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

//ideally only data associated with a player here. Does contain some logic...
public abstract class PlayerSleepData {

    protected static final String HOME_BED_NBT = "home_bed_id";
    protected static final String INSOMNIA_COOLDOWN_NBT = "insomnia_cooldown";
    protected static final String TIME_SINCE_LAST_SLEPT_NBT = "time_since_last_slept";
    protected static final String CONSECUTIVE_NIGHTS_NBT = "consecutive_nights";
    protected static final String HOME_BED_LEVEL_NBT = "home_bed_nights";
    protected static final String USING_DOUBLE_BED_NBT = "using_double_bed";

    @Nullable
    private UUID lastBedSleptInto = null;

    private long maxLastInsomniaCooldown = 0;
    private long insomniaCooldown = 0;
    private long timeSinceLastSlept = 0;

    private int consecutiveNightsSlept = 0;
    private int nightsSleptInHomeBed = 0;
    private boolean usingDoubleBed = false;

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        if (lastBedSleptInto != null) {
            tag.putUUID(HOME_BED_NBT, lastBedSleptInto);
        }
        tag.putLong(INSOMNIA_COOLDOWN_NBT, insomniaCooldown);
        tag.putLong(TIME_SINCE_LAST_SLEPT_NBT, timeSinceLastSlept);
        tag.putInt(CONSECUTIVE_NIGHTS_NBT, consecutiveNightsSlept);
        tag.putInt(HOME_BED_LEVEL_NBT, nightsSleptInHomeBed);
        tag.putBoolean(USING_DOUBLE_BED_NBT, usingDoubleBed);
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        if (tag.contains(HOME_BED_NBT)) this.lastBedSleptInto = tag.getUUID(HOME_BED_NBT);
        this.insomniaCooldown = tag.getLong(INSOMNIA_COOLDOWN_NBT);
        this.timeSinceLastSlept = tag.getLong(TIME_SINCE_LAST_SLEPT_NBT);
        this.consecutiveNightsSlept = tag.getInt(CONSECUTIVE_NIGHTS_NBT);
        this.nightsSleptInHomeBed = tag.getInt(HOME_BED_LEVEL_NBT);
        this.usingDoubleBed = tag.getBoolean(USING_DOUBLE_BED_NBT);
    }

    public void tick(Level level) {
        if (this.insomniaCooldown > 0) {
            this.insomniaCooldown--;
        }
        this.timeSinceLastSlept++;
    }

    public void setInsomniaCooldown(Player player, long duration) {
        this.insomniaCooldown = (int) duration;
        this.maxLastInsomniaCooldown = duration;
    }

    public void maybeIncreaseNightsInHomeBed(BedData bed, Player player) {
        var bedId = bed.getId();
        if (bedId.equals(lastBedSleptInto)) {
            int required = CommonConfigs.HOME_BED_REQUIRED_NIGHTS.get();

            this.nightsSleptInHomeBed = Math.min(required + CommonConfigs.HOME_BED_MAX_LEVEL.get(), nightsSleptInHomeBed + 1);
            if (this.nightsSleptInHomeBed >= required) {
                bed.setHomeBedFor(player);
            }
        } else {
            player.displayClientMessage(Component.literal("Debug: setting last bed slept into"), false);
            this.setLastSleptInto(bed);
            this.nightsSleptInHomeBed = 0;
        }
    }

    public void setLasWokenUpTime(Level level) {
        this.timeSinceLastSlept = 0;
    }

    public void increaseConsecutiveNightSleptCounter(Player player) {
        long awakeTime = this.timeSinceLastSlept;
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

    public long getInsomniaCooldown() {
        return insomniaCooldown;
    }

    public float getInsomniaCooldownPercentage(Player player) {
        //creative are immune
        if (player.getAbilities().instabuild) return 0;
        return getInsomniaCooldown() / (float) maxLastInsomniaCooldown;
    }

    public boolean isOnSleepCooldown(Player player) {
        return getInsomniaCooldown() > 0;
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
        return lastBedSleptInto;
    }

    public int getConsecutiveNightsSlept() {
        return consecutiveNightsSlept;
    }

    public int getNightsSleptInHomeBed() {
        return nightsSleptInHomeBed;
    }

    public long getTimeSinceLastSlept() {
        return timeSinceLastSlept;
    }

    public void acceptFromServer(UUID id, long insomniaCooldown, long timeSinceLastSlept, int nightSlept, int homeBedNights, boolean doubleBed) {
        this.lastBedSleptInto = id;
        this.insomniaCooldown = insomniaCooldown;
        this.consecutiveNightsSlept = nightSlept;
        this.timeSinceLastSlept = timeSinceLastSlept;
        this.nightsSleptInHomeBed = homeBedNights;
        this.usingDoubleBed = doubleBed;
    }

    public void syncToClient(ServerPlayer player) {
        NetworkHandler.CHANNEL.sendToClientPlayer(player, new ClientBoundSyncPlayerSleepCapMessage(this));
    }

    public void setConsecutiveNightsSlept(int consecutiveNightsSlept) {
        this.consecutiveNightsSlept = consecutiveNightsSlept;
    }

    public void setNightsSleptInHomeBed(int nightsSleptInHomeBed) {
        this.nightsSleptInHomeBed = nightsSleptInHomeBed;
    }

    public void copyFrom(PlayerSleepData oldData) {
        this.consecutiveNightsSlept = oldData.consecutiveNightsSlept;
        this.lastBedSleptInto = oldData.lastBedSleptInto;
        this.nightsSleptInHomeBed = oldData.nightsSleptInHomeBed;
        this.insomniaCooldown = oldData.insomniaCooldown;
        this.timeSinceLastSlept = oldData.timeSinceLastSlept;
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
        BedData bedCap = SleepTightPlatformStuff.getBedDataAt(player.level(), pos);
        if (bedCap != null && bedCap.getId().equals(sleepData.getHomeBed()) && bedCap.isHomeBedFor(player)) {
            return bedCap;
        }
        return null;
    }

    public void setLastSleptInto(BedData data) {
        this.lastBedSleptInto = data.getId();
    }
}
