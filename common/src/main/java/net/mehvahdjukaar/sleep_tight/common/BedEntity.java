package net.mehvahdjukaar.sleep_tight.common;

import dev.architectury.injectables.annotations.PlatformOnly;
import net.mehvahdjukaar.moonlight.api.entity.IControllableVehicle;
import net.mehvahdjukaar.moonlight.api.entity.IExtraClientSpawnData;
import net.mehvahdjukaar.moonlight.api.platform.PlatformHelper;
import net.mehvahdjukaar.sleep_tight.SleepTight;
import net.mehvahdjukaar.sleep_tight.SleepTightClient;
import net.mehvahdjukaar.sleep_tight.client.ClientEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;
import java.util.List;

public class BedEntity extends Entity implements IControllableVehicle, IExtraClientSpawnData {

    private Direction dir = Direction.NORTH;
    private boolean hasOffset = false;
    private BlockState bedState = Blocks.AIR.defaultBlockState();

    public BedEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    public BedEntity(Level worldIn, BlockState bedState) {
        super(SleepTight.BED_ENTITY.get(), worldIn);
        Direction bedDir = bedState.getValue(BedBlock.FACING);
        this.dir = bedDir.getOpposite();
        this.hasOffset = isTripleBed(bedState);
        this.setYRot(this.dir.toYRot());
        this.bedState = bedState;
    }

    @Override
    public void tick() {
        super.tick();
        List<Entity> passengers = getPassengers();
        for (var p : passengers) {
            p.setPose(Pose.SLEEPING);
        }
        boolean dead = passengers.isEmpty();
        BlockPos pos = blockPosition();
        this.bedState = level.getBlockState(pos);
        boolean isBed = isBed(bedState);

        if (isBed) {
            dir = bedState.getValue(BedBlock.FACING).getOpposite();
            hasOffset = isTripleBed(bedState);
        }

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
                level.setBlockAndUpdate(pos, bedState.setValue(BedBlock.OCCUPIED, false));
            }
        }
    }

    private static boolean isTripleBed(BlockState state) {
        return state.getBlock() instanceof HammockBlock && !state.getValue(HammockBlock.PART).isOnFence();
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
            if(bedState.getBlock() instanceof IModBed b){
                var v = b.getSleepingPosition(bedState, this.blockPosition());
               v =  v.relative(dir, -3 / 32f);
                passenger.setPos(v.x, v.y, v.z);
            }
        }
    }

    @Override
    public void onPassengerTurned(Entity entity) {
        float diff = Mth.wrapDegrees(entity.getYHeadRot() - this.getYRot());
        float clampedDiff = Mth.clamp(diff, -90, 90);
        float subtract = clampedDiff - diff;
        //((LivingEntity)  entity).yHeadRotO += subtract;

          ((LivingEntity)  entity).yHeadRot += subtract;
        //   entity.setYRot(entity.getYRot() + f1 - diff);
          entity.setXRot(Mth.clamp(entity.getXRot(),-75,0));
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
        // passenger.setPose(Pose.SLEEPING);
        positionRider(passenger);
        passenger.setYRot(this.getYRot());
    }

    public void startSleepingOn(Player player) {
        var e = player.getVehicle();
        if (e instanceof BedEntity) {
            player.removeVehicle();
            e.discard();
        }
        player.startSleepInBed(this.getOnPos()).ifLeft(bedSleepingProblem -> {
            if (bedSleepingProblem.getMessage() != null) {
                player.displayClientMessage(bedSleepingProblem.getMessage(), true);
            }
        });
    }

    public static void layDown(BlockState state, Level level, BlockPos pos, Player player) {
        if (!level.isClientSide) {

            BedEntity entity = new BedEntity(level, state);
            entity.setPos(pos.getX() + 0.5, pos.getY() + 0.25, pos.getZ() + 0.5);

            level.addFreshEntity(entity);
            player.startRiding(entity);
            level.setBlockAndUpdate(pos, state.setValue(BedBlock.OCCUPIED, true));
        }
    }

    @Override
    public void onInputUpdate(boolean left, boolean right, boolean up, boolean down, boolean sprint, boolean jumping) {
        if (jumping) {
            ClientEvents.playerSleepCommit(this);
        } else if (up || down) {
            if (getBedTile() instanceof HammockBlockEntity tile) {
                if (up) tile.accelerate();
                if (down) tile.decelerate();
            }
        }
    }

    public BlockEntity getBedTile() {
        return this.level.getBlockEntity(this.getOnPos());
    }

    @Override
    public void writeSpawnData(FriendlyByteBuf buf) {
        buf.writeInt(this.dir.get2DDataValue());
        buf.writeBoolean(this.hasOffset);
    }

    @Override
    public void readSpawnData(FriendlyByteBuf buf) {
        this.dir = Direction.from2DDataValue(buf.readInt());
        this.hasOffset = buf.readBoolean();

    }
}
