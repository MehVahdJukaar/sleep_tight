package net.mehvahdjukaar.sleep_tight.mixins;

import net.mehvahdjukaar.sleep_tight.common.tiles.IExtraBedDataProvider;
import net.mehvahdjukaar.sleep_tight.core.BedData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.entity.BedBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(value = BedBlockEntity.class, priority = 1100)
public abstract class BedBlockEntityMixin extends BlockEntity implements IExtraBedDataProvider {

    @Unique
    private BedData sleep_tight$bedData;

    public BedBlockEntityMixin(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
        super(blockEntityType, blockPos, blockState);
    }

    @Inject(method = "<init>(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/item/DyeColor;)V", at = @At("RETURN"))
    public void onInit(BlockPos blockPos, BlockState blockState, DyeColor dyeColor, CallbackInfo ci) {
        if (blockState.getValue(BedBlock.PART) == BedPart.HEAD) {
            sleep_tight$bedData = new BedData();
        } else sleep_tight$bedData = null;
    }

    @Inject(method = "<init>(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V", at = @At("RETURN"))
    public void onInit(BlockPos blockPos, BlockState blockState, CallbackInfo ci) {
        if (blockState.getValue(BedBlock.PART) == BedPart.HEAD) {
            sleep_tight$bedData = new BedData();
        } else sleep_tight$bedData = null;
    }

    @Override
    public CompoundTag getUpdateTag() {
        return this.saveWithoutMetadata();
    }

    @Nullable
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
