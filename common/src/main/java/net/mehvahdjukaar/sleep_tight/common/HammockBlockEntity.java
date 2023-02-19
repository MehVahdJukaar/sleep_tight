package net.mehvahdjukaar.sleep_tight.common;

import dev.architectury.injectables.annotations.PlatformOnly;
import net.mehvahdjukaar.sleep_tight.SleepTight;
import net.mehvahdjukaar.sleep_tight.configs.ClientConfigs;
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

    private final float maxAngle;
    private final float minAngle;
    private final float k;

    private float pivotOffset;
    private Direction direction;

    private boolean accelerating;
    private boolean decelerating;
    private float prevYaw;
    private float yaw = 1;
    private float angularVel = 0.1f;

    public HammockBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(SleepTight.HAMMOCK_TILE.get(), blockPos, blockState);
        this.color = ((HammockBlock) blockState.getBlock()).getColor();
        this.pivotOffset = blockState.getValue(HammockBlock.PART).getPivotOffset();
        this.direction = blockState.getValue(HammockBlock.FACING);

        float period = ClientConfigs.;
    }

    public DyeColor getColor() {
        return color;
    }

    public float gerRoll(float partialTicks) {
        return (180 / Mth.PI) * Mth.rotLerp(partialTicks, prevYaw, yaw);//Mth.lerp(partialTicks, prevAmpl, ampl) * Mth.sin((tickCount + partialTicks) * 0.08f);
    }

    public float getPivotOffset() {
        return pivotOffset;
    }

    public Direction getDirection() {
        return direction;
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


        float damping = 0.0f;
        float dt = 0.1f;
        float freq = 1;

        if (e.accelerating || e.decelerating) {
            double dec = 0.1;
            e.angularVel += dec * ((e.angularVel > 0 ^ e.decelerating) ? 1 : -1);
        }

        float drag = -damping * e.angularVel;

        float k = (float) Math.pow(2 * Math.PI * 0.125, 2);

        float acc = -k * Mth.sin(e.yaw) + drag;

        e.angularVel += dt * acc;

        e.yaw += (e.angularVel * dt);

        //float max_yaw = max_swing_angle(self.yaw, self.angular_velocity, ff)


        e.pivotOffset = state.getValue(HammockBlock.PART).getPivotOffset();
        e.direction = state.getValue(HammockBlock.FACING);


        e.accelerating = false;
        e.decelerating = false;
    }


    public void accelerate() {
        this.accelerating = true;
    }

    public void decelerate() {
        this.decelerating = true;
    }

}
