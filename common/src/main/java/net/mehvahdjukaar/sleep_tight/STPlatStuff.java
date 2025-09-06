package net.mehvahdjukaar.sleep_tight;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Unit;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.mehvahdjukaar.sleep_tight.core.BedData;
import net.mehvahdjukaar.sleep_tight.core.ModEvents;
import net.mehvahdjukaar.sleep_tight.core.PlayerSleepData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class STPlatStuff {

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
            return SleepTight.BED_DATA.getOrNull(be);
        }
        return null;
    }

    @NotNull
    @Contract
    @ExpectPlatform
    public static PlayerSleepData getPlayerSleepData(Player player) {
        throw new AssertionError();
    }

    @Contract
    @ExpectPlatform
    public static Either<Player.BedSleepingProblem, Unit> invokeSleepChecksEvents(ServerPlayer player, BlockPos pos) {
        throw new AssertionError();
    }


}
