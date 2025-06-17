package net.mehvahdjukaar.sleep_tight;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.mehvahdjukaar.sleep_tight.core.BedData;
import net.mehvahdjukaar.sleep_tight.core.ModEvents;
import net.mehvahdjukaar.sleep_tight.core.PlayerSleepData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class STPlatStuff {

    @NotNull
    @Contract
    @ExpectPlatform
    public static PlayerSleepData getPlayerSleepData(Player player) {
        throw new AssertionError();
    }

    //get bed data. 1 per bed. both ved positions can be passed here
    @ApiStatus.Internal
    @Nullable
    @Contract
    @ExpectPlatform
    public static BedData getBedDataFromThis(BlockEntity be) {
        throw new AssertionError();
    }

    @Nullable
    public static BedData getBedData(Level level, BlockPos pos) {
        return getBedData(level, pos, null);
    }

    @Contract
    @Nullable
    public static BedData getBedData(Level level, BlockPos pos, @Nullable BlockEntity be) {
        BlockState state = be == null ? level.getBlockState(pos) : be.getBlockState();
        BlockPos headPos = ModEvents.getBedHead(state, pos);
        if (be == null || !headPos.equals(pos)) {
            be = level.getBlockEntity(headPos);
        }
        if (be != null) {
            return getBedDataFromThis(be);
        }
        return null;
    }

    @Contract
    @ExpectPlatform
    public static Player.BedSleepingProblem invokeSleepChecksEvents(Player player, BlockPos pos) {
        throw new AssertionError();
    }


}
