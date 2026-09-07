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

//walks to the HOME bed and burrows once on top of it. AcquirePoi does the finding and claiming
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

    //runs until the bed goes away, which tick handles
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

        //cant reach a bed in another dimension
        if (home.dimension() != level.dimension()) {
            forgetBed(mob);
            return;
        }

        BlockPos bed = home.pos();
        if (!BedbugEntity.isValidBedForInfestation(level.getBlockState(bed))) {
            forgetBed(mob);
            return;
        }

        //only burrows straight down, so it has to be standing on the bed
        BlockPos feet = mob.blockPosition();
        if (feet.equals(bed) || feet.equals(bed.above())) {
            brain.eraseMemory(MemoryModuleType.WALK_TARGET);
            mob.setBurrowing(true);
        } else {
            mob.setBurrowing(false);
            //0 so it walks onto the bed, not next to it
            brain.setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(bed, this.speedModifier, 0));
        }
    }

    @Override
    protected void stop(ServerLevel level, BedbugEntity mob, long gameTime) {
        mob.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        //entity tick clears burrowing once its off the bed, no need to do it here
    }

    private static void forgetBed(BedbugEntity mob) {
        mob.getBrain().eraseMemory(MemoryModuleType.HOME);
        mob.setBurrowing(false);
    }
}
