package net.mehvahdjukaar.sleep_tight.integration;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.violetmoon.quark.content.building.block.WoodPostBlock;

public class QuarkCompat {

    public static boolean isVerticalPost(BlockState facingState) {
        return facingState.getBlock() instanceof WoodPostBlock && facingState.getValue(WoodPostBlock.AXIS) == Direction.Axis.Y;
    }
}
