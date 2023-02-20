package net.mehvahdjukaar.sleep_tight.common;

import com.mojang.datafixers.util.Pair;
import dev.architectury.injectables.annotations.PlatformOnly;
import net.mehvahdjukaar.moonlight.api.block.IRotatable;
import net.mehvahdjukaar.moonlight.api.set.BlocksColorAPI;
import net.mehvahdjukaar.moonlight.api.util.Utils;
import net.mehvahdjukaar.sleep_tight.SleepTight;
import net.mehvahdjukaar.sleep_tight.integration.SupplementariesCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class HammockBlock extends HorizontalDirectionalBlock implements EntityBlock, IRotatable, IModBed{

    public static final EnumProperty<HammockPart> PART = EnumProperty.create("part", HammockPart.class);
    public static final BooleanProperty OCCUPIED = BlockStateProperties.OCCUPIED;
    public static final VoxelShape SHAPE_FULL = Block.box(0.0, 3.0, 0.0, 16.0, 6.0, 16.0);
    public static final VoxelShape SHAPE_NORTH = Block.box(0.0, 3.0, 8.0, 16.0, 6.0, 16.0);
    public static final VoxelShape SHAPE_SOUTH = Block.box(0.0, 3.0, 0.0, 16.0, 6.0, 8);
    public static final VoxelShape SHAPE_WEST = Block.box(8, 3.0, 0.0, 16.0, 6.0, 16.0);
    public static final VoxelShape SHAPE_EAST = Block.box(0.0, 3.0, 0, 8, 6.0, 16);

    private final DyeColor color;

    public HammockBlock(DyeColor color) {
        super(Properties.of(Material.WOOL, color.getMaterialColor())
                .sound(SoundType.WOOL).strength(0.1F).noOcclusion());
        this.color = color;
        this.registerDefaultState(this.stateDefinition.any().setValue(PART, HammockPart.MIDDLE).setValue(OCCUPIED, false));
    }

    public DyeColor getColor() {
        return color;
    }

    //shapes

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
        super.fallOn(level, state, pos, entity, fallDistance * 0.8F);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos currentPos, BlockPos neighborPos) {
        HammockPart part = state.getValue(PART);
        Direction myDir = state.getValue(FACING);
        if (direction.getAxis() == myDir.getAxis()) {
            for (var v : part.getPiecesDirections(myDir)) {
                if (v.getFirst() == direction) {
                    if (neighborState.getBlock() instanceof HammockBlock nh) {
                        if (neighborState.getValue(PART) == v.getSecond() &&
                                neighborState.getValue(FACING) == myDir) {
                            //accounts for color change
                            BlockState newState = neighborState.is(this) ? state :
                                    BlocksColorAPI.changeColor(state.getBlock(), nh.color)
                                            .withPropertiesOf(state);
                            return newState.setValue(OCCUPIED, neighborState.getValue(OCCUPIED));
                        }
                        break;
                    }
                    return Blocks.AIR.defaultBlockState();
                }
            }
            Direction attDir = part.getConnectionDirection(myDir);
            if (attDir == direction) {
                Connection c = getConnectionType(attDir, currentPos, level);
                if (c == Connection.NONE || (c == Connection.FENCE != part.isOnFence())) {
                    return Blocks.AIR.defaultBlockState();
                }
            }
        }
        return super.updateShape(state, direction, neighborState, level, currentPos, neighborPos);
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && player.isCreative()) {
            //prevents drop
            HammockPart part = state.getValue(PART);
            if (!part.isMaster()) {
                int i = part.getMasterOffset();
                if (i != 0) {
                    BlockPos blockPos = pos.relative(state.getValue(FACING), i);
                    BlockState blockState = level.getBlockState(blockPos);
                    if (blockState.is(this) && blockState.getValue(PART).isMaster()) {
                        level.setBlock(blockPos, Blocks.AIR.defaultBlockState(), 35);
                        level.levelEvent(player, 2001, blockPos, Block.getId(blockState));
                    }
                }
            }
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();

        BlockPos p;

        for (Direction dir : context.getNearestLookingDirections()) {
            if (dir.getAxis().isHorizontal()) {
                for (int i = 0; i < 2; i++) {
                    p = pos.relative(dir, i);
                    var type = getConnectionType(dir, p, level);
                    if ((type == Connection.FENCE && i != 1) || type == Connection.BLOCK) {
                        Direction opposite = dir.getOpposite();
                        BlockPos nextPos = p.relative(opposite);
                        if (!level.getBlockState(p).canBeReplaced(context) || !level.getWorldBorder().isWithinBounds(nextPos)) {
                            continue;
                        }
                        if (type == Connection.BLOCK) {
                            nextPos = nextPos.relative(opposite);
                            if (!level.getBlockState(nextPos).canBeReplaced(context) || !level.getWorldBorder().isWithinBounds(nextPos)) {
                                continue;
                            }
                        }
                        if (getConnectionType(opposite, nextPos, level) == type) {
                            var t = i == 1 ? HammockPart.MIDDLE : type.getHead();
                            return this.defaultBlockState().setValue(FACING, dir).setValue(PART, t);
                        }
                    }
                }
            }
        }
        return null;
    }

    private static Connection getConnectionType(Direction dir, BlockPos pos, LevelAccessor level) {
        BlockPos relative = pos.relative(dir);
        BlockState facingState = level.getBlockState(relative);
        Direction opposite = dir.getOpposite();
        if (facingState.isFaceSturdy(level, relative, opposite, SupportType.CENTER)) return Connection.BLOCK;
        if (facingState.getBlock() instanceof FenceBlock ||
                (SleepTight.SUPP && SupplementariesCompat.isRopeKnot(facingState))) return Connection.FENCE;
        return Connection.NONE;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide) {
            HammockPart part = state.getValue(PART);
            for (var v : part.getPiecesDirections(state.getValue(FACING))) {
                BlockPos blockPos = pos.relative(v.getFirst());
                level.setBlock(blockPos, state.setValue(PART, v.getSecond()), 3);
                if (part == HammockPart.HALF_HEAD) {
                    blockPos = blockPos.relative(v.getFirst());
                    level.setBlock(blockPos, state.setValue(PART, HammockPart.HALF_FOOT), 3);
                }
            }
        }
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        HammockPart part = state.getValue(PART);
        if (part.isOnFence() || part == HammockPart.MIDDLE) return SHAPE_FULL;
        else {
            return switch (part.getConnectionDirection(state.getValue(FACING))) {
                case WEST -> SHAPE_WEST;
                case EAST -> SHAPE_EAST;
                case SOUTH -> SHAPE_SOUTH;
                default -> SHAPE_NORTH;
            };
        }
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.DESTROY;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING, PART, OCCUPIED);
    }

    @Override
    public long getSeed(BlockState state, BlockPos pos) {
        BlockPos blockPos = getMasterPos(state, pos);
        return Mth.getSeed(blockPos.getX(), pos.getY(), blockPos.getZ());
    }

    private static BlockPos getMasterPos(BlockState state, BlockPos pos) {
        return pos.relative(state.getValue(FACING), state.getValue(PART).getMasterOffset());
    }

    //block stuff

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART).isMaster() ? new HammockBlockEntity(pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? Utils.getTicker(type, SleepTight.HAMMOCK_TILE.get(), HammockBlockEntity::tick) : null;
    }

    @Override
    public boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
        return super.triggerEvent(state, level, pos, id, param);
    }

    @Override
    public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType type) {
        return false;
    }

    //bed logic

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        //called on both sides
        pos = getMasterPos(state, pos);
        state = level.getBlockState(pos);
        if (!state.is(this) || player.getVehicle() != null) {
            return InteractionResult.FAIL;
        }

        if (state.getValue(OCCUPIED)) {
            //TODO: make nitwids use hammocks if available
            //if (!this.kickVillagerOutOfBed(level, pos)) {
            player.displayClientMessage(Component.translatable("block.minecraft.bed.occupied"), true);
            //}

            return InteractionResult.SUCCESS;
        } else {
            BedEntity.layDown(state, level, pos, player);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
    }

    @Override
    public Vec3 getSleepingPosition(BlockState state, BlockPos pos) {
        Vec3 v = Vec3.atCenterOf(pos).subtract(0, 0.125, 0);
        float off = 3 / 32f;
        if (!state.getValue(PART).isOnFence()) {
            off += 0.5;
        }
        v = v.relative(state.getValue(FACING), off);
        return v;
    }


    @Override
    public Optional<BlockState> getRotatedState(BlockState state, LevelAccessor world, BlockPos pos, Rotation rotation, Direction axis, @Nullable Vec3 hit) {
        return Optional.empty();
    }

    //TODO
    @Override
    public Optional<Direction> rotateOverAxis(BlockState state, LevelAccessor world, BlockPos pos, Rotation rotation, Direction axis, @Nullable Vec3 hit) {
        return IRotatable.super.rotateOverAxis(state, world, pos, rotation, axis, hit);
    }


    //@Override
    @PlatformOnly(PlatformOnly.FORGE)
    public boolean isBed(BlockState state, BlockGetter level, BlockPos pos, @Nullable Entity player) {
        return true;
    }

    //@Override
    @PlatformOnly(PlatformOnly.FORGE)
    public void setBedOccupied(BlockState state, Level level, BlockPos pos, LivingEntity sleeper, boolean occupied) {
        level.setBlock(pos, state.setValue(BedBlock.OCCUPIED, occupied), 3);
    }

    //@Override
    @PlatformOnly(PlatformOnly.FORGE)
    public Direction getBedDirection(BlockState state, LevelReader level, BlockPos pos) {
        return state.getValue(HorizontalDirectionalBlock.FACING);
    }

    private enum Connection {
        BLOCK, FENCE, NONE;

        public int length() {
            return this == BLOCK ? 3 : 2;
        }

        public HammockPart getHead() {
            return this == BLOCK ? HammockPart.HALF_HEAD : HammockPart.HEAD;
        }
    }
}
