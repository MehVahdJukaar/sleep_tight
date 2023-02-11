package net.mehvahdjukaar.sleep_tight.integration;

import net.mehvahdjukaar.supplementaries.common.block.ModBlockProperties;
import net.mehvahdjukaar.supplementaries.common.block.blocks.RopeBlock;
import net.mehvahdjukaar.supplementaries.common.block.blocks.RopeKnotBlock;
import net.mehvahdjukaar.supplementaries.reg.ModRegistry;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public class SupplementariesCompat {
    public static boolean isRopeKnot(BlockState state) {
        return state.is(ModRegistry.ROPE.get()) ||
                state.is(ModRegistry.ROPE_KNOT.get()) &&
                state.getValue(RopeKnotBlock.AXIS) == Direction.Axis.Y &&
                state.getValue(RopeKnotBlock.POST_TYPE) == ModBlockProperties.PostType.POST;
    }

    public static boolean isRope(BlockState state, Direction face){
        return state.is(ModRegistry.ROPE.get());
    }
}
