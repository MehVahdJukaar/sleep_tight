package net.mehvahdjukaar.sleep_tight.common;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class NightBagBlock extends BedBlock implements IModBed {

    private static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 2.0D, 16.0D);
    private static final VoxelShape SHAPE_HACK = Shapes.or(SHAPE, Block.box(6.0D, 2.0D, 6.0D, 10.0D, 4.0D, 10.0D));

    public NightBagBlock(Properties properties) {
        super(DyeColor.BLUE, properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(PART) == BedPart.HEAD ? SHAPE_HACK : SHAPE;
    }

    @Deprecated
    public VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
        super.fallOn(level, state, pos, entity, fallDistance * 2);
    }

    @Override
    public void updateEntityAfterFallOn(BlockGetter level, Entity entity) {
        entity.setDeltaMovement(entity.getDeltaMovement().multiply(1.0, 0.0, 1.0));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return null;
    }

    @Override
    public Vec3 getSleepingPosition(BlockState state, BlockPos pos) {
        return Vec3.atCenterOf(pos).subtract(0, 0.25, 0);
    }

    @Override
    public void onWokenUp(BlockState state, BlockPos pos, Player player) {
        Level level = player.level;
        level.removeBlock(pos, false);
        BlockPos blockPos = pos.relative((state.getValue(FACING)).getOpposite());
        if (level.getBlockState(blockPos).is(this)) {
            level.removeBlock(blockPos, false);
        }
        InteractionHand hand = player.getUsedItemHand();
        ItemStack stack = new ItemStack(this);

        if (!player.getAbilities().instabuild) {
            if (player.getItemInHand(hand).isEmpty()) {
                player.setItemInHand(hand, stack);
            } else {
                if (!player.getInventory().add(stack)) {
                    player.drop(stack, false);
                }
            }
        }
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction direction = context.getHorizontalDirection();
        BlockPos blockPos = context.getClickedPos();
        BlockPos blockPos2 = blockPos.relative(direction);
        Level level = context.getLevel();
        return level.getBlockState(blockPos2).canBeReplaced(context) &&
                level.getWorldBorder().isWithinBounds(blockPos2) &&
                isSupporting(level, blockPos.below()) && isSupporting(level, blockPos2.below()) ?
                this.defaultBlockState().setValue(PART, BedPart.HEAD).setValue(FACING, direction.getOpposite()) : null;
    }

    private boolean isSupporting(Level level, BlockPos blockPos) {
        return level.getBlockState(blockPos).isFaceSturdy(level, blockPos, Direction.UP);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        //todo: fix client desync when running
        if (state.getValue(PART) == BedPart.HEAD && !level.isClientSide) {
            BlockPos blockPos = pos.relative(state.getValue(FACING).getOpposite());
            level.setBlock(blockPos, state.setValue(PART, BedPart.FOOT), 3);
            level.blockUpdated(pos, Blocks.AIR);
            state.updateNeighbourShapes(level, pos, 3);

            if (placer instanceof Player player) {
                this.use(state, level, pos, player, placer.getUsedItemHand(), new BlockHitResult(Vec3.atBottomCenterOf(pos), Direction.DOWN, pos, false));
            }
        } else {
            super.setPlacedBy(level, pos, state, placer, stack);
        }
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos currentPos, BlockPos neighborPos) {
        if (direction == Direction.DOWN && !neighborState.isFaceSturdy(level, neighborPos, Direction.UP)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, currentPos, neighborPos);
    }
}
