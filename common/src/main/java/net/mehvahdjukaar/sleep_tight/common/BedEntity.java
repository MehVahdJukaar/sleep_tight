package net.mehvahdjukaar.sleep_tight.common;

import dev.architectury.injectables.annotations.PlatformOnly;
import net.mehvahdjukaar.moonlight.api.platform.PlatformHelper;
import net.mehvahdjukaar.sleep_tight.SleepTight;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;
import java.util.List;

public class BedEntity extends Entity {


    public BedEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    public BedEntity(Level worldIn) {
        super(SleepTight.BED_ENTITY.get(), worldIn);
    }


    @Override
    public void tick() {
        super.tick();
        List<Entity> passengers = getPassengers();
        for(var p : passengers){
         //   p.setPose(Pose.SLEEPING);
        }
        boolean dead = passengers.isEmpty();
        BlockPos pos = blockPosition();
        BlockState state = level.getBlockState(pos);
        boolean isBed = isBed(state);

        if (!dead && !isBed) {


            PistonMovingBlockEntity piston = null;
            boolean didOffset = false;

            BlockEntity tile = level.getBlockEntity(pos);
            if (tile instanceof PistonMovingBlockEntity pistonBE && isBed(pistonBE.getMovedState()))
                piston = pistonBE;
            else for (Direction d : Direction.values()) {
                BlockPos offPos = pos.relative(d);
                tile = level.getBlockEntity(offPos);

                if (tile instanceof PistonMovingBlockEntity pistonBE && isBed(pistonBE.getMovedState())) {
                    piston = pistonBE;
                    break;
                }
            }

            if (piston != null) {
                Direction dir = piston.getMovementDirection();
                move(MoverType.PISTON, new Vec3(dir.getStepX() * 0.33, dir.getStepY() * 0.33, dir.getStepZ() * 0.33));

                didOffset = true;
            }
            dead = !didOffset;
        }

        if (dead && !level.isClientSide) {
            discard();
            if (isBed) {
                level.setBlockAndUpdate(pos, state.setValue(BedBlock.OCCUPIED, false));
            }
        }
    }

    private static boolean isBed(BlockState state) {
        Block b = state.getBlock();
        if (b instanceof BedBlock) {
            return state.getValue(BedBlock.PART) == BedPart.HEAD;
        }
        if (b instanceof HammockBlock) {
            return state.getValue(HammockBlock.PART).isMaster();
        }
        return false;
    }

    @Override
    public double getPassengersRidingOffset() {
        return 0.0125;
    }

    @Override
    public void positionRider(Entity passenger) {
        if (this.hasPassenger(passenger)) {
            double d = this.getY() + this.getPassengersRidingOffset() + passenger.getMyRidingOffset();
            passenger.setPos(this.getX()+0.5, d, this.getZ());
        }
    }

    @PlatformOnly(PlatformOnly.FORGE)
    boolean shouldRiderSit() {
        return false;
    }

    @Override
    public boolean shouldRender(double x, double y, double z) {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return false;
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    protected void readAdditionalSaveData(@Nonnull CompoundTag compound) {
    }

    @Override
    protected void addAdditionalSaveData(@Nonnull CompoundTag compound) {
    }

    @Nonnull
    @Override
    public Packet<?> getAddEntityPacket() {
        return PlatformHelper.getEntitySpawnPacket(this);
    }

    @Override
    protected void addPassenger(Entity passenger) {
        super.addPassenger(passenger);
        passenger.setPose(Pose.SLEEPING);
    }

    public static void commitSleep(Player player, BlockPos pos){
        player.startSleepInBed(pos).ifLeft(bedSleepingProblem -> {
            if (bedSleepingProblem.getMessage() != null) {
                player.displayClientMessage(bedSleepingProblem.getMessage(), true);
            }
        });
    }

    public static void layDown(BlockState state, Level level, BlockPos pos, Player player) {
        if (!level.isClientSide && !state.getValue(BedBlock.OCCUPIED) && player.getVehicle() == null) {
            BedEntity entity = new BedEntity(level);
            entity.setPos(pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5);
            entity.setYRot(state.getValue(BedBlock.FACING).getOpposite().toYRot());
            level.addFreshEntity(entity);
            player.startRiding(entity);
            level.setBlockAndUpdate(pos, state.setValue(BedBlock.OCCUPIED, true));
        }
    }
}
