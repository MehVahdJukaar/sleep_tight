package net.mehvahdjukaar.sleep_tight.common.entities;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.AcquirePoi;
import net.minecraft.world.entity.ai.behavior.DoNothing;
import net.minecraft.world.entity.ai.behavior.LookAtTargetSink;
import net.minecraft.world.entity.ai.behavior.MeleeAttack;
import net.minecraft.world.entity.ai.behavior.MoveToTargetSink;
import net.minecraft.world.entity.ai.behavior.RandomStroll;
import net.minecraft.world.entity.ai.behavior.RunOne;
import net.minecraft.world.entity.ai.behavior.SetWalkTargetFromAttackTargetIfTargetOutOfReach;
import net.minecraft.world.entity.ai.behavior.StopAttackingIfTargetInvalid;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.schedule.Activity;

import java.util.Optional;

public class BedbugAi {

    private static final float SPEED_WHEN_GOING_TO_BED = 1.15F;
    private static final float SPEED_WHEN_SEARCHING = 0.6F;
    private static final float SPEED_WHEN_FIGHTING = 1.0F;
    // pretty fast, but never faster than its dash for a bed (which keeps priority over panic)
    private static final float SPEED_WHEN_PANICKING = 1.1F;
    private static final int MELEE_COOLDOWN = 20;

    private BedbugAi() {
    }

    public static Brain<?> makeBrain(Brain<BedbugEntity> brain) {
        initCoreActivity(brain);
        initIdleActivity(brain);
        initFightActivity(brain);
        brain.setCoreActivities(ImmutableSet.of(Activity.CORE));
        brain.setDefaultActivity(Activity.IDLE);
        brain.useDefaultActivity();
        return brain;
    }

    private static void initCoreActivity(Brain<BedbugEntity> brain) {
        brain.addActivity(Activity.CORE, 0, ImmutableList.of(
                new LookAtTargetSink(45, 90),
                new MoveToTargetSink(),
                AcquirePoi.create(holder -> holder.is(PoiTypes.HOME), MemoryModuleType.HOME, false, Optional.empty())));
    }

    private static void initIdleActivity(Brain<BedbugEntity> brain) {
        brain.addActivity(Activity.IDLE, 10, ImmutableList.of(
                new InfestBedBehavior(SPEED_WHEN_GOING_TO_BED),
                new BedbugPanicBehavior(SPEED_WHEN_PANICKING),
                //no bed yet, wander around looking for one
                new RunOne<>(ImmutableList.of(
                        Pair.of(RandomStroll.stroll(SPEED_WHEN_SEARCHING), 2),
                        Pair.of(new DoNothing(30, 60), 1)))));
    }

    private static void initFightActivity(Brain<BedbugEntity> brain) {
        brain.addActivityAndRemoveMemoriesWhenStopped(
                Activity.FIGHT,
                ImmutableList.of(
                        Pair.of(0, SetWalkTargetFromAttackTargetIfTargetOutOfReach.create(SPEED_WHEN_FIGHTING)),
                        Pair.of(1, MeleeAttack.create(MELEE_COOLDOWN)),
                        Pair.of(2, StopAttackingIfTargetInvalid.create())),
                ImmutableSet.of(
                        Pair.of(MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT),
                        Pair.of(MemoryModuleType.HOME, MemoryStatus.VALUE_ABSENT)),
                ImmutableSet.of(MemoryModuleType.ATTACK_TARGET));
    }

    public static void updateActivity(BedbugEntity bedbug) {
        Brain<BedbugEntity> brain = bedbug.getBrain();
        if (bedbug.level().getDifficulty() == Difficulty.PEACEFUL) {
            brain.eraseMemory(MemoryModuleType.ATTACK_TARGET);
            bedbug.setAggressive(false);
        }
        brain.setActiveActivityToFirstValid(ImmutableList.of(Activity.FIGHT, Activity.IDLE));
        bedbug.setAggressive(bedbug.level().getDifficulty() != Difficulty.PEACEFUL
                && brain.isActive(Activity.FIGHT)
                && brain.hasMemoryValue(MemoryModuleType.ATTACK_TARGET));
    }
}
