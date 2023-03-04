package net.mehvahdjukaar.sleep_tight.common;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;


public class InfestedBedBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final EnumProperty<BedPart> PART = BlockStateProperties.BED_PART;
    public static final VoxelShape NORTH_SHAPE = Blocks.WHITE_BED.defaultBlockState().setValue(FACING, Direction.NORTH).getShape(null, null);
    public static final VoxelShape SOUTH_SHAPE = Blocks.WHITE_BED.defaultBlockState().setValue(FACING, Direction.SOUTH).getShape(null, null);
    public static final VoxelShape WEST_SHAPE = Blocks.WHITE_BED.defaultBlockState().setValue(FACING, Direction.WEST).getShape(null, null);
    public static final VoxelShape EAST_SHAPE = Blocks.WHITE_BED.defaultBlockState().setValue(FACING, Direction.EAST).getShape(null, null);

    public InfestedBedBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(PART, BedPart.FOOT));

    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(PART, FACING);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction direction = getConnectedDirection(state).getOpposite();
        return switch (direction) {
            case NORTH -> NORTH_SHAPE;
            case SOUTH -> SOUTH_SHAPE;
            case WEST -> WEST_SHAPE;
            default -> EAST_SHAPE;
        };
    }

    public static Direction getConnectedDirection(BlockState state) {
        Direction direction = state.getValue(FACING);
        return state.getValue(PART) == BedPart.HEAD ? direction.getOpposite() : direction;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new InfestedBedTile(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);
    }

}
