package net.mehvahdjukaar.sleep_tight.common;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class NightBagItem extends BlockItem {
    public NightBagItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        BlockPos pos = new BlockPos(player.position().add(0, 1 / 16f, 0));
        BlockHitResult hit = new BlockHitResult(Vec3.atBottomCenterOf(pos).add(0, 1, 0), Direction.DOWN, pos, false);
        BlockPlaceContext context = new BlockPlaceContext(player, usedHand, stack, hit);
        var r = this.place(context);

        return switch (r) {
            case SUCCESS ->InteractionResultHolder.consume(stack); //no swing anim
            case CONSUME, CONSUME_PARTIAL -> InteractionResultHolder.consume(stack);
            case FAIL -> {
                if(level.isClientSide) {
                    player.displayClientMessage(Component.translatable("message.sleep_tight.night_bag"),true);
                }
                yield  InteractionResultHolder.fail(stack);
            }
            default -> InteractionResultHolder.pass(stack);
        };
    }

    @Override
    protected boolean canPlace(BlockPlaceContext context, BlockState state) {
        return (!this.mustSurvive() || state.canSurvive(context.getLevel(), context.getClickedPos()));
    }
}
