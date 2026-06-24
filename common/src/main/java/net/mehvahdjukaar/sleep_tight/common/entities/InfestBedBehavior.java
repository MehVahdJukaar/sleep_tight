package net.mehvahdjukaar.sleep_tight.common.entities;

import com.google.common.collect.ImmutableMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;

/**
 * Drives a bedbug to its remembered bed ({@link MemoryModuleType#HOME}) and starts burrowing once it is
 * actually standing on a bed block. Replaces the old {@code InfestBedGoal}: acquisition + ticket claiming is
 * handled upstream by {@code AcquirePoi}, this behavior only handles the "walk there and burrow" part.
 */
public class InfestBedBehavior extends Behavior<BedbugEntity> {

    private final float speedModifier;

    public InfestBedBehavior(float speedModifier) {
        super(ImmutableMap.of(
                MemoryModuleType.HOME, MemoryStatus.VALUE_PRESENT,
                MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED));
        this.speedModifier = speedModifier;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, BedbugEntity mob) {
        return mob.getBrain().hasMemoryValue(MemoryModuleType.HOME);
    }

    // run indefinitely while we still have a valid bed; invalidation is handled in tick()
    @Override
    protected boolean timedOut(long gameTime) {
        return false;
    }

    @Override
    protected boolean canStillUse(ServerLevel level, BedbugEntity mob, long gameTime) {
        return mob.getBrain().hasMemoryValue(MemoryModuleType.HOME);
    }

    @Override
    protected void tick(ServerLevel level, BedbugEntity mob, long gameTime) {
        Brain<BedbugEntity> brain = mob.getBrain();
        GlobalPos home = brain.getMemory(MemoryModuleType.HOME).orElse(null);
        if (home == null) return;

        // bed in another dimension can never be reached: forget it so AcquirePoi finds a new one
        if (home.dimension() != level.dimension()) {
            forgetBed(mob);
            return;
        }

        BlockPos bed = home.pos();
        if (!BedbugEntity.isValidBedForInfestation(level.getBlockState(bed))) {
            forgetBed(mob);
            return;
        }

        // burrow only while standing on top of the bed (its feet block, or one above with the bed
        // directly below): the bug buries straight down into the bed it stands on, not from beside it.
        BlockPos feet = mob.blockPosition();
        if (feet.equals(bed) || feet.equals(bed.above())) {
            brain.eraseMemory(MemoryModuleType.WALK_TARGET);
            mob.setBurrowing(true);
        } else {
            mob.setBurrowing(false);
            // closeEnoughDist 0: keep walking right onto the bed, not merely next to it
            brain.setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(bed, this.speedModifier, 0));
        }
    }

    @Override
    protected void stop(ServerLevel level, BedbugEntity mob, long gameTime) {
        mob.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        // don't force-clear burrowing here: the entity tick clears it when no longer on a bed,
        // so an idle->fight handoff that walks the bug off the bed cleans up naturally.
    }

    private static void forgetBed(BedbugEntity mob) {
        mob.getBrain().eraseMemory(MemoryModuleType.HOME);
        mob.setBurrowing(false);
    }
}
