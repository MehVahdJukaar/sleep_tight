package net.mehvahdjukaar.sleep_tight.common.tiles;

import com.mojang.math.Vector3f;
import dev.architectury.injectables.annotations.PlatformOnly;
import net.mehvahdjukaar.moonlight.api.platform.PlatformHelper;
import net.mehvahdjukaar.sleep_tight.SleepTight;
import net.mehvahdjukaar.sleep_tight.SleepTightClient;
import net.mehvahdjukaar.sleep_tight.client.PendulumAnimation;
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

    private final PendulumAnimation animation;

    public HammockTile(BlockPos blockPos, BlockState blockState) {
        super(SleepTight.HAMMOCK_TILE.get(), blockPos, blockState);
        this.color = ((HammockBlock) blockState.getBlock()).getColor();
        this.pivotOffset = blockState.getValue(HammockBlock.PART).getPivotOffset();
        this.direction = blockState.getValue(HammockBlock.FACING);

        if (PlatformHelper.getEnv().isClient()) {
            this.animation = new PendulumAnimation(ClientConfigs.HAMMOCK_ANIMATION_PARAM, this::getRotationAxis);
        } else {
            this.animation = null;
        }
    }

    private Vector3f getRotationAxis() {
        return getBlockState().getValue(HammockBlock.FACING).step();
    }


    public DyeColor getColor() {
        return color;
    }

    public float getRoll(float partialTicks) {
        if (!ClientConfigs.HAMMOCK_ANIMATION.get()) return 0;
        return animation.getAngle(partialTicks);
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
        if(e.accelerateLeft || e.accelerateRight) {
            double push = ClientConfigs.SWING_FORCE.get();
            e.animation.addImpulse((float) (push * (e.accelerateLeft ? -1 : 1)));
            //update other clients
            NetworkHandler.CHANNEL.sendToServer(new AccelerateHammockMessage(pos, e.accelerateLeft));
        }
        e.animation.tick(false);

        //client is in charge here. Therefore they are only in charge of their own player
        if (Mth.abs(e.animation.getAngle(0)) >( -4 + 90) && ClientConfigs.HAMMOCK_FALL.get()) {
            for (var b : e.level.getEntitiesOfClass(BedEntity.class, new AABB(pos))) {
                for (var p : b.getPassengers()) {
                    if (p == SleepTightClient.getPlayer()) {
                        NetworkHandler.CHANNEL.sendToServer(new ServerBoundFallFromHammockMessage());
                    } else return;
                }
            }
        }

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

    public void addImpulse(float vel) {
        animation.addImpulse(vel);
    }
}
