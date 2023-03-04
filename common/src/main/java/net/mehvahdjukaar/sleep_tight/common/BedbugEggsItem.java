package net.mehvahdjukaar.sleep_tight.common;

import com.google.common.collect.ImmutableSet;
import net.mehvahdjukaar.sleep_tight.SleepTight;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;

import java.util.Set;

public class BedbugEggsItem extends Item {
    public BedbugEggsItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        BlockState state = level.getBlockState(pos);
        Block b = state.getBlock();
        if (BEDS.contains(b)) {
            //get head
            if (state.getValue(BedBlock.PART) != BedPart.HEAD) {
                pos = pos.relative(state.getValue(BedBlock.FACING));
                state = level.getBlockState(pos);
                if (!state.is(b)) {
                    return InteractionResult.PASS;
                }
            }
            level.setBlockAndUpdate(pos, SleepTight.INFESTED_BED.get().withPropertiesOf(state));
            return InteractionResult.SUCCESS;
            //particles
        }

        return super.useOn(context);
    }

    static final Set<Block> BEDS = ImmutableSet.of(Blocks.RED_BED, Blocks.BLACK_BED, Blocks.BLUE_BED, Blocks.BROWN_BED, Blocks.CYAN_BED, Blocks.GRAY_BED, Blocks.GREEN_BED, Blocks.LIGHT_BLUE_BED, Blocks.LIGHT_GRAY_BED, Blocks.LIME_BED, Blocks.MAGENTA_BED, Blocks.ORANGE_BED, Blocks.PINK_BED, Blocks.PURPLE_BED, Blocks.WHITE_BED, Blocks.YELLOW_BED);

    public InteractionResult useOnBed(Player player, InteractionHand hand, ItemStack itemInHand, BlockState state, BlockPos pos) {
        if (BEDS.contains(state.getBlock())) {
            Level level = player.level;
            level.setBlock(pos, SleepTight.INFESTED_BED.get().withPropertiesOf(state), Block.UPDATE_KNOWN_SHAPE | 2);
            BlockPos pos2 = pos.relative(state.getValue(BedBlock.FACING).getOpposite());
            level.setBlock(pos2, SleepTight.INFESTED_BED.get().withPropertiesOf(state.setValue(BedBlock.PART, BedPart.FOOT)), 2);

            DyeColor color = ((BedBlock) state.getBlock()).getColor();
            if (level.getBlockEntity(pos) instanceof InfestedBedTile tile) {
                tile.setColor(color);
            }
            if (level.getBlockEntity(pos2) instanceof InfestedBedTile tile) {
                tile.setColor(color);
            }
            level.playSound(player, pos, SoundEvents.TURTLE_EGG_CRACK, SoundSource.PLAYERS, 0.6f,1.7f);
            level.playSound(player, pos, SoundEvents.SILVERFISH_STEP, SoundSource.PLAYERS, 1,1f);
            return InteractionResult.SUCCESS;
            //particles
        }
        return InteractionResult.PASS;
    }
}
