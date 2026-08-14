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

//walks the bug to the bed it remembers and makes it burrow once it's standing on one.
//finding and claiming the bed is AcquirePoi's job, this is just the walking and burrowing part
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

    // keeps going as long as it has a bed, tick() is what drops it
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

        // only burrows while standing on the bed, since it digs straight down
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
        // no need to clear burrowing, the entity tick does that once it's off the bed
    }

    private static void forgetBed(BedbugEntity mob) {
        mob.getBrain().eraseMemory(MemoryModuleType.HOME);
        mob.setBurrowing(false);
    }
}
