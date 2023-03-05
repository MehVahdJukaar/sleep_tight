package net.mehvahdjukaar.sleep_tight.common;

import net.fabricmc.loader.impl.lib.sat4j.core.Vec;
import net.mehvahdjukaar.moonlight.api.misc.EventCalled;
import net.mehvahdjukaar.sleep_tight.SleepTight;
import net.mehvahdjukaar.sleep_tight.configs.CommonConfigs;
import net.mehvahdjukaar.sleep_tight.core.ModEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

public class InvigoratingEffect extends MobEffect {

    public InvigoratingEffect(MobEffectCategory mobEffectCategory, int i) {
        super(mobEffectCategory, i);
    }


    public static final ThreadLocal<Integer> BLOCK_XP_LEVEL = new ThreadLocal<>();

    @EventCalled
    public static void onBlcokXpDropped(ServerLevel level, BlockPos pos, int amount) {
        if(amount > 0){
            Integer amp = BLOCK_XP_LEVEL.get();
            if(amp != null){
                awardBonusXp(level, Vec3.atCenterOf(pos), amount, amp);
            }
        }
    }

    @EventCalled
    public static void onLivingDeath(ServerLevel serverLevel, LivingEntity entity, LivingEntity killer) {
        MobEffectInstance i = killer.getEffect(SleepTight.INVIGORATING.get());
        if (i != null) {
            if (entity.lastHurtByPlayerTime > 0 && !entity.wasExperienceConsumed() && !(entity instanceof Player) &&
                    entity.shouldDropExperience() && serverLevel.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) {

                awardBonusXp(serverLevel, entity.position(), entity.getExperienceReward(),  i.getAmplifier());
            }
        }
    }

    private static void awardBonusXp(ServerLevel serverLevel, Vec3 pos, int oldXp, int amp) {
        double xp = getExtraXp(oldXp, amp, serverLevel.random);
        if(xp != 0) {
            ExperienceOrb.award(serverLevel, pos, (int) xp);
        }
    }

    private static double getExtraXp(int oldXp, int amp, RandomSource random) {
        double value = oldXp * CommonConfigs.INVIGORATING_XP.get() * (amp + 1);

        int actual = (int) (value);
        double remainder = value - actual;
        if (remainder != 0 && random.nextFloat() < remainder) {
            actual++;
        }
        return actual;
    }

    public static int onBlockBreak(int i, Player player) {
        MobEffectInstance e = player.getEffect(SleepTight.INVIGORATING.get());
        if(e != null){
            return (int)getExtraXp(i, e.getAmplifier(), player.getRandom());
        }
        return 0;
    }
}
