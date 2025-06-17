package net.mehvahdjukaar.sleep_tight.common.blocks;

import net.mehvahdjukaar.moonlight.api.block.IRotatable;
import net.mehvahdjukaar.moonlight.api.block.IWashable;
import net.mehvahdjukaar.moonlight.api.block.MimicBlock;
import net.mehvahdjukaar.sleep_tight.SleepTight;
import net.mehvahdjukaar.sleep_tight.common.entities.BedbugEntity;
import net.mehvahdjukaar.sleep_tight.common.network.ClientBoundParticleMessage;
import net.mehvahdjukaar.sleep_tight.common.network.NetworkHandler;
import net.mehvahdjukaar.sleep_tight.common.tiles.InfestedBedTile;
import net.minecraft.client.renderer.blockentity.BedRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.LingeringPotionItem;
import net.minecraft.world.item.SplashPotionItem;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;


public class InfestedBedBlock extends MimicBlock implements IWashable, EntityBlock, IRotatable, SimpleWaterloggedBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<BedPart> BED_PART = BlockStateProperties.BED_PART;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public InfestedBedBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.getStateDefinition().any()
                .setValue(WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING, BED_PART, WATERLOGGED);
    }

    @Override
    public boolean tryWash(Level level, BlockPos pos, BlockState state, Vec3 hitPos) {
        if (level.getBlockEntity(pos) instanceof InfestedBedTile tile) {
            BlockState oldBlock = tile.getHeldBlock();
            // BlocksColorAPI.changeColor()
            return true;
        }
        return false;
    }

    @Override
    public Optional<BlockState> getRotatedState(BlockState blockState, LevelAccessor levelAccessor, BlockPos blockPos, Rotation rotation, Direction axis, @Nullable Vec3 vec3) {
        if (axis.getAxis().isVertical()) {
            return Optional.of(rotate(blockState, rotation));
        }
        return Optional.empty();
    }

    @Override
    public void onRotated(BlockState newState, BlockState oldState, LevelAccessor world, BlockPos pos, Rotation rotation, Direction axis, @Nullable Vec3 hit) {
        IRotatable.super.onRotated(newState, oldState, world, pos, rotation, axis, hit);
        if (world.getBlockEntity(pos) instanceof InfestedBedTile tile) {
            tile.setHeldBlock(tile.getHeldBlock().rotate(rotation));
        }
    }

    @Override
    public BlockState rotate(BlockState blockState, Rotation rotation) {
        return blockState.setValue(FACING, rotation.rotate(blockState.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState blockState, Mirror mirror) {
        return blockState.rotate(mirror.getRotation(blockState.getValue(FACING)));
    }

    @Override
    public VoxelShape getShape(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, CollisionContext collisionContext) {
        if (blockGetter.getBlockEntity(blockPos) instanceof InfestedBedTile tile) {
            BlockState heldBlock = tile.getHeldBlock();
            return heldBlock.getShape(blockGetter, blockPos, collisionContext);
        }
        return Shapes.empty();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new InfestedBedTile(pos, state);
    }

    @Override
    public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType type) {
        return false;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);
        if (random.nextFloat() < 0.3) {
            float x = pos.getX() + level.random.nextFloat();
            float z = pos.getZ() + level.random.nextFloat();
            float y = pos.getY() + 9 / 16f;
            level.addParticle(SleepTight.BEDBUG_PARTICLE.get(), x, y + 0.01, z, 0, 0, 0);
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (IModBed.tryExploding(level, pos)) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        ItemStack stack = player.getItemInHand(hand);
        if (stack.getItem() instanceof LingeringPotionItem || stack.getItem() instanceof SplashPotionItem) {
            return InteractionResult.PASS;
        }
        player.displayClientMessage(Component.translatable("message.sleep_tight.bedbug"), true);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onProjectileHit(Level level, BlockState state, BlockHitResult hit, Projectile projectile) {
        if (projectile instanceof ThrownPotion tp) {
            Potion p = PotionUtils.getPotion(tp.getItem());
            for (var e : p.getEffects()) {
                if (e.getEffect() == MobEffects.HARM) {
                    restoreBed(level, state, hit.getBlockPos());
                    return;
                }
            }
        }
        super.onProjectileHit(level, state, hit, projectile);
    }

    private static Direction getNeighbourDirection(BedPart part, Direction direction) {
        return part == BedPart.FOOT ? direction : direction.getOpposite();
    }

    @Override
    public void spawnAfterBreak(BlockState state, ServerLevel level, BlockPos pos, ItemStack stack, boolean bl) {
        super.spawnAfterBreak(state, level, pos, stack, bl);
        if (level.getBlockEntity(pos) instanceof InfestedBedTile tile) {
            BlockState bedState = tile.getHeldBlock();
            if (bedState.getValue(BedBlock.PART) == BedPart.FOOT && level.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS)) {
                BedbugEntity entity = SleepTight.BEDBUG_ENTITY.get().create(level);
                entity.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
                level.addFreshEntity(entity);
                entity.spawnAnim();
            }
        }
    }


    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        if (level.getBlockEntity(pos) instanceof InfestedBedTile tile) {
            return tile.getHeldBlock().getBlock().asItem().getDefaultInstance();
        }
        return getCloneItemStack(level, pos, state);
    }

    //water stuff
    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public BlockState updateShape(BlockState stateIn, Direction facing, BlockState facingState, LevelAccessor worldIn, BlockPos currentPos, BlockPos facingPos) {
        if (stateIn.getValue(WATERLOGGED)) {
            worldIn.scheduleTick(currentPos, Fluids.WATER, Fluids.WATER.getTickDelay(worldIn));
        }

        return stateIn;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        FluidState fluidstate = context.getLevel().getFluidState(context.getClickedPos());
        return this.defaultBlockState().setValue(WATERLOGGED, fluidstate.is(FluidTags.WATER) && fluidstate.getAmount() == 8);
    }


    public static void restoreBed(Level level, BlockState myInfested, BlockPos blockPos) {

        if (level.getBlockEntity(blockPos) instanceof InfestedBedTile tile) {
            BlockState heldBedState = tile.getHeldBlock();
            Direction dir = getNeighbourDirection(heldBedState.getValue(BedBlock.PART), myInfested.getValue(FACING));
            BlockPos otherBedPos = blockPos.relative(dir);

            BlockState otherInfested = level.getBlockState(otherBedPos);

            if (level.getBlockEntity(otherBedPos) instanceof InfestedBedTile neighborTile) {
                BlockState otherHeldBedState = neighborTile.getHeldBlock();
                //water
                if (otherHeldBedState.hasProperty(WATERLOGGED)) {
                    otherHeldBedState = otherHeldBedState.setValue(WATERLOGGED, otherInfested.getValue(WATERLOGGED));
                }
                level.setBlock(blockPos, otherHeldBedState, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                BlockEntity newOtherTile = level.getBlockEntity(blockPos);
                if (newOtherTile != null && tile.getInner() != null) {
                    newOtherTile.load(tile.getInner().saveWithoutMetadata());
                }
            }
            //if the bed is waterlogged, we need to remove that
            if (heldBedState.hasProperty(WATERLOGGED)) {
                heldBedState = heldBedState.setValue(WATERLOGGED, myInfested.getValue(WATERLOGGED));
            }
            level.setBlock(blockPos, heldBedState, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            BlockEntity newTile = level.getBlockEntity(blockPos);
            if (newTile != null && tile.getInner() != null) {
                newTile.load(tile.getInner().saveWithoutMetadata());
            }

            level.playSound(null, blockPos, SoundEvents.SILVERFISH_DEATH, SoundSource.BLOCKS, 1, 1.3f);
            if (!level.isClientSide) {
                NetworkHandler.CHANNEL.sendToAllClientPlayersInRange(level, blockPos, 32,
                        ClientBoundParticleMessage.bedbugInfest(blockPos, dir));
            }
        }
    }

    public static boolean infestBed(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (BedbugEntity.isValidBedForInfestation(state)) {
            Direction dir = state.getValue(BedBlock.FACING);
            BlockPos neighborPos = pos.relative(state.getValue(BedBlock.PART) == BedPart.FOOT ? dir : dir.getOpposite());

            BlockEntity oldTile = level.getBlockEntity(pos);
            BlockEntity oldNeighborTile = level.getBlockEntity(neighborPos);

            BlockState oldNeighborState = level.getBlockState(neighborPos);

            level.setBlock(pos, SleepTight.INFESTED_BED.get().withPropertiesOf(state), Block.UPDATE_KNOWN_SHAPE | 2);
            level.setBlock(neighborPos, SleepTight.INFESTED_BED.get().withPropertiesOf(level.getBlockState(neighborPos)), 2);

            if (level.getBlockEntity(pos) instanceof InfestedBedTile tile) {
                if (state.hasProperty(WATERLOGGED)) {
                    state = state.setValue(WATERLOGGED, false);
                }
                tile.setBed(state, oldTile);
            }
            if (level.getBlockEntity(neighborPos) instanceof InfestedBedTile tile) {
                if (oldNeighborState.hasProperty(WATERLOGGED)) {
                    oldNeighborState = oldNeighborState.setValue(WATERLOGGED, false);
                }
                tile.setBed(oldNeighborState, oldNeighborTile);
            }
            return true;
        }
        return false;
    }

}
