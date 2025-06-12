package net.mehvahdjukaar.sleep_tight.mixins;

import net.mehvahdjukaar.sleep_tight.common.tiles.IExtraBedDataProvider;
import net.mehvahdjukaar.sleep_tight.core.BedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BedBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.ApiStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;


@Mixin(value = BedBlockEntity.class, priority = 1100)
public abstract class BedBlockEntityMixin extends BlockEntity implements IExtraBedDataProvider {

    @Unique
    private BedData sleep_tight$bedData = new BedData();

    protected BedBlockEntityMixin(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
        super(blockEntityType, blockPos, blockState);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Override
    public BedData st_getBedData() {
        return sleep_tight$bedData;
    }

    @ApiStatus.Internal
    @Override
    public void st_setBedData(BedData data) {
        this.sleep_tight$bedData = data;
    }
}
