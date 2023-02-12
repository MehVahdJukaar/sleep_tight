package net.mehvahdjukaar.sleep_tight.common;

import dev.architectury.injectables.annotations.PlatformOnly;
import net.mehvahdjukaar.sleep_tight.SleepTight;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class HammockBlockEntity extends BlockEntity {

    private final DyeColor color;

    //client stuff
    private float yaw;
    private float prevYaw;
    private float pivotOffset;
    private Direction.Axis axis;

    public HammockBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(SleepTight.HAMMOCK_TILE.get(), blockPos, blockState);
        this.color = ((HammockBlock) blockState.getBlock()).getColor();
    }

    public DyeColor getColor() {
        return color;
    }

    public float getYaw(float partialTicks) {
        return Mth.lerp(partialTicks, prevYaw, yaw);
    }

    public float getPivotOffset() {
        return pivotOffset;
    }

    public Direction.Axis getAxis() {
        return axis;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @PlatformOnly(PlatformOnly.FORGE)
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition.offset(-2.5, 0, -2.5), worldPosition.offset(2.5, 2, 2.5));
    }

    public static void tick(Level level, BlockPos pos, BlockState state, HammockBlockEntity e) {
        e.prevYaw = e.yaw;
        e.yaw+=0.1f;
        e.pivotOffset = state.getValue(HammockBlock.PART).getPivotOffset();
        e.axis = state.getValue(HammockBlock.FACING).getAxis();
    }


}
