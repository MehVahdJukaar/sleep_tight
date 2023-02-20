package net.mehvahdjukaar.sleep_tight;

import net.mehvahdjukaar.moonlight.api.misc.EventCalled;
import net.mehvahdjukaar.moonlight.api.util.Utils;
import net.mehvahdjukaar.sleep_tight.common.BedEntity;
import net.mehvahdjukaar.sleep_tight.common.HammockBlock;
import net.mehvahdjukaar.sleep_tight.common.IModBed;
import net.mehvahdjukaar.sleep_tight.common.NightBagBlock;
import net.mehvahdjukaar.sleep_tight.configs.CommonConfigs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class ModEvents {

    @EventCalled
    public static long getWakeTime(ServerLevel level, long currentTime) {
        if (level.isDay()) {
            boolean success = false;

            for (Player player : level.players()) {
                var p = player.getSleepingPos();
                if (p.isPresent() && player.isSleepingLongEnough() &&
                        level.getBlockState(p.get()).getBlock() instanceof HammockBlock) {
                    success = true;
                    break;
                }
            }
            if (success) {
                long i = level.getDayTime() + 24000L;
                return (i - i % 24000L) - 12001L;
            }
        }
        return currentTime;
    }

    @EventCalled
    public static boolean canSetSpawn(Player player, @Nullable BlockPos pos) {
        if (pos != null) {
            Level level = player.getLevel();
            if (!level.isClientSide) {
                return !(level.getBlockState(pos).getBlock() instanceof NightBagBlock);
            }
        }
        return true;
    }

    @EventCalled
    public static InteractionResult onCheckSleepTime(Level level, BlockPos pos) {
        long t = level.getDayTime() % 24000L;
        if (level.getBlockState(pos).getBlock() instanceof HammockBlock) {
            if (t > 500L && t < 11500L) {
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.CONSUME;
    }

    @EventCalled
    public static InteractionResult onRightClickBlock(Player player, Level level, InteractionHand hand, BlockHitResult hitResult) {
        if (!player.isSpectator()) { //is this check even needed?
            BlockPos pos = hitResult.getBlockPos();
            var state = level.getBlockState(pos);
            Block b = state.getBlock();
            if (state.getBlock() instanceof BedBlock) {

                if (state.getValue(BedBlock.PART) != BedPart.HEAD) {
                    pos = pos.relative(state.getValue(BedBlock.FACING));
                    state = level.getBlockState(pos);
                    if (!state.is(b)) {
                        return InteractionResult.PASS;
                    }
                }

                BedEntity.layDown(state, level, pos, player);
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    @EventCalled
    public static Vec3 getSleepingPosition(BlockState state, BlockPos pos) {
        if(state.getBlock() instanceof IModBed hammockBlock){
            return hammockBlock.getSleepingPosition(state, pos);
        }else if(state.is(BlockTags.BEDS) && CommonConfigs.FIX_BED_POSITION.get()){
            //vanilla places player 2 pixels above bed. Player then falls down
           return new Vec3(pos.getX() + 0.5, pos.getY() + 9/16f, pos.getZ() + 0.5);
        }
        return null;
    }
}
