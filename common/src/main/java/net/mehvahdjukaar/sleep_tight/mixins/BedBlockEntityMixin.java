package net.mehvahdjukaar.sleep_tight.mixins;

import net.mehvahdjukaar.sleep_tight.core.BedData;
import net.mehvahdjukaar.sleep_tight.common.tiles.IExtraBedDataProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BedBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;


@Mixin(value = BedBlockEntity.class, priority = 1100)
public abstract class BedBlockEntityMixin extends BlockEntity implements IExtraBedDataProvider {

    @Unique
    private final BedData sleep_tight$bedCapability = new BedData();

    protected BedBlockEntityMixin(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
        super(blockEntityType, blockPos, blockState);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return this.saveWithoutMetadata();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (!sleep_tight$bedCapability.isEmpty()) tag.put("sleep_tight_data", sleep_tight$bedCapability.serializeNBT());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        var c = tag.getCompound("sleep_tight_data");
        this.sleep_tight$bedCapability.deserializeNBT(c);
    }

    @Override
    public BedData st_getBedData() {
        return sleep_tight$bedCapability;
    }
}
