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
import net.minecraft.world.entity.ai.behavior.Swim;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.schedule.Activity;

import java.util.Optional;

//bedbugs are timid: as long as they remember a bed they run for it and won't fight back even while hit.
//only a bedless one that got hurt will fight
public class BedbugAi {

    private static final ImmutableList<? extends SensorType<? extends Sensor<? super BedbugEntity>>> SENSOR_TYPES =
            ImmutableList.of(SensorType.NEAREST_LIVING_ENTITIES, SensorType.NEAREST_PLAYERS, SensorType.HURT_BY);
    private static final ImmutableList<? extends MemoryModuleType<?>> MEMORY_TYPES = ImmutableList.of(
            MemoryModuleType.HOME,
            MemoryModuleType.NEAREST_LIVING_ENTITIES,
            MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES,
            MemoryModuleType.NEAREST_VISIBLE_PLAYER,
            MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER,
            MemoryModuleType.LOOK_TARGET,
            MemoryModuleType.WALK_TARGET,
            MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE,
            MemoryModuleType.PATH,
            MemoryModuleType.ATTACK_TARGET,
            MemoryModuleType.ATTACK_COOLING_DOWN,
            MemoryModuleType.HURT_BY,
            MemoryModuleType.HURT_BY_ENTITY);

    private static final float SPEED_WHEN_GOING_TO_BED = 1.15F;
    private static final float SPEED_WHEN_SEARCHING = 0.6F;
    private static final float SPEED_WHEN_FIGHTING = 1.0F;
    // pretty fast, but never faster than its dash for a bed (which keeps priority over panic)
    private static final float SPEED_WHEN_PANICKING = 1.1F;
    private static final int MELEE_COOLDOWN = 20;

    public static Brain.Provider<BedbugEntity> brainProvider() {
        return Brain.provider(MEMORY_TYPES, SENSOR_TYPES);
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
                new Swim(0.8F),
                new LookAtTargetSink(45, 90),
                new MoveToTargetSink(),
                // claims the closest bed it can reach and remembers it as HOME. sits in CORE so it keeps looking
                // while fighting too, and finding a bed mid fight makes it break off and run there
                AcquirePoi.create(holder -> holder.is(PoiTypes.HOME), MemoryModuleType.HOME, false, Optional.empty())));
    }

    private static void initIdleActivity(Brain<BedbugEntity> brain) {
        brain.addActivity(Activity.IDLE, 10, ImmutableList.of(
                // make a run for the remembered bed and burrow into it (faster than the search wander)
                new InfestBedBehavior(SPEED_WHEN_GOING_TO_BED),
                // no bed and can't fight back, so run from whatever hit us. after InfestBedBehavior and
                // only with no HOME, so going for a bed always wins
                new BedbugPanicBehavior(SPEED_WHEN_PANICKING),
                // no bed known yet, wander around looking for one
                new RunOne<>(ImmutableList.of(
                        Pair.of(RandomStroll.stroll(SPEED_WHEN_SEARCHING), 2),
                        Pair.of(new DoNothing(30, 60), 1)))));
    }

    private static void initFightActivity(Brain<BedbugEntity> brain) {
        // only fights if something attacked it and it has no bed to run to
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
