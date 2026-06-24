package net.mehvahdjukaar.sleep_tight.common.entities;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Peaceful-only flee, modelled on vanilla {@link net.minecraft.world.entity.ai.behavior.AnimalPanic}: once it
 * starts, the bug keeps re-picking an escape position every time its navigation goes idle, for a short panic
 * window, so it bolts around like a scared animal rather than dashing to a single spot.
 * <p>
 * It only kicks in when the bug genuinely can't fight back (peaceful difficulty wipes its {@code ATTACK_TARGET},
 * see {@link BedbugAi#updateActivity}) and has no bed to run to. Unlike vanilla {@code AnimalPanic} its
 * {@link #canStillUse} bails the instant a bed appears ({@link MemoryModuleType#HOME} present), handing control
 * straight back to {@link InfestBedBehavior} — reaching a bed always wins over panicking. Flee speed is passed
 * in and kept at or below the run-to-bed speed.
 */
public class BedbugPanicBehavior extends Behavior<BedbugEntity> {

    // matches AnimalPanic's panic window so it feels the same
    private static final int PANIC_MIN_DURATION = 100;
    private static final int PANIC_MAX_DURATION = 120;
    private static final int FLEE_RADIUS_HORIZONTAL = 16;
    private static final int FLEE_RADIUS_VERTICAL = 7;

    private final float speedModifier;

    public BedbugPanicBehavior(float speedModifier) {
        super(ImmutableMap.of(
                // no bed to flee to -> bed-seeking (InfestBedBehavior) has priority and runs instead
                MemoryModuleType.HOME, MemoryStatus.VALUE_ABSENT,
                // populated by the HurtBySensor when something attacks us
                MemoryModuleType.HURT_BY_ENTITY, MemoryStatus.VALUE_PRESENT),
                PANIC_MIN_DURATION, PANIC_MAX_DURATION);
        this.speedModifier = speedModifier;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, BedbugEntity mob) {
        // only when it genuinely can't retaliate
        return level.getDifficulty() == Difficulty.PEACEFUL;
    }

    @Override
    protected boolean canStillUse(ServerLevel level, BedbugEntity mob, long gameTime) {
        // keep panicking for the duration, but stop the moment a bed shows up so InfestBedBehavior can take
        // over, or if we're no longer in peaceful (can fight back again).
        return !mob.hasBed() && level.getDifficulty() == Difficulty.PEACEFUL;
    }

    @Override
    protected void start(ServerLevel level, BedbugEntity mob, long gameTime) {
        // drop whatever we were heading to and repath fresh, away from the threat
        mob.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
    }

    @Override
    protected void tick(ServerLevel level, BedbugEntity mob, long gameTime) {
        // re-pick only once we've actually arrived/stalled, like AnimalPanic, so the run stays smooth
        if (!mob.getNavigation().isDone()) return;
        Vec3 target = this.findFleePos(mob);
        if (target != null) {
            mob.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(target, this.speedModifier, 0));
        }
    }

    @Nullable
    private Vec3 findFleePos(BedbugEntity mob) {
        // run directly away from the attacker while we still remember it; once that memory lapses, fall back
        // to AnimalPanic's plain random scurry.
        Vec3 awayFrom = mob.getBrain().getMemory(MemoryModuleType.HURT_BY_ENTITY)
                .map(net.minecraft.world.entity.LivingEntity::position).orElse(null);
        if (awayFrom != null) {
            Vec3 away = LandRandomPos.getPosAway(mob, FLEE_RADIUS_HORIZONTAL, FLEE_RADIUS_VERTICAL, awayFrom);
            if (away != null) return away;
        }
        return LandRandomPos.getPos(mob, FLEE_RADIUS_HORIZONTAL, FLEE_RADIUS_VERTICAL);
    }
}
