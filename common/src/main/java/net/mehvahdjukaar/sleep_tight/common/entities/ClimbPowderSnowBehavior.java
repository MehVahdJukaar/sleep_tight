package net.mehvahdjukaar.sleep_tight.common.entities;

import com.google.common.collect.ImmutableMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;

//brain version of vanilla ClimbOnTopOfPowderSnowGoal, which only exists as a goal
public class ClimbPowderSnowBehavior extends Behavior<BedbugEntity> {

    public ClimbPowderSnowBehavior() {
        super(ImmutableMap.of());
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, BedbugEntity mob) {
        if (!mob.wasInPowderSnow && !mob.isInPowderSnow) return false;
        BlockPos above = mob.blockPosition().above();
        BlockState state = level.getBlockState(above);
        return state.is(Blocks.POWDER_SNOW) || state.getCollisionShape(level, above) == Shapes.empty();
    }

    @Override
    protected boolean canStillUse(ServerLevel level, BedbugEntity mob, long gameTime) {
        return this.checkExtraStartConditions(level, mob);
    }

    @Override
    protected void tick(ServerLevel level, BedbugEntity mob, long gameTime) {
        mob.getJumpControl().jump();
    }
}
