package net.mehvahdjukaar.sleep_tight.integration;

import earth.terrarium.handcrafted.common.blocks.FancyBedBlock;
import earth.terrarium.handcrafted.common.registry.ModBlocks;
import earth.terrarium.handcrafted.common.utils.InteractionUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.BlockHitResult;

public class HandcraftedCompat {

    public static Block[] getAllFancyBeds() {
        return ModBlocks.FANCY_BEDS.boundStream().toArray(Block[]::new);
    }

    public static ItemInteractionResult placeSheet(BlockState state, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (state.getBlock() instanceof FancyBedBlock) {
            ItemStack stack = player.getItemInHand(hand);
            if (state.getValue(BedBlock.PART) == BedPart.HEAD) {
                return InteractionUtils.interactCushion(state, player.level(), pos, player, stack, FancyBedBlock.COLOR);
            } else {
                return InteractionUtils.interactSheet(state, player.level(), pos, player, stack, FancyBedBlock.COLOR);
            }
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
}
