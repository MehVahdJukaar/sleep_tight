package net.mehvahdjukaar.sleep_tight.common;

import dev.architectury.injectables.annotations.PlatformOnly;
import net.mehvahdjukaar.sleep_tight.SleepTight;
import net.mehvahdjukaar.sleep_tight.configs.ClientConfigs;
import net.mehvahdjukaar.sleep_tight.network.AccelerateHammockMessage;
import net.mehvahdjukaar.sleep_tight.network.NetworkHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class HammockBlockEntity extends BlockEntity {

    private final DyeColor color;

    private boolean accelerateLeft;
    private boolean accelerateRight;

    //client stuff
    private float pivotOffset;
    private Direction direction;

    private float prevYaw;
    private float angle;
    private float angularVel = 0.1f;
    private boolean hasDrag = true;


    public HammockBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(SleepTight.HAMMOCK_TILE.get(), blockPos, blockState);
        this.color = ((HammockBlock) blockState.getBlock()).getColor();
        this.pivotOffset = blockState.getValue(HammockBlock.PART).getPivotOffset();
        this.direction = blockState.getValue(HammockBlock.FACING);
        this.angle = RandomSource.create().nextFloat();
    }


    private static double calculateEnergy(float k, float vel, float angle) {
        return (1 - Mth.cos(angle)) * k + 0.5 * (vel * vel);
    }

    public DyeColor getColor() {
        return color;
    }

    public float getRoll(float partialTicks) {
        return (180 / Mth.PI) * Mth.rotLerp(partialTicks, prevYaw, angle);
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

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, HammockBlockEntity e) {
        e.prevYaw = e.angle;

        double damping = ClientConfigs.DAMPING.get();
        float dt = 1 / 20f;

        double energy = 0;

        float k = ClientConfigs.getK();

        boolean hasAcc = (e.accelerateLeft || e.accelerateRight);
        if (hasAcc) e.hasDrag = true;
        if (hasAcc || e.hasDrag) energy = calculateEnergy(k, e.angularVel, e.angle);


        if (hasAcc && energy < ClientConfigs.getMaxAngleEnergy()) {
            double dec = ClientConfigs.SWING_FORCE.get();
            e.angularVel += dec * (e.accelerateLeft ? -1 : 1);

            //update other clients
            NetworkHandler.CHANNEL.sendToServer(new AccelerateHammockMessage(pos, e.accelerateLeft));
        }

        float acc = -k * Mth.sin(e.angle);

        if (e.hasDrag) {
            if (energy > ClientConfigs.getMinAngleEnergy()) {
                float drag = (float) (-damping * e.angularVel);
                acc += drag;
            } else {
                e.hasDrag = false;
            }
        }

        e.angularVel += dt * acc;

        e.angle += (e.angularVel * dt);

        //float max_yaw = max_swing_angle(self.yaw, self.angular_velocity, ff)


        e.pivotOffset = state.getValue(HammockBlock.PART).getPivotOffset();
        e.direction = state.getValue(HammockBlock.FACING);


        e.accelerateLeft = false;
        e.accelerateRight = false;
    }

    public void accelerateLeft() {
        this.accelerateLeft = true;
    }

    public void accelerateRight() {
        this.accelerateRight = true;
    }

    public void addImpulse(double vel) {
        this.angularVel += vel;
        this.hasDrag = true;
    }
}
