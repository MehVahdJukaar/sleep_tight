package net.mehvahdjukaar.sleep_tight.mixins;

import net.mehvahdjukaar.sleep_tight.common.BedCapability;
import net.mehvahdjukaar.sleep_tight.common.ISleepTightBed;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BedBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;


@Mixin(BedBlockEntity.class)
public abstract class BedBlockEntityMixin extends BlockEntity implements ISleepTightBed {

    @Unique
    private final BedCapability bedCapability = new BedCapability();

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
        if (!bedCapability.isEmpty()) tag.put("sleep_tight_data", bedCapability.serializeNBT());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        var c = tag.getCompound("sleep_tight_data");
        if (c != null) this.bedCapability.deserializeNBT(c);
    }

    @Override
    public BedCapability getBedCap() {
        return bedCapability;
    }
}
