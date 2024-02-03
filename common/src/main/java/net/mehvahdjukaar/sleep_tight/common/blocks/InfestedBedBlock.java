package net.mehvahdjukaar.sleep_tight.common.blocks;

import net.mehvahdjukaar.moonlight.api.block.IWashable;
import net.mehvahdjukaar.moonlight.api.platform.network.NetworkHelper;
import net.mehvahdjukaar.sleep_tight.SleepTight;
import net.mehvahdjukaar.sleep_tight.common.network.ModMessages;
import net.mehvahdjukaar.sleep_tight.common.tiles.InfestedBedTile;
import net.mehvahdjukaar.sleep_tight.common.entities.BedbugEntity;
import net.mehvahdjukaar.sleep_tight.common.network.ClientBoundParticleMessage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.LingeringPotionItem;
import net.minecraft.world.item.SplashPotionItem;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;


public class InfestedBedBlock extends BedBlock implements IWashable {

    public InfestedBedBlock(Properties properties) {
        super(DyeColor.BROWN, properties);
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
                    convertToBed(level, state, hit.getBlockPos());
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
        if (state.getValue(PART) == BedPart.FOOT && level.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS)) {
            BedbugEntity entity = SleepTight.BEDBUG_ENTITY.get().create(level);
            entity.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
            level.addFreshEntity(entity);
            entity.spawnAnim();
        }
    }

    @Override
    public boolean tryWash(Level level, BlockPos pos, BlockState state) {
        if (level.getBlockEntity(pos) instanceof InfestedBedTile tile) {
            //TODO:
            //tile.setHeldBlock(Blocks.WHITE_BED);
            return true;
        }
        return false;
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        if (level.getBlockEntity(pos) instanceof InfestedBedTile tile) {
            return tile.getHeldBlock().getBlock().asItem().getDefaultInstance();
        }
        return getCloneItemStack(level, pos, state);
    }

    public static void convertToBed(Level level, BlockState state, BlockPos blockPos) {
        if (level.getBlockEntity(blockPos) instanceof InfestedBedTile tile) {
            Direction dir = getNeighbourDirection(state.getValue(PART), state.getValue(FACING));
            BlockPos neighbor = blockPos.relative(dir);
            if (!level.isClientSide) {
                NetworkHelper.sendToAllClientPlayersInRange(level, blockPos, 32,
                        ClientBoundParticleMessage.bedbugInfest(blockPos, dir));
            }
            Block bed = tile.getBed().getBlock();
            if (bed != null) {
                level.setBlock(blockPos, bed.withPropertiesOf(state), 2 | Block.UPDATE_KNOWN_SHAPE);
                level.setBlock(neighbor, bed.withPropertiesOf(level.getBlockState(neighbor)), 2 | Block.UPDATE_KNOWN_SHAPE);
                level.playSound(null, blockPos, SoundEvents.SILVERFISH_DEATH, SoundSource.BLOCKS, 1, 1.3f);
            }
        }
    }

    public static boolean infestBed(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if(BedbugEntity.isValidBedForInfestation(state)) {
            level.setBlock(pos, SleepTight.INFESTED_BED.get().withPropertiesOf(state), Block.UPDATE_KNOWN_SHAPE | 2);
            Direction dir = state.getValue(BedBlock.FACING);
            BlockPos neighborPos = pos.relative(state.getValue(BedBlock.PART) == BedPart.FOOT ? dir : dir.getOpposite());
            level.setBlock(neighborPos, SleepTight.INFESTED_BED.get().withPropertiesOf(level.getBlockState(neighborPos)), 2);

            if (level.getBlockEntity(pos) instanceof InfestedBedTile tile) {
                tile.setHeldBlock(state.getBlock().withPropertiesOf(tile.getBlockState()));
            }
            if (level.getBlockEntity(neighborPos) instanceof InfestedBedTile tile) {
                tile.setHeldBlock(state.getBlock().withPropertiesOf(tile.getBlockState()));
            }
            return true;
        }
        return false;
    }

}
