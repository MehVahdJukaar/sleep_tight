package net.mehvahdjukaar.sleep_tight.common.tiles;

import net.mehvahdjukaar.sleep_tight.SleepTight;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class CompatBedTile extends BlockEntity {
    public CompatBedTile(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
        super(blockEntityType, blockPos, blockState);
    }

    public CompatBedTile(BlockPos blockPos, BlockState blockState) {
        this(SleepTight.COMPAT_BED_TILE.get(), blockPos, blockState);
    }
}
