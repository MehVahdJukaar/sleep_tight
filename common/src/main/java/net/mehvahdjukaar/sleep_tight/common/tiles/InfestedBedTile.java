package net.mehvahdjukaar.sleep_tight.common.tiles;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.moonlight.api.block.MimicBlockTile;
import net.mehvahdjukaar.sleep_tight.SleepTight;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import org.jetbrains.annotations.Nullable;

public class InfestedBedTile extends MimicBlockTile {

    @Nullable
    private BlockEntity innerTile = null;
    @Nullable
    private CompoundTag mobTag = null;

    public InfestedBedTile(BlockPos blockPos, BlockState blockState) {
        super(SleepTight.INFESTED_BED_TILE.get(), blockPos, blockState);
        mimic = Blocks.RED_BED.defaultBlockState();
    }

    @Nullable
    public BlockEntity getInner() {
        if (innerTile == null && mimic.getBlock() instanceof EntityBlock eb && getBlockState().getValue(BedBlock.PART) == BedPart.HEAD) {
            innerTile = eb.newBlockEntity(worldPosition, mimic);
        }
        return innerTile;
    }

    @Override
    public boolean setHeldBlock(BlockState state, int index) {
        if (state.getBlock() instanceof EntityBlock eb && getBlockState().getValue(BedBlock.PART) == BedPart.HEAD) {
            innerTile = eb.newBlockEntity(this.worldPosition, state);
        }
        var r = super.setHeldBlock(state, index);

        return r;
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (mobTag != null) {
            tag.put("bedbug", mobTag);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("bedbug")) {
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
        tag.put("Mimic", NbtUtils.writeBlockState(mimic));
        return tag;
    }


    public BlockState getBed() {
        return mimic;
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
