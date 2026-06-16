package net.mehvahdjukaar.sleep_tight.common.entities;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
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
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.schedule.Activity;

import java.util.Optional;

/**
 * Brain wiring for {@link BedbugEntity}, kept out of the entity in the vanilla {@code GoatAi}/{@code PiglinAi} style.
 * <p>
 * The bedbug is timid: while it remembers a bed ({@link MemoryModuleType#HOME}) it always runs for it and never
 * fights, even while being hit. Only when it has no bed to flee to can a provoked bedbug enter {@link Activity#FIGHT}.
 */
public class BedbugAi {

    private static final float SPEED_WHEN_GOING_TO_BED = 1.15F;
    private static final float SPEED_WHEN_SEARCHING = 0.6F;
    private static final float SPEED_WHEN_FIGHTING = 1.0F;
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
                new MoveToTargetSink()));
    }

    private static void initIdleActivity(Brain<BedbugEntity> brain) {
        brain.addActivity(Activity.IDLE, 10, ImmutableList.of(
                // find + claim (POI ticket) the nearest reachable bed, remembered as HOME
                AcquirePoi.create(holder -> holder.is(PoiTypes.HOME), MemoryModuleType.HOME, false, Optional.empty()),
                // make a run for the remembered bed and burrow into it (faster than the search wander)
                new InfestBedBehavior(SPEED_WHEN_GOING_TO_BED),
                // no bed yet: calmly wander in search of one (only runs while WALK_TARGET is unset)
                new RunOne<>(ImmutableList.of(
                        Pair.of(RandomStroll.stroll(SPEED_WHEN_SEARCHING), 2),
                        Pair.of(new DoNothing(30, 60), 1)))));
    }

    private static void initFightActivity(Brain<BedbugEntity> brain) {
        brain.addActivityAndRemoveMemoryWhenStopped(Activity.FIGHT, 10, ImmutableList.of(
                SetWalkTargetFromAttackTargetIfTargetOutOfReach.create(SPEED_WHEN_FIGHTING),
                MeleeAttack.create(MELEE_COOLDOWN),
                StopAttackingIfTargetInvalid.create()), MemoryModuleType.ATTACK_TARGET);
    }

    public static void updateActivity(BedbugEntity bedbug) {
        Brain<BedbugEntity> brain = bedbug.getBrain();
        // Timid: a bedbug with a bed always runs for it and never fights, even while being hit.
        // It only enters FIGHT when it has no bed to flee to (and was provoked).
        if (bedbug.hasBed()) {
            brain.setActiveActivityToFirstValid(ImmutableList.of(Activity.IDLE));
        } else {
            brain.setActiveActivityToFirstValid(ImmutableList.of(Activity.FIGHT, Activity.IDLE));
        }
        bedbug.setAggressive(brain.isActive(Activity.FIGHT)
                && brain.hasMemoryValue(MemoryModuleType.ATTACK_TARGET));
    }
}
