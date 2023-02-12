package net.mehvahdjukaar.sleep_tight;

import net.mehvahdjukaar.moonlight.api.misc.EventCalled;
import net.mehvahdjukaar.sleep_tight.common.HammockBlock;
import net.mehvahdjukaar.sleep_tight.common.NightBagBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
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
}
