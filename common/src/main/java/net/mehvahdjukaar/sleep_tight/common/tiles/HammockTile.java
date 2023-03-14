package net.mehvahdjukaar.sleep_tight.common.tiles;

import dev.architectury.injectables.annotations.PlatformOnly;
import net.mehvahdjukaar.sleep_tight.SleepTight;
import net.mehvahdjukaar.sleep_tight.SleepTightClient;
import net.mehvahdjukaar.sleep_tight.common.blocks.HammockBlock;
import net.mehvahdjukaar.sleep_tight.common.entities.BedEntity;
import net.mehvahdjukaar.sleep_tight.common.network.AccelerateHammockMessage;
import net.mehvahdjukaar.sleep_tight.common.network.NetworkHandler;
import net.mehvahdjukaar.sleep_tight.common.network.ServerBoundFallFromHammockMessage;
import net.mehvahdjukaar.sleep_tight.configs.ClientConfigs;
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

public class HammockTile extends BlockEntity {

    private final DyeColor color;

    private boolean accelerateLeft;
    private boolean accelerateRight;

    //client stuff
    private float pivotOffset;
    private Direction direction;

    private float prevYaw;
    private float angle;
    private float angularVel = 0f;
    private boolean hasDrag = true;


    public HammockTile(BlockPos blockPos, BlockState blockState) {
        super(SleepTight.HAMMOCK_TILE.get(), blockPos, blockState);
        this.color = ((HammockBlock) blockState.getBlock()).getColor();
        this.pivotOffset = blockState.getValue(HammockBlock.PART).getPivotOffset();
        this.direction = blockState.getValue(HammockBlock.FACING);
        this.angle = 0 * (float) ((RandomSource.create().nextFloat() - 0.5) * Math.toRadians(ClientConfigs.HAMMOCK_MIN_ANGLE.get()));
    }


    private static double calculateEnergy(float k, float vel, float angle) {
        return (1 - Mth.cos(angle)) * k + 0.5 * (vel * vel);
    }

    public DyeColor getColor() {
        return color;
    }

    public float getRoll(float partialTicks) {
        if (!ClientConfigs.HAMMOCK_ANIMATION.get()) return 0;
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

    public static void tick(Level level, BlockPos pos, BlockState state, HammockTile e) {
        e.prevYaw = e.angle;

        float dt = 1 / 20f; //time step

        double energy = 0;

        float k = ClientConfigs.getK();

        boolean hasAcc = (e.accelerateLeft || e.accelerateRight);
        if (hasAcc) e.hasDrag = true;
        if (e.hasDrag) energy = calculateEnergy(k, e.angularVel, e.angle);


        if (hasAcc && energy < ClientConfigs.getMaxAngleEnergy()) {
            double push = ClientConfigs.SWING_FORCE.get();
            e.angularVel += (push * (e.accelerateLeft ? -1 : 1));

            //update other clients
            NetworkHandler.CHANNEL.sendToServer(new AccelerateHammockMessage(pos, e.accelerateLeft));
        }

        float acc = -k * Mth.sin(e.angle);

        if (e.hasDrag && !hasAcc) {
            //note that since its proportional to speed this effectively limits the max angle
            if (energy > ClientConfigs.getMinAngleEnergy()) {
                double damping = ClientConfigs.DAMPING.get();

                float drag = (float) (damping * e.angularVel);

                acc -= drag;
            } else {
                e.hasDrag = false;
            }
        }

        /* //more precise method
        float k1v, k2v, k3v =0;
        k1v =  e.angularVel;

        float  k1a =  -k * Mth.sin(e.angle);

        k2v = e.angularVel + 0.5f * dt * k1a;
        float   k2a =  -k * Mth.sin(e.angle + 0.5f * dt * k1v);

        k3v = e.angularVel + dt * k2a;
        float  k3a = -k * Mth.sin(e.angle + dt * k2v);

        e.angle += (dt / 4.0) * (k1v + 2.0 * k2v + k3v);
        e.angularVel += (dt / 4.0) * (k1a + 2 * k2a + k3a);
        */

        e.angularVel += dt * acc;

        e.angle += (e.angularVel * dt);


        //float max_yaw = max_swing_angle(self.yaw, self.angular_velocity, ff)


        e.pivotOffset = state.getValue(HammockBlock.PART).getPivotOffset();
        e.direction = state.getValue(HammockBlock.FACING);


        e.accelerateLeft = false;
        e.accelerateRight = false;

        //client is in charge here. Therefore they are only in charge of their own player
        if (Mth.abs(e.angle) > 0.2+ Mth.PI / 2) {
            for (var b : e.level.getEntitiesOfClass(BedEntity.class, new AABB(pos))) {
                for (var p : b.getPassengers()) {
                    if (p == SleepTightClient.getPlayer()) {
                        NetworkHandler.CHANNEL.sendToServer(new ServerBoundFallFromHammockMessage());
                    } else return;
                }
            }
        }
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
