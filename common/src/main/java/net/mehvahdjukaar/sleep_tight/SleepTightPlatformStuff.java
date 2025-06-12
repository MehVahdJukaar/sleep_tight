package net.mehvahdjukaar.sleep_tight;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Unit;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.mehvahdjukaar.sleep_tight.common.tiles.IExtraBedDataProvider;
import net.mehvahdjukaar.sleep_tight.core.BedData;
import net.mehvahdjukaar.sleep_tight.core.PlayerSleepData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SleepTightPlatformStuff {

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
