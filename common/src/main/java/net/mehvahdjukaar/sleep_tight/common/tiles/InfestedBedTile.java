package net.mehvahdjukaar.sleep_tight.common.tiles;

import net.mehvahdjukaar.moonlight.api.block.MimicBlockTile;
import net.mehvahdjukaar.moonlight.api.client.model.ExtraModelData;
import net.mehvahdjukaar.moonlight.api.client.model.IExtraModelDataProvider;
import net.mehvahdjukaar.moonlight.api.set.BlocksColorAPI;
import net.mehvahdjukaar.sleep_tight.SleepTight;
import net.mehvahdjukaar.sleep_tight.core.BedData;
import net.mehvahdjukaar.supplementaries.Supplementaries;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.entity.animal.Bucketable;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class InfestedBedTile extends BlockEntity implements IExtraModelDataProvider {

    private DyeColor color = DyeColor.WHITE;
    private BlockState bed = Blocks.WHITE_BED.defaultBlockState();
    @Nullable
    private CompoundTag mobTag = null;

    public InfestedBedTile(BlockPos blockPos, BlockState blockState) {
        super(SleepTight.INFESTED_BED_TILE.get(), blockPos, blockState);
    }

    public DyeColor getColor() {
        return this.color;
    }

    public void setColor(DyeColor color) {
        this.color = color;
        this.bed = BlocksColorAPI.getColoredBlock("bed",color).defaultBlockState();
    }

    @Override
    public ExtraModelData getExtraModelData() {
        return ExtraModelData.builder()
                .with(MimicBlockTile.MIMIC, bed)
                .build();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("color", this.color.ordinal());
        if(mobTag != null){
            tag.put("bedbug", mobTag);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.setColor(DyeColor.values() [tag.getInt("color")]);
        if(tag.contains("bedbug")){
            this.mobTag = tag.getCompound("bedbug");
        }
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("color", this.color.ordinal());
        return tag;
    }

    public BlockState getBed() {
        return bed;
    }

    private static CompoundTag prepareMobTagForContainer(Entity entity, double yOffset) {
        //set post relative to center block cage
        double px = 0.5;
        double py = yOffset + 0.0001;
        double pz = 0.5;
        entity.setPos(px, py, pz);
        entity.xOld = px;
        entity.yOld = py;
        entity.zOld = pz;

        if (entity.isPassenger()) {
            entity.getVehicle().ejectPassengers();
        }

        //prepares entity
        if (entity instanceof LivingEntity le) {
            le.yHeadRotO = 0;
            le.yHeadRot = 0;
            le.animationSpeed = 0;
            le.animationSpeedOld = 0;
            le.animationPosition = 0;
            le.hurtDuration = 0;
            le.hurtTime = 0;
            le.attackAnim = 0;
        }
        entity.setYRot(0);
        entity.yRotO = 0;
        entity.xRotO = 0;
        entity.setXRot(0);
        entity.clearFire();
        entity.invulnerableTime = 0;

        CompoundTag mobTag = new CompoundTag();
        entity.save(mobTag);
        if (mobTag.isEmpty()) {
            return null;
        }
        mobTag.remove("Passengers");
        mobTag.remove("Leash");
        mobTag.remove("UUID");//TODO: UUID
        return mobTag;
    }
}
