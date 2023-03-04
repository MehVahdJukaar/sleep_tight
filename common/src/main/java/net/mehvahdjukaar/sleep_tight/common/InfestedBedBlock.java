package net.mehvahdjukaar.sleep_tight.common;

import net.mehvahdjukaar.moonlight.api.client.util.ParticleUtil;
import net.mehvahdjukaar.moonlight.api.set.BlocksColorAPI;
import net.mehvahdjukaar.sleep_tight.network.ClientBoundParticlePacket;
import net.mehvahdjukaar.sleep_tight.network.NetworkHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.LingeringPotionItem;
import net.minecraft.world.item.SplashPotionItem;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;


public class InfestedBedBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final EnumProperty<BedPart> PART = BlockStateProperties.BED_PART;
    public static final VoxelShape NORTH_SHAPE = Blocks.WHITE_BED.defaultBlockState().setValue(FACING, Direction.SOUTH).getShape(null, null);
    public static final VoxelShape SOUTH_SHAPE = Blocks.WHITE_BED.defaultBlockState().setValue(FACING, Direction.NORTH).getShape(null, null);
    public static final VoxelShape WEST_SHAPE = Blocks.WHITE_BED.defaultBlockState().setValue(FACING, Direction.EAST).getShape(null, null);
    public static final VoxelShape EAST_SHAPE = Blocks.WHITE_BED.defaultBlockState().setValue(FACING, Direction.WEST).getShape(null, null);

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

    @Override
    public long getSeed(BlockState state, BlockPos pos) {
        BlockPos blockPos = pos.relative(state.getValue(FACING), state.getValue(PART) == BedPart.HEAD ? 0 : 1);
        return Mth.getSeed(blockPos.getX(), pos.getY(), blockPos.getZ());
    }

    @Override
    public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType type) {
        return false;
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
        super.fallOn(level, state, pos, entity, fallDistance * 0.5F);
    }

    @Override
    public void updateEntityAfterFallOn(BlockGetter level, Entity entity) {
        if (entity.isSuppressingBounce()) {
            super.updateEntityAfterFallOn(level, entity);
        } else {
            this.bounceUp(entity);
        }

    }

    private void bounceUp(Entity entity) {
        Vec3 vec3 = entity.getDeltaMovement();
        if (vec3.y < 0.0) {
            double d = entity instanceof LivingEntity ? 1.0 : 0.8;
            entity.setDeltaMovement(vec3.x, -vec3.y * 0.6600000262260437 * d, vec3.z);
        }
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos currentPos, BlockPos neighborPos) {
        if (direction == getNeighbourDirection(state.getValue(PART), state.getValue(FACING))) {
            return neighborState.is(this) && neighborState.getValue(PART) != state.getValue(PART) ? state : Blocks.AIR.defaultBlockState();
        } else {
            return super.updateShape(state, direction, neighborState, level, currentPos, neighborPos);
        }
    }

    private static Direction getNeighbourDirection(BedPart part, Direction direction) {
        return part == BedPart.FOOT ? direction : direction.getOpposite();
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (HammockBlock.tryExploding(level, pos)) {
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
                    convertToBed(level, state, hit.getBlockPos());
                    return;
                }
            }
        }
        super.onProjectileHit(level, state, hit, projectile);
    }

    public void convertToBed(Level level, BlockState state, BlockPos blockPos) {
        if (level.getBlockEntity(blockPos) instanceof InfestedBedTile tile) {
            BlockPos neighbor = blockPos.relative(getNeighbourDirection(state.getValue(PART), state.getValue(FACING)));
            if (!level.isClientSide) {
                NetworkHandler.CHANNEL.sendToAllClientPlayersInRange(level, blockPos,32, new ClientBoundParticlePacket(neighbor, blockPos));
            }
            Block bed = BlocksColorAPI.getColoredBlock("bed", tile.getColor());
            if (bed != null) {
                level.setBlock(blockPos, bed.withPropertiesOf(state), 2 | Block.UPDATE_KNOWN_SHAPE);
                level.setBlock(neighbor, bed.withPropertiesOf(level.getBlockState(neighbor)), 2 | Block.UPDATE_KNOWN_SHAPE);
                level.playSound(null, blockPos, SoundEvents.SILVERFISH_DEATH, SoundSource.BLOCKS, 1, 1.3f);
            }
        }
    }

    @Override
    public void spawnAfterBreak(BlockState state, ServerLevel level, BlockPos pos, ItemStack stack, boolean bl) {
        super.spawnAfterBreak(state, level, pos, stack, bl);
    }
}
