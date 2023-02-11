package net.mehvahdjukaar.sleep_tight.common;

import com.mojang.datafixers.util.Pair;
import dev.architectury.injectables.annotations.PlatformOnly;
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
import net.minecraft.world.phys.shapes.VoxelShape;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class HammockBlock extends HorizontalDirectionalBlock implements EntityBlock {

    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);
    public static final BooleanProperty OCCUPIED = BlockStateProperties.OCCUPIED;
    public static final VoxelShape SHAPE = Block.box(0.0, 3.0, 0.0, 16.0, 6.0, 16.0);


    private final DyeColor color;

    public HammockBlock(DyeColor color) {
        super(Properties.of(Material.WOOL, color.getMaterialColor())
                .sound(SoundType.WOOL).strength(0.1F).noOcclusion());
        this.color = color;
        this.registerDefaultState(this.stateDefinition.any().setValue(PART, Part.MIDDLE).setValue(OCCUPIED, false));
    }

    public DyeColor getColor() {
        return color;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.CONSUME;
        } else {
            pos = getMasterPos(state, pos);
            state = level.getBlockState(pos);
            if (!state.is(this)) {
                return InteractionResult.CONSUME;
            }

            if (state.getValue(OCCUPIED)) {
                //TODO: make nitwids use hammocks if available
                //if (!this.kickVillagerOutOfBed(level, pos)) {
                player.displayClientMessage(Component.translatable("block.minecraft.bed.occupied"), true);
                //}

                return InteractionResult.SUCCESS;
            } else {
                SleepHelper.startSleepInBed(player, pos,state.getValue(FACING), !state.getValue(PART).onFence).ifLeft(bedSleepingProblem -> {
                    if (bedSleepingProblem.getMessage() != null) {
                        player.displayClientMessage(bedSleepingProblem.getMessage(), true);
                    }
                });
                return InteractionResult.SUCCESS;
            }
        }
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
        super.fallOn(level, state, pos, entity, fallDistance * 0.8F);
    }

    @Override
    public void updateEntityAfterFallOn(BlockGetter level, Entity entity) {
        if (entity.isSuppressingBounce()) {
            super.updateEntityAfterFallOn(level, entity);
        } else {
            Vec3 vec3 = entity.getDeltaMovement();
            if (vec3.y < 0.0) {
                double d = entity instanceof LivingEntity ? 1.0 : 0.8;
                entity.setDeltaMovement(vec3.x, -vec3.y * 0.9F * d, vec3.z);
            }
        }
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos currentPos, BlockPos neighborPos) {
        Part part = state.getValue(PART);
        Direction myDir = state.getValue(FACING);
        if (direction.getAxis() == myDir.getAxis()) {
            for (var v : part.getPiecesDirections(myDir)) {
                if (v.getFirst() == direction) {
                    if (neighborState.getBlock() instanceof HammockBlock) {
                        if (neighborState.getValue(PART) == v.getSecond() &&
                                neighborState.getValue(FACING) == myDir) {
                            //accounts for color change
                            BlockState newState = neighborState.is(this) ? neighborState :
                                    BlocksColorAPI.changeColor(neighborState.getBlock(), this.color).withPropertiesOf(neighborState);
                            return newState.setValue(OCCUPIED, neighborState.getValue(OCCUPIED));
                        }
                        break;
                    }
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
            Part part = state.getValue(PART);
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
                        if (!level.getBlockState(nextPos).canBeReplaced(context) || !level.getWorldBorder().isWithinBounds(nextPos)) {
                            continue;
                        } else if (type == Connection.BLOCK) {
                            nextPos = nextPos.relative(opposite);
                            if (!level.getBlockState(nextPos).canBeReplaced(context) || !level.getWorldBorder().isWithinBounds(nextPos)) {
                                continue;
                            }
                        }
                        if (getConnectionType(opposite, nextPos, level) == type) {
                            var t = i == 1 ? Part.MIDDLE : type.getHead();
                            return this.defaultBlockState().setValue(FACING, dir).setValue(PART, t);
                        }
                    }
                }
            }
        }
        return null;
    }

    private static Connection getConnectionType(Direction dir, BlockPos pos, Level level) {
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
            Part part = state.getValue(PART);
            for (var v : part.getPiecesDirections(state.getValue(FACING))) {
                BlockPos blockPos = pos.relative(v.getFirst());
             //   level.setBlock(blockPos, state.setValue(PART, v.getSecond()), 3);
                if (part == Part.HALF_HEAD) {
                    blockPos = blockPos.relative(v.getFirst());
                  //  level.setBlock(blockPos, state.setValue(PART, Part.HALF_FOOT), 3);
                }
                level.blockUpdated(pos, Blocks.AIR);
                state.updateNeighbourShapes(level, pos, 3);
            }
        }
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /*
    public static DoubleBlockCombiner.BlockType getBlockType(BlockState state) {
        BedPart bedPart = state.getValue(PART);
        return bedPart == BedPart.HEAD ? DoubleBlockCombiner.BlockType.FIRST : DoubleBlockCombiner.BlockType.SECOND;
    }*/

    private static Optional<Vec3> findStandUpPositionAtOffset(
            EntityType<?> entityType, CollisionGetter collisionGetter, BlockPos pos, int[][] offsets, boolean simulate
    ) {
        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();

        for (int[] is : offsets) {
            mutableBlockPos.set(pos.getX() + is[0], pos.getY(), pos.getZ() + is[1]);
            Vec3 vec3 = DismountHelper.findSafeDismountLocation(entityType, collisionGetter, mutableBlockPos, simulate);
            if (vec3 != null) {
                return Optional.of(vec3);
            }
        }
        return Optional.empty();
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
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART).isMaster() ? new HammockBlockEntity(pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? Utils.getTicker(type, SleepTight.HAMMOCK_TILE.get(), HammockBlockEntity::tick) : null;
    }


    @Override
    public long getSeed(BlockState state, BlockPos pos) {
        BlockPos blockPos = getMasterPos(state, pos);
        return Mth.getSeed(blockPos.getX(), pos.getY(), blockPos.getZ());
    }

    private static BlockPos getMasterPos(BlockState state, BlockPos pos) {
        return pos.relative(state.getValue(FACING), state.getValue(PART).getMasterOffset());
    }

    @Override
    public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType type) {
        return false;
    }

    private static int[][] bedStandUpOffsets(Direction firstDir, Direction secondDir) {
        return ArrayUtils.addAll(bedSurroundStandUpOffsets(firstDir, secondDir), bedAboveStandUpOffsets(firstDir));
    }

    private static int[][] bedSurroundStandUpOffsets(Direction firstDir, Direction secondDir) {
        return new int[][]{
                {secondDir.getStepX(), secondDir.getStepZ()},
                {secondDir.getStepX() - firstDir.getStepX(), secondDir.getStepZ() - firstDir.getStepZ()},
                {secondDir.getStepX() - firstDir.getStepX() * 2, secondDir.getStepZ() - firstDir.getStepZ() * 2},
                {-firstDir.getStepX() * 2, -firstDir.getStepZ() * 2},
                {-secondDir.getStepX() - firstDir.getStepX() * 2, -secondDir.getStepZ() - firstDir.getStepZ() * 2},
                {-secondDir.getStepX() - firstDir.getStepX(), -secondDir.getStepZ() - firstDir.getStepZ()},
                {-secondDir.getStepX(), -secondDir.getStepZ()},
                {-secondDir.getStepX() + firstDir.getStepX(), -secondDir.getStepZ() + firstDir.getStepZ()},
                {firstDir.getStepX(), firstDir.getStepZ()},
                {secondDir.getStepX() + firstDir.getStepX(), secondDir.getStepZ() + firstDir.getStepZ()}
        };
    }

    private static int[][] bedAboveStandUpOffsets(Direction dir) {
        return new int[][]{{0, 0}, {-dir.getStepX(), -dir.getStepZ()}};
    }

    private enum Connection {
        BLOCK, FENCE, NONE;

        public int length() {
            return this == BLOCK ? 3 : 2;
        }

        public Part getHead() {
            return this == BLOCK ? Part.HALF_HEAD : Part.HEAD;
        }
    }

    public enum Part implements StringRepresentable {
        HALF_HEAD("half_head", -1, false),
        MIDDLE("middle", 0, false),
        HALF_FOOT("half_foot", 1, false),
        HEAD("head", 0, true),
        FOOT("foot", 1, true);

        private final String name;
        private final int masterOffset;
        private final boolean onFence;

        Part(String name, int offset, boolean onRope) {
            this.name = name;
            this.masterOffset = offset;
            this.onFence = onRope;
        }

        @Override
        public String toString() {
            return this.name;
        }

        public String getSerializedName() {
            return this.name;
        }

        //goes from head to foot
        public boolean isFoot() {
            return this == FOOT || this == HALF_FOOT;
        }

        public Part getToFootPart() {
            return switch (this) {
                case HEAD -> FOOT;
                case HALF_HEAD -> MIDDLE;
                case MIDDLE -> HALF_FOOT;
                default -> null;
            };
        }

        public Part getToHeadPart() {
            return switch (this) {
                case FOOT -> HEAD;
                case MIDDLE -> HALF_HEAD;
                case HALF_FOOT -> MIDDLE;
                default -> null;
            };
        }

        public List<Pair<Direction, Part>> getPiecesDirections(Direction myDirection) {
            var list = new ArrayList<Pair<Direction, Part>>();
            var p = getToHeadPart();
            if (p != null) list.add(Pair.of(myDirection, p));
            var p1 = getToFootPart();
            if (p1 != null) list.add(Pair.of(myDirection.getOpposite(), p1));
            return list;
        }

        public int getMasterOffset() {
            return masterOffset;
        }

        public boolean isMaster() {
            return this.masterOffset == 0;
        }

        public boolean isOnFence() {
            return onFence;
        }

        @Nullable
        public Direction getRopeAttachmentDirection(Direction myDir) {
            return switch (this) {
                case FOOT, HALF_FOOT -> myDir.getOpposite();
                case HEAD, HALF_HEAD -> myDir;
                case MIDDLE -> null;
            };
        }
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

}
