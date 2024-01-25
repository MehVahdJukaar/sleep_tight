package net.mehvahdjukaar.sleep_tight.core;

import net.mehvahdjukaar.sleep_tight.common.blocks.ISleepTightBed;
import net.mehvahdjukaar.sleep_tight.common.entities.BedEntity;
import net.mehvahdjukaar.sleep_tight.common.tiles.HammockTile;
import net.mehvahdjukaar.sleep_tight.configs.CommonConfigs;
import net.mehvahdjukaar.sleep_tight.integration.HeartstoneCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

import static net.mehvahdjukaar.sleep_tight.configs.CommonConfigs.*;

public class SleepEffectsHelper {

    public static void applyEffectsOnWakeUp(PlayerSleepData playerCap, ServerPlayer player,
                                            long dayTimeDelta, BlockPos pos, ISleepTightBed bed,
                                            BlockState state, @Nullable BedData data) {
        if (data != null) { //just for vanilla bed
            applyVanillaBedBonuses(player, dayTimeDelta, data, playerCap);
            applyHeartstoneBonuses(player, pos, state, data, playerCap);

        }
        if (player.gameMode.isSurvival()) {
            if (bed.st_hasPenalties()) applySleepPenalties(player, dayTimeDelta);
            if (bed.st_hasRequirements()) paySleepRequirements(player);
        }
    }

    private static void applySleepPenalties(ServerPlayer player, long dayTimeDelta) {

        double hunger = CONSUMED_HUNGER.get();
        if (hunger == 0) return;
        HungerMode mode = CONSUME_HUNGER_MODE.get();
        if (mode == HungerMode.DIFFICULTY_BASED || mode == HungerMode.TIME_DIFFICULTY_BASED) {
            int diff = player.getLevel().getDifficulty().getId();
            if (diff == 0) return;
            else hunger *= 1 + ((diff - 1) * 0.25);
        }
        if (mode == HungerMode.TIME_BASED || mode == HungerMode.TIME_DIFFICULTY_BASED) {
            hunger = (hunger / 11000) * dayTimeDelta;
        }

        int level = player.getFoodData().getFoodLevel();
        level = (int) Mth.clamp(level - hunger, 0, 20);
        player.getFoodData().setFoodLevel(level);
    }

    private static void applyVanillaBedBonuses(ServerPlayer player, long dayTimeDelta, BedData data, PlayerSleepData playerSleepData) {

        BedStatus status = BED_BENEFITS.get();
        if (status == BedStatus.NONE) return;
        if (status == BedStatus.HOME_BED && !data.isHomeBedFor(player)) return;
        //healing
        EffectIntensity healing = HEALING.get();
        if (healing != EffectIntensity.NONE) {
            float maxHealth = player.getMaxHealth();
            float heal = maxHealth;
            if (healing == EffectIntensity.TIME_BASED) {
                heal = (maxHealth / 12000f) * dayTimeDelta;
            }
            player.heal(heal);
        }
        //potion clearing
        EffectIntensity potionClearing = EFFECT_CLEARING.get();
        if (potionClearing != EffectIntensity.NONE) {
            var type = EFFECT_CLEARING_TYPE.get();
            boolean timeBased = potionClearing == EffectIntensity.TIME_BASED;
            List<MobEffectInstance> toEditOrRemove = new ArrayList<>();
            for (var e : player.getActiveEffects()) {
                if (switch (type) {
                    case ALL -> true;
                    case HARMFUL -> e.getEffect().getCategory() == MobEffectCategory.HARMFUL;
                    case BENEFICIAL -> e.getEffect().getCategory() != MobEffectCategory.HARMFUL;
                }) {
                    toEditOrRemove.add(e);
                }
            }
            for (var e : toEditOrRemove) {
                player.removeEffect(e.getEffect());
                if (timeBased) {
                    long remainingTime = e.getDuration() - dayTimeDelta;
                    if (remainingTime > 0) {
                        CompoundTag tag = new CompoundTag();
                        e.save(tag);
                        tag.putInt("Duration", (int) remainingTime);
                        MobEffectInstance load = MobEffectInstance.load(tag);
                        if (load != null) player.addEffect(load);
                    }
                }
            }
        }
        //effects
        for (var e : WAKE_UP_EFFECTS.get()) {
            player.addEffect(e.createInstance(playerSleepData.getHomeBedLevel()));
        }
    }

    private static void applyHeartstoneBonuses(ServerPlayer player, BlockPos pos, BlockState state,
                                               @Nullable BedData data, PlayerSleepData playerSleepData) {
        BlockPos otherPos = getPartnerPos(player, state, pos);
        if (otherPos != null) {
            for (var e : HEARTSTONE_EFFECT.get()) {
                player.addEffect(e.createInstance(playerSleepData.getHomeBedLevel()));
            }
        }
    }

    private static void paySleepRequirements(ServerPlayer serverPlayer) {
        serverPlayer.giveExperiencePoints(-XP_COST.get());
    }

    //true if can sleep
    public static boolean checkExtraRequirements(Player player, @Nullable BlockPos bedPos) {
        if (bedPos != null && !player.getAbilities().instabuild) {
            BlockEntity tile = player.getLevel().getBlockEntity(bedPos);
            if (tile == null && !REQUIREMENT_NIGHT_BAG.get()) return true; //only hammocks have null tile of our bocks
            if (tile instanceof HammockTile && !REQUIREMENT_HAMMOCK.get()) return true;
            if (!REQUIREMENT_BED.get()) return true;

            int xp = XP_COST.get();
            if (xp != 0 && player.totalExperience < xp) {
                if (player.level.isClientSide) {
                    player.displayClientMessage(Component.translatable("message.sleep_tight.xp"), true);
                }
                return false;
            }
            if (NEED_FULL_HUNGER.get() && player.getFoodData().needsFood()) {
                if (player.level.isClientSide) {
                    player.displayClientMessage(Component.translatable("message.sleep_tight.hunger"), true);
                }
                return false;
            }
        }
        return true;
    }


    //partner stuff
    @org.jetbrains.annotations.Nullable
    public static BlockPos getPartnerPos(Player player, BlockState state, BlockPos pos) {
        var mode = CommonConfigs.HEARTSTONE_MODE.get();
        Level level = player.getLevel();
        if (mode != HeartstoneMode.OFF && state.getBlock() instanceof BedBlock) {
            BlockPos otherPos = BedEntity.getDoubleBedPos(pos, state);
            boolean x = hasPartnerAt(player, mode, level, otherPos);
            if (!x) {
                otherPos = BedEntity.getInverseDoubleBedPos(pos, state);
                x = hasPartnerAt(player, mode, level, otherPos);
            }
            if (x) return otherPos;
        }
        return null;
    }

    private static boolean hasPartnerAt(Player player, CommonConfigs.HeartstoneMode mode, Level level, BlockPos otherPos) {
        BlockState leftState = level.getBlockState(otherPos);
        if (leftState.getBlock() instanceof BedBlock) {
            AABB bb = new AABB(otherPos);
            for (var entity : level.getEntitiesOfClass(LivingEntity.class, bb,
                    e -> e.getSleepingPos().map(p -> p.equals(otherPos)).orElse(false))) {
                if (mode.allowVillagers && entity instanceof Villager) return true;
                if (entity instanceof Player p) {
                    if (!mode.needsHeartstone || HeartstoneCompat.isFren(player, p)) return true;
                }
            }
        }
        return false;
    }

}
