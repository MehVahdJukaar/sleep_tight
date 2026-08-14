package net.mehvahdjukaar.sleep_tight.common.entities;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

//runs away like vanilla AnimalPanic, picking a new escape spot whenever it stops moving, so it scurries
//around instead of running to one place. only used when it can't fight back (peaceful) and has no bed.
//unlike AnimalPanic it stops as soon as it finds a bed, since running to a bed comes first
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
        // stops as soon as it finds a bed or can fight back again
        return !mob.hasBed() && level.getDifficulty() == Difficulty.PEACEFUL;
    }

    @Override
    protected void start(ServerLevel level, BedbugEntity mob, long gameTime) {
        // drop whatever we were heading to and repath fresh, away from the threat
        mob.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
    }

    @Override
    protected void tick(ServerLevel level, BedbugEntity mob, long gameTime) {
        // only pick a new spot once it stopped moving, like AnimalPanic does
        if (!mob.getNavigation().isDone()) return;
        Vec3 target = this.findFleePos(mob);
        if (target != null) {
            mob.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(target, this.speedModifier, 0));
        }
    }

    @Nullable
    private Vec3 findFleePos(BedbugEntity mob) {
        // runs away from the attacker while it still remembers it, random spot otherwise
        Vec3 awayFrom = mob.getBrain().getMemory(MemoryModuleType.HURT_BY_ENTITY)
                .map(LivingEntity::position).orElse(null);
        if (awayFrom != null) {
            Vec3 away = LandRandomPos.getPosAway(mob, FLEE_RADIUS_HORIZONTAL, FLEE_RADIUS_VERTICAL, awayFrom);
            if (away != null) return away;
        }
        return LandRandomPos.getPos(mob, FLEE_RADIUS_HORIZONTAL, FLEE_RADIUS_VERTICAL);
    }
}
