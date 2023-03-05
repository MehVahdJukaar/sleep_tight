package net.mehvahdjukaar.sleep_tight.common;

import net.mehvahdjukaar.sleep_tight.SleepTight;
import net.mehvahdjukaar.sleep_tight.core.BedData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class InfestedBedTile extends BlockEntity {

    private DyeColor color = DyeColor.WHITE;

    public InfestedBedTile(BlockPos blockPos, BlockState blockState) {
        super(SleepTight.INFESTED_BED_TILE.get(), blockPos, blockState);
    }

    public DyeColor getColor() {
        return this.color;
    }

    public void setColor(DyeColor color) {
        this.color = color;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("color", this.color.ordinal());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.color = DyeColor.values() [tag.getInt("color")];
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
