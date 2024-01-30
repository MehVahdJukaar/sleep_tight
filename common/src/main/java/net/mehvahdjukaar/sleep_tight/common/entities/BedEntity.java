package net.mehvahdjukaar.sleep_tight.common.entities;

import net.mehvahdjukaar.moonlight.api.entity.IControllableVehicle;
import net.mehvahdjukaar.moonlight.api.entity.IExtraClientSpawnData;
import net.mehvahdjukaar.moonlight.api.platform.PlatHelper;
import net.mehvahdjukaar.moonlight.api.util.math.MthUtils;
import net.mehvahdjukaar.sleep_tight.SleepTight;
import net.mehvahdjukaar.sleep_tight.SleepTightPlatformStuff;
import net.mehvahdjukaar.sleep_tight.client.ClientEvents;
import net.mehvahdjukaar.sleep_tight.common.blocks.HammockBlock;
import net.mehvahdjukaar.sleep_tight.common.blocks.IModBed;
import net.mehvahdjukaar.sleep_tight.common.network.ClientBoundAlightCameraOnLayMessage;
import net.mehvahdjukaar.sleep_tight.common.network.ClientBoundSleepImmediatelyMessage;
import net.mehvahdjukaar.sleep_tight.common.network.NetworkHandler;
import net.mehvahdjukaar.sleep_tight.common.network.ServerBoundCommitSleepMessage;
import net.mehvahdjukaar.sleep_tight.common.tiles.HammockTile;
import net.mehvahdjukaar.sleep_tight.configs.CommonConfigs;
import net.mehvahdjukaar.sleep_tight.core.ModEvents;
import net.mehvahdjukaar.sleep_tight.core.PlayerSleepData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
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

import java.util.List;
import java.util.UUID;

public class BedEntity extends Entity implements IControllableVehicle, IExtraClientSpawnData {

    private Direction dir = Direction.NORTH;
    public static final EntityDataSerializer<OffsetMode> SERIALIZER = EntityDataSerializer.simpleEnum(OffsetMode.class);
    private static final EntityDataAccessor<OffsetMode> DATA_OFFSET = SynchedEntityData.defineId(BedEntity.class, SERIALIZER);


    private BlockState bedState = Blocks.AIR.defaultBlockState();

    private boolean dismountOnTheSpot = false;

    public BedEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    public BedEntity(Level worldIn, BlockPos mainPos, BlockState bedState, OffsetMode offsetMode) {
        super(SleepTight.BED_ENTITY.get(), worldIn);
        Direction bedDir = bedState.getValue(BedBlock.FACING);
        this.dir = bedDir.getOpposite();
        this.setYRot(this.dir.toYRot());
        this.bedState = bedState;
        this.setOffsetMode(offsetMode);

        this.setPos(mainPos.getX() + 0.5, mainPos.getY() + 0.25, mainPos.getZ() + 0.5);
    }

    protected void defineSynchedData() {
        this.entityData.define(DATA_OFFSET, OffsetMode.NONE);
    }

    public OffsetMode getOffsetMode() {
        return this.entityData.get(DATA_OFFSET);
    }

    public void setOffsetMode(OffsetMode mode) {
        this.entityData.set(DATA_OFFSET, mode);
    }

    public Direction getBedDirection() {
        return dir;
    }

    public boolean isDoubleBed() {
        return getOffsetMode() == OffsetMode.DOUBLE_BED;
    }

    public void clearDoubleBed() {
        if (getOffsetMode() == OffsetMode.DOUBLE_BED) {
            setOffsetMode(OffsetMode.NONE);

            BlockPos otherPos = getDoubleBedPos();
            Level level = level();
            BlockState otherState = level.getBlockState(otherPos);
            if (otherState == bedState) {
                level.setBlockAndUpdate(otherPos, otherState.setValue(BedBlock.OCCUPIED, false));
            }
        }
    }

    @Override
    public void tick() {
        super.tick();

        Level level = this.level();
        List<Entity> passengers = getPassengers();
        for (var p : passengers) {
            p.setPose(Pose.SLEEPING);

            if (this.tickCount > 2 && !level.isClientSide && CommonConfigs.SLEEP_IMMEDIATELY.get() && p instanceof ServerPlayer sp) {
                this.startSleepingOn(sp);
            }
        }
        boolean dead = passengers.isEmpty();
        BlockPos pos = blockPosition();
        BlockState newBedState = level.getBlockState(pos);
        boolean isBed = isValidBed(newBedState);

        if (isDoubleBed() && tickCount > 2) {
            BlockPos otherPos = getDoubleBedPos();
            if (level.getBlockState(otherPos) != newBedState) {
                this.clearDoubleBed();
            }
        }

        if (isBed) {
            this.dir = newBedState.getValue(BedBlock.FACING).getOpposite();
        }

        if (!dead && !isBed) {


            PistonMovingBlockEntity piston = null;
            boolean didOffsetByPiston = false;
            //this will break with double beds
            BlockEntity tile = level.getBlockEntity(pos);
            if (tile instanceof PistonMovingBlockEntity pistonBE && isValidBed(pistonBE.getMovedState()))
                piston = pistonBE;
            else for (Direction d : Direction.values()) {
                BlockPos offPos = pos.relative(d);
                tile = level.getBlockEntity(offPos);

                if (tile instanceof PistonMovingBlockEntity pistonBE && isValidBed(pistonBE.getMovedState())) {
                    piston = pistonBE;
                    break;
                }
            }

            if (piston != null) {
                Direction dir = piston.getMovementDirection();
                move(MoverType.PISTON, new Vec3(dir.getStepX() * 0.33, dir.getStepY() * 0.33, dir.getStepZ() * 0.33));

                didOffsetByPiston = true;
                this.clearDoubleBed();
            }
            dead = !didOffsetByPiston;
        }


        if (dead && !level.isClientSide) {
            if (isBed) {
                level.setBlockAndUpdate(pos, newBedState.setValue(BedBlock.OCCUPIED, false));
            }
            clearDoubleBed();
            discard();
        }

        this.bedState = newBedState;
    }


    public static Vec3 getDoubleBedOffset(Direction dir, Vec3 vec3) {
        Direction d = dir.getCounterClockWise();
        return vec3.add(d.getStepX() * -0.5, 0, d.getStepZ() * -0.5);
    }

    public static BlockPos getDoubleBedPos(BlockPos pos, BlockState state) {
        return pos.relative(state.getValue(BedBlock.FACING).getClockWise());
    }

    public static BlockPos getInverseDoubleBedPos(BlockPos pos, BlockState state) {
        return pos.relative(state.getValue(BedBlock.FACING).getCounterClockWise());
    }

    public BlockPos getDoubleBedPos() {
        return this.blockPosition().relative(dir.getCounterClockWise());
    }


    private boolean isValidBed(BlockState state) {
        Block b = state.getBlock();
        if (ModEvents.isValidBed(state)) {
            return state.getValue(BedBlock.PART) == BedPart.HEAD;
        }
        if (b instanceof HammockBlock) {
            return state.getValue(HammockBlock.PART).isMaster();
        }
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
    protected void readAdditionalSaveData(CompoundTag compound) {
        this.setOffsetMode(OffsetMode.values()[compound.getByte("mode")]);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        compound.putByte("mode", (byte) getOffsetMode().ordinal());
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return PlatHelper.getEntitySpawnPacket(this);
    }

    @Override
    protected float ridingOffset(Entity entity) {
        return 0.0125f;
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction callback) {
        if (this.hasPassenger(passenger)) {
            if (bedState.getBlock() instanceof IModBed b) {
                var v = b.getSleepingPosition(bedState, this.blockPosition());
                callback.accept(passenger, v.x, v.y, v.z);
            } else {
                //same as set pos to bed
                BlockPos pos = this.blockPosition();
                Vec3 c = new Vec3(pos.getX() + 0.5, pos.getY() + 9 / 16f, pos.getZ() + 0.5);
                if (isDoubleBed()) {
                    c = getDoubleBedOffset(dir.getOpposite(), c);
                }
                callback.accept(passenger, c.x, c.y, c.z);
            }
        }
    }

    @Override
    public void onPassengerTurned(Entity entity) {
        float diff = Mth.wrapDegrees(entity.getYHeadRot() - this.getYRot());
        float clampedDiff = Mth.clamp(diff, -90, 90);
        float subtract = clampedDiff - diff;
        //((LivingEntity)  entity).yHeadRotO += subtract;

        ((LivingEntity) entity).yHeadRot += subtract;
        //   entity.setYRot(entity.getYRot() + f1 - diff);
        entity.setXRot(Mth.clamp(entity.getXRot(), -75, 0));
    }

    @Override
    protected void addPassenger(Entity passenger) {
        super.addPassenger(passenger);
        positionRider(passenger);
        passenger.setYRot(this.getYRot());
        passenger.setOldPosAndRot();
        passenger.setPose(Pose.SLEEPING);
        if (passenger instanceof Player player && player.isLocalPlayer()) {
            ClientEvents.displayRidingMessage(this);
            alignCamera(player, this.getYRot());
        }
    }

    public static void alignCamera(Player player, float bedYRot) {
        player.setYRot(bedYRot);
        player.setYHeadRot(bedYRot);
        player.yRotO = player.getYRot();
        player.yHeadRotO = player.yHeadRot;
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        this.positionRider(passenger);
        if (passenger instanceof Player p && p.isSleeping()) {
            passenger.setPose(Pose.SLEEPING);
        } else {
            passenger.setPose(Pose.STANDING);
        }
    }

    @Override
    public void onInputUpdate(boolean left, boolean right, boolean up, boolean down, boolean sprint, boolean jumping) {
        if (jumping) {
            NetworkHandler.CHANNEL.sendToServer(new ServerBoundCommitSleepMessage());
        } else if (left ^ right) {
            if (this.level().getBlockEntity(this.getOnPos()) instanceof HammockTile tile) {
                if (left) {
                    tile.accelerateLeft();
                } else {
                    tile.accelerateRight();
                }
            }
        }
    }

    @Override
    public void writeSpawnData(FriendlyByteBuf buf) {
        buf.writeInt(this.dir.get2DDataValue());
        buf.writeInt(this.getOffsetMode().ordinal());
        boolean isValid = !this.getPassengers().isEmpty();
        buf.writeBoolean(isValid);
        if (isValid) {
            buf.writeUUID(this.getPassengers().get(0).getUUID());
        }
    }

    @Override
    public void readSpawnData(FriendlyByteBuf buf) {
        this.dir = Direction.from2DDataValue(buf.readInt());
        this.setOffsetMode(OffsetMode.values()[buf.readInt()]);
        if (buf.readBoolean()) {
            UUID uniqueId = buf.readUUID();
            var p = this.level().getPlayerByUUID(uniqueId);
            if (p != null) p.startRiding(this);
        }
    }

    public MutableComponent getRidingMessage(Component keyMessage, Component shiftMessage) {
        this.bedState = level().getBlockState(this.blockPosition());
        if (bedState.getBlock() instanceof HammockBlock) {
            return Component.translatable("message.sleep_tight.start_resting", keyMessage, shiftMessage);
        } else {
            return Component.translatable("message.sleep_tight.start_sleeping", keyMessage, shiftMessage);
        }
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean skipAttackInteraction(Entity entity) {
        return true;
    }

    @Override
    public boolean isInvulnerable() {
        return true;
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        if (dismountOnTheSpot) return super.getDismountLocationForPassenger(passenger);
        var o = BedBlock.findStandUpPosition(passenger.getType(), passenger.level(),
                this.blockPosition(), this.dir, passenger.getYRot());
        //this will not quite work for hammocks but its good enough
        return o.orElseGet(() -> super.getDismountLocationForPassenger(passenger));
    }

    private enum OffsetMode {
        NONE, HAMMOCK_3L, DOUBLE_BED;
    }

    private static boolean isHammock3L(BlockState state) {
        return state.getBlock() instanceof HammockBlock && !state.getValue(HammockBlock.PART).isOnFence();
    }


    public void startSleepingOn(ServerPlayer player) {
        //safety check since we call startSleepInBed
        BlockPos pos = this.blockPosition();
        if (!this.isValidBed(level().getBlockState(pos))) {
            return;
        }
        this.dismountOnTheSpot = true;
        //Player should dismount bed entity here. called by vanilla code somewhere i think
        var r = player.startSleepInBed(pos);
        this.dismountOnTheSpot = false;

        var op = r.left();
        if (op.isPresent()) {
            //failed sleeping. mount entity again
            player.startRiding(this, true);
            Player.BedSleepingProblem problem = op.get();
            Component m;
            if (problem == Player.BedSleepingProblem.NOT_POSSIBLE_NOW && this.bedState.getBlock() instanceof IModBed mb) {
                m = mb.getSleepingProblemMessage();
            } else m = problem.getMessage();
            if (m != null) {
                player.displayClientMessage(m, true);
            }
        } else {
            //Incase player is somehow still riding this
            player.removeVehicle();

            PlayerSleepData data = SleepTightPlatformStuff.getPlayerSleepData(player);
            data.setDoubleBed(isDoubleBed());
            data.syncToClient(player);

            NetworkHandler.CHANNEL.sendToClientPlayer(player, new ClientBoundSleepImmediatelyMessage(pos));
            //safety check
            Level level = level();
            BlockState blockState = level.getBlockState(pos);
            if (blockState.getBlock() instanceof BedBlock) {
                level.setBlockAndUpdate(pos, blockState.setValue(BedBlock.OCCUPIED, true));
            }

            this.discard();
        }
    }

    public static void layDown(BlockState state, Level level, BlockPos pos, Player player) {
        if (!level.isClientSide) {

            OffsetMode mode = OffsetMode.NONE;
            if (isHammock3L(state)) mode = OffsetMode.HAMMOCK_3L;
            if (CommonConfigs.DOUBLE_BED.get()) {
                //TODO: single bed logic. also occupied stuff
                if (state.is(BlockTags.BEDS)) {

                    Direction dir = state.getValue(BedBlock.FACING).getClockWise();
                    BlockPos relative = pos.relative(dir);
                    BlockState s = level.getBlockState(relative);
                    //tries left and right bed
                    if (s == state) {
                        level.setBlockAndUpdate(relative, state.setValue(BedBlock.OCCUPIED, true));
                        mode = OffsetMode.DOUBLE_BED;

                    } else {
                        //move pos on double bed left one
                        dir = dir.getOpposite();
                        relative = pos.relative(dir);
                        s = level.getBlockState(relative);
                        if (s == state) {
                            level.setBlockAndUpdate(pos, state.setValue(BedBlock.OCCUPIED, true));
                            mode = OffsetMode.DOUBLE_BED;
                            pos = relative;
                        }
                    }
                }
            }
            level.setBlockAndUpdate(pos, state.setValue(BedBlock.OCCUPIED, true));

            BedEntity bedEntity = new BedEntity(level, pos, state, mode);

            player.startRiding(bedEntity);
            level.addFreshEntity(bedEntity);
            if (player instanceof ServerPlayer serverPlayer) {
                //Don't ask me why this is needed. Align camera immediately to prevent camera jerk
                //TODO: actually this doesnt even work. Fix. Also fix shifting when going off bed
                NetworkHandler.CHANNEL.sendToClientPlayer(serverPlayer, new ClientBoundAlightCameraOnLayMessage(bedEntity));
            }

        } else if (level.getBlockEntity(pos) instanceof HammockTile tile) {

            var d = player.getDeltaMovement();
            double vel = d.dot(MthUtils.V3itoV3(tile.getDirection().getClockWise().getNormal())) / d.length();

            tile.addImpulse((float) (-vel * 1.1f));
        }
    }

}
