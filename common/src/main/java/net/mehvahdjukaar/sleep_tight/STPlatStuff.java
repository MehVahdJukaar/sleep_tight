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
import net.minecraft.world.level.block.entity.BedBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class STPlatStuff {

    public static PlayerSleepData getPlayerSleepData(Player player) {
        return SleepTight.PLAYER_DATA.getOrCreate(player);
    }

    @Nullable
    public static BedData getBedDataIfPresent(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof BedBlockEntity be) {
            return getBedDataIfPresent(be);
        }
        return null;
    }

    @Nullable
    public static BedData getBedDataIfPresent(BlockEntity be) {
        BlockState state = be.getBlockState();
        BlockPos pos = be.getBlockPos();
        Level level = be.getLevel();
        BlockPos headPos = ModEvents.getBedHead(state, pos);
        if (!headPos.equals(pos)) {
            be = level.getBlockEntity(headPos);
            if (be != null) {
                return SleepTight.BED_DATA.getOrNull(be);
            }
            return null;
        }
        return SleepTight.BED_DATA.getOrNull(be);
    }

    @Contract
    @ExpectPlatform
    public static Either<Player.BedSleepingProblem, Unit> invokeSleepChecksEvents(ServerPlayer player, BlockPos pos) {
        throw new AssertionError();
    }


}
