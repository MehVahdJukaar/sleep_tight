package net.mehvahdjukaar.sleep_tight.core;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.sleep_tight.common.blocks.DreamEssenceBlock;
import net.mehvahdjukaar.sleep_tight.common.blocks.ISleepTightBed;
import net.mehvahdjukaar.sleep_tight.common.network.ClientBoundSyncPlayerSleepCapMessage;
import net.mehvahdjukaar.sleep_tight.common.network.ModNetworking;
import net.mehvahdjukaar.sleep_tight.configs.CommonConfigs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public class PlayerSleepData {

    private static final Codec<PlayerSleepData> CURRENT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.optionalFieldOf("home_bed_id").forGetter(d -> Optional.ofNullable(d.homeBed)),
            InsomniaCooldown.CODEC.fieldOf("insomnia").forGetter(d -> d.insomnia),
            Codec.LONG.fieldOf("last_time_slept").forGetter(PlayerSleepData::getLastWokenUpTime),
            Codec.INT.fieldOf("consecutive_nights").forGetter(PlayerSleepData::getConsecutiveNightsSlept),
            Codec.INT.fieldOf("home_bed_nights").forGetter(d -> d.nightsSleptInSameBed),
            Codec.BOOL.fieldOf("using_double_bed").forGetter(d -> d.usingDoubleBed)
    ).apply(instance, PlayerSleepData::new));

    //legacy layout (pre nested-insomnia rework): the cooldown was a single day-clock deadline stored flat as
    //"insomnia_elapses_at" + "last_known_time", with no game-clock backstop. Read those into an InsomniaCooldown
    //whose game deadline never elapses (Long.MAX_VALUE), so behaviour matches the old day-only cooldown.
    private static final Codec<PlayerSleepData> LEGACY_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.optionalFieldOf("home_bed_id").forGetter(d -> Optional.ofNullable(d.homeBed)),
            Codec.LONG.fieldOf("insomnia_elapses_at").forGetter(d -> d.insomnia.dayDeadline()),
            Codec.LONG.fieldOf("last_known_time").forGetter(d -> d.insomnia.lastKnownDayTime()),
            Codec.LONG.fieldOf("last_time_slept").forGetter(PlayerSleepData::getLastWokenUpTime),
            Codec.INT.fieldOf("consecutive_nights").forGetter(PlayerSleepData::getConsecutiveNightsSlept),
            Codec.INT.fieldOf("home_bed_nights").forGetter(d -> d.nightsSleptInSameBed),
            Codec.BOOL.fieldOf("using_double_bed").forGetter(d -> d.usingDoubleBed)
    ).apply(instance, PlayerSleepData::fromLegacy));

    //decode tries the current layout first, falling back to the legacy flat layout; always encode as current.
    public static final Codec<PlayerSleepData> CODEC = Codec.either(CURRENT_CODEC, LEGACY_CODEC)
            .xmap(e -> e.map(d -> d, d -> d), Either::left);

    @Nullable
    private UUID homeBed = null; //last bed slept into

    //deadline + day-clock rewind tracking; see InsomniaCooldown.
    //need to be timestamps otherwise it wont work whe player logs off or sets the time
    private InsomniaCooldown insomnia = new InsomniaCooldown();
    private long lastWokenUpTimeStamp = -1;

    private int consecutiveNightsSlept = 0;
    private int nightsSleptInSameBed = 0;
    private boolean usingDoubleBed = false;

    public PlayerSleepData() {
    }

    public PlayerSleepData(Optional<UUID> homeBed, InsomniaCooldown insomnia, long lastWokenUpTimeStamp, int consecutiveNightsSlept, int nightsSleptInSameBed, boolean usingDoubleBed) {
        this.homeBed = homeBed.orElse(null);
        this.insomnia = insomnia;
        this.lastWokenUpTimeStamp = lastWokenUpTimeStamp;
        this.consecutiveNightsSlept = consecutiveNightsSlept;
        this.nightsSleptInSameBed = nightsSleptInSameBed;
        this.usingDoubleBed = usingDoubleBed;
    }

    //builds an instance from the legacy flat insomnia fields; the missing game-clock deadline is set to
    //Long.MAX_VALUE so it never elapses, leaving the day-clock deadline as the sole (legacy) cooldown.
    private static PlayerSleepData fromLegacy(Optional<UUID> homeBed, long insomniaElapsesAt, long lastKnownTime,
                                             long lastWokenUp, int consecutiveNights, int homeBedNights, boolean doubleBed) {
        InsomniaCooldown insomnia = new InsomniaCooldown(insomniaElapsesAt, Long.MAX_VALUE, lastKnownTime);
        return new PlayerSleepData(homeBed, insomnia, lastWokenUp, consecutiveNights, homeBedNights, doubleBed);
    }

    public void tick(ServerPlayer player) {
        //on a day-time rewind the cooldown clears itself and notifies the player; we then reset the
        //consecutive-nights baseline (a player-level concern) and resync.
        if (insomnia.tickRewind(player)) {
            this.lastWokenUpTimeStamp = -1;
            syncToClient(player);
        }
    }

    public void setInsomniaCooldown(long dayTimeNow, long gameTimeNow, long cooldownDuration) {
        this.insomnia.set(dayTimeNow, gameTimeNow, cooldownDuration);
    }

    public void setLasWokenUpTime(long dayTimeNow) {
        this.lastWokenUpTimeStamp = dayTimeNow;
    }

    public void increaseNightSleptInThisBed(BedData bed, Player player) {
        if (this.isBedLastSleptInto(bed)) {

            this.nightsSleptInSameBed++;
            if (isBedFamiliarityMaxed(bed)) {
                bed.incrementBedLevel(player);
            }
        } else {
            if (nightsSleptInSameBed != 0 && homeBed != null && CommonConfigs.HOME_BED_REWARD_REQUIRED_NIGHTS.get() >= 0) {
                player.displayClientMessage(Component.translatable("message.sleep_tight.home_bed_lost"), false);
            }
            this.setLastSleptInto(bed);
            this.nightsSleptInSameBed = 0;
        }
    }

    public void increaseConsecutiveNightSleptCounter(long wakeUpTime) {
        long awakeTime = wakeUpTime - this.lastWokenUpTimeStamp; //this breaks when players log off and time passes...
        if (awakeTime > CommonConfigs.SLEEP_INTERVAL.get()) {
            //reset when hasn't slept for a while
            setConsecutiveNightsSlept(0);
        } else {
            this.consecutiveNightsSlept += 1;
        }
    }

    public void setConsecutiveNightsSlept(int consecutiveNightsSlept) {
        this.consecutiveNightsSlept = consecutiveNightsSlept;
    }

    public long getInsomniaCooldown(Player player) {
        if (player.getAbilities().instabuild) return 0;
        return insomnia.remaining(player);
    }

    public float getInsomniaCooldownPercentage(Player player) {
        //creative are immune
        long timeLeft = getInsomniaCooldown(player);
        if (timeLeft < 0) return 0;
        long maxCooldown = insomnia.dayDeadline() - this.lastWokenUpTimeStamp;
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
        var chance = CommonConfigs.NIGHTMARE_CHANCE_INCREMENT_PER_NIGHT.get()
                * (consecutiveNightsSlept - minNights - 1);

        if (!BuiltInRegistries.POINT_OF_INTEREST_TYPE.get(PoiTypes.HOME).is(state)) {
            chance *= CommonConfigs.SPECIAL_BED_NIGHTMARE_CHANCE_MULT.get();
        }
        return chance;
    }

    @Nullable
    public UUID getLastBedSleptInto() {
        return homeBed;
    }

    // Affects nightmare chance
    public int getConsecutiveNightsSlept() {
        return consecutiveNightsSlept;
    }

    public float getBedFamiliarity(BedData currentBed) {
        if (!isBedLastSleptInto(currentBed)) return 0;
        return Math.min(1, (float) nightsSleptInSameBed / CommonConfigs.HOME_BED_REWARD_REQUIRED_NIGHTS.get());
    }

    public boolean isBedFamiliarityMaxed(BedData data) {
        return getBedFamiliarity(data) >= 1;
    }

    public int getNightsSleptInHomeBed() {
        return nightsSleptInSameBed;
    }

    public long getLastWokenUpTime() {
        return lastWokenUpTimeStamp;
    }

    public void syncToClient(ServerPlayer player) {
        ModNetworking.CHANNEL.sendToClientPlayer(player, new ClientBoundSyncPlayerSleepCapMessage(this));
    }

    public InsomniaCooldown getInsomnia() {
        return insomnia;
    }

    //copies all persistent state from another instance (used on respawn/clone with the forge capability)
    public void copyFrom(PlayerSleepData other) {
        this.homeBed = other.homeBed;
        this.insomnia = other.insomnia;
        this.lastWokenUpTimeStamp = other.lastWokenUpTimeStamp;
        this.consecutiveNightsSlept = other.consecutiveNightsSlept;
        this.nightsSleptInSameBed = other.nightsSleptInSameBed;
        this.usingDoubleBed = other.usingDoubleBed;
    }

    //called on the client when receiving a sync packet from the server
    public void acceptFromServer(@Nullable UUID homeBed, long dayDeadline, long gameDeadline, long lastKnownDayTime,
                                 long lastWokenUp, int consecutiveNights, int homeBedNights, boolean doubleBed) {
        this.homeBed = homeBed;
        this.insomnia = new InsomniaCooldown(dayDeadline, gameDeadline, lastKnownDayTime);
        this.lastWokenUpTimeStamp = lastWokenUp;
        this.consecutiveNightsSlept = consecutiveNights;
        this.nightsSleptInSameBed = homeBedNights;
        this.usingDoubleBed = doubleBed;
    }


    public void setNightsSleptInHomeBed(int nightsSleptInHomeBed) {
        this.nightsSleptInSameBed = nightsSleptInHomeBed;
    }

    public boolean usingDoubleBed() {
        return usingDoubleBed;
    }

    public void setDoubleBed(boolean doubleBed) {
        this.usingDoubleBed = doubleBed;
    }

    public boolean isBedLastSleptInto(@Nullable BedData bedData) {
        return bedData != null && bedData.getId().equals(this.getLastBedSleptInto());
    }

    public void setLastSleptInto(BedData data) {
        this.homeBed = data.getId();
    }
}
