package net.mehvahdjukaar.sleep_tight.common.tiles;

import net.mehvahdjukaar.moonlight.api.block.MimicBlockTile;
import net.mehvahdjukaar.moonlight.api.misc.ForgeOverride;
import net.mehvahdjukaar.sleep_tight.SleepTight;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

public class InfestedBedTile extends MimicBlockTile {

    @Nullable
    private BlockEntity innerTile = null;
    @Nullable
    private CompoundTag mobTag = null;

    public InfestedBedTile(BlockPos blockPos, BlockState blockState) {
        super(SleepTight.INFESTED_BED_TILE.get(), blockPos, blockState);
    }

    public void setBed(BlockState bed, @Nullable BlockEntity be){
        this.setHeldBlock(bed);
        if (be != null) {
            this.innerTile = be;
        }
    }

    @Nullable
    public BlockEntity getInner() {
        return innerTile;
    }

    @Override
    public boolean setHeldBlock(BlockState state, int index) {
        if (state.getBlock() instanceof EntityBlock eb && getBlockState().getValue(BedBlock.PART) == BedPart.HEAD) {
            innerTile = eb.newBlockEntity(this.worldPosition, state);
        }
        return super.setHeldBlock(state, index);
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (innerTile != null) {
            CompoundTag innerTag = innerTile.saveWithFullMetadata();
            tag.put("InnerTile", innerTag);
        }
        if (mobTag != null) {
            tag.put("bedbug", mobTag);
        }
    }


    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("InnerTile")) {
            innerTile = BlockEntity.loadStatic(this.worldPosition, this.mimic, tag.getCompound("InnerTile"));
        }
        if (tag.contains("bedbug")) {
            this.mobTag = tag.getCompound("bedbug");
        }
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.put("Mimic", NbtUtils.writeBlockState(mimic));
        return tag;
    }

    public BlockState getBed() {
        return mimic;
    }

    @ForgeOverride
    public AABB getRenderBoundingBox() {
        BlockPos pos = this.getBlockPos();
        return AABB.encapsulatingFullBlocks(pos.offset(-1, 0, -1), pos.offset(2, 2, 2));
    }
}
