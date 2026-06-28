package net.mehvahdjukaar.sleep_tight.common;

import net.mehvahdjukaar.moonlight.api.misc.EventCalled;
import net.mehvahdjukaar.sleep_tight.SleepTight;
import net.mehvahdjukaar.sleep_tight.configs.CommonConfigs;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.Vec3;

public class InvigoratedEffect extends MobEffect {

    public InvigoratedEffect(MobEffectCategory mobEffectCategory, int i) {
        super(mobEffectCategory, i);
    }


    private static final ThreadLocal<Integer> BLOCK_XP_LEVEL = new ThreadLocal<>();


    public static void captureLevel(LivingEntity le) {
        var eff = le.getEffect(SleepTight.INVIGORATED.get());
        if (eff != null) BLOCK_XP_LEVEL.set(eff.getAmplifier());
    }

    public static void clearCapturedLevel() {
        BLOCK_XP_LEVEL.remove();
    }

    @EventCalled
    public static void fabricOnBlockXpDropped(ServerLevel level, BlockPos pos, int oldXp) {
        if (oldXp > 0) {
            Integer amp = BLOCK_XP_LEVEL.get();
            if (amp != null) {
                double extraXp = getExtraXp(oldXp, amp, level.random);

                ExperienceOrb.award(level, pos.getCenter(), (int) extraXp);
            }
        }
    }

    public static int forgeGetExtraXpForBlockBroken(int i, Entity breaker) {
        if(breaker instanceof LivingEntity le) {
            MobEffectInstance e = le.getEffect(SleepTight.INVIGORATED.get());
            if (e != null) {
                return (int) getExtraXp(i, e.getAmplifier(), le.getRandom());
            }
        }
        return 0;
    }

    @EventCalled
    public static void onLivingDeath(ServerLevel serverLevel, LivingEntity entity, LivingEntity killer) {
        MobEffectInstance i = killer.getEffect(SleepTight.INVIGORATED.get());
        if (i != null) {
            if (entity.lastHurtByPlayerTime > 0 && !entity.wasExperienceConsumed() && !(entity instanceof Player) &&
                    entity.shouldDropExperience() && serverLevel.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) {
                int oldXp = entity.getExperienceReward();
                int amp = i.getAmplifier();
                double extraXp = getExtraXp(oldXp, amp, serverLevel.random);

                ExperienceOrb.award(serverLevel, entity.position(), (int) extraXp);
            }
        }
    }

    public static double getExtraXp(int oldXp, int invigoratingLevel, RandomSource random) {
        double value = oldXp * CommonConfigs.INVIGORATED_XP.get() * (invigoratingLevel + 1);

        int actual = (int) (value);
        double remainder = value - actual;
        if (remainder != 0 && random.nextFloat() < remainder) {
            actual++;
        }
        return actual;
    }


}
