package net.mehvahdjukaar.sleep_tight.common.items;

import net.mehvahdjukaar.sleep_tight.SleepTight;
import net.mehvahdjukaar.sleep_tight.common.blocks.InfestedBedBlock;
import net.mehvahdjukaar.sleep_tight.common.tiles.InfestedBedTile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class BedbugEggsItem extends Item {
    public BedbugEggsItem(Properties properties) {
        super(properties);
    }

    public InteractionResult useOnBed(Player player, InteractionHand hand, ItemStack stack, BlockState state, BlockPos pos, BlockHitResult hit) {
        if (InfestedBedBlock.convertBed(player.level, state, pos)) {
            Level level = player.level;
            player.awardStat(Stats.ITEM_USED.get(this));
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            level.playSound(player, pos, SoundEvents.TURTLE_EGG_CRACK, SoundSource.PLAYERS, 0.6f, 1.7f);
            level.playSound(player, pos, SoundEvents.SILVERFISH_STEP, SoundSource.PLAYERS, 1, 1f);

            if (level.isClientSide) {
                Vec3 h = hit.getLocation();
                for (int i = 0; i < 8; ++i) {
                    level.addParticle(new ItemParticleOption(ParticleTypes.ITEM, this.getDefaultInstance()),
                            h.x, h.y + 0.1, h.z,
                            (level.random.nextFloat() - 0.5) * 0.08,
                            (level.random.nextFloat() - 0.5) * 0.08,
                            (level.random.nextFloat() - 0.5) * 0.08);
                }
            }

            return InteractionResult.SUCCESS;
            //particles
        }
        return InteractionResult.PASS;
    }


}
