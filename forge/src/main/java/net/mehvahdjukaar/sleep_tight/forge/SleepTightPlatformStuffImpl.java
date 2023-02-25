package net.mehvahdjukaar.sleep_tight.forge;


import net.mehvahdjukaar.sleep_tight.core.PlayerSleepData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.ForgeEventFactory;

import javax.annotation.Nullable;
import java.util.Optional;

public class SleepTightPlatformStuffImpl {


    @org.jetbrains.annotations.Contract
    public static PlayerSleepData getPlayerSleepCap(Player player) {
        return player.getCapability(ForgePlayerSleepCapability.TOKEN).orElse(null);
    }

    @org.jetbrains.annotations.Contract
    @Nullable
    public static Player.BedSleepingProblem invokeSleepChecksEvents(Player player, BlockPos pos) {
        Player.BedSleepingProblem ret = ForgeEventFactory.onPlayerSleepInBed(player, Optional.of(pos));
        if (ret != null) {
            return ret;
        }
        if (!player.isSleeping() && player.isAlive()) {
            if (!player.level.dimensionType().natural()) {
                return Player.BedSleepingProblem.NOT_POSSIBLE_HERE;
            }
            if (!ForgeEventFactory.fireSleepingTimeCheck(player, Optional.of(pos))) {
                return Player.BedSleepingProblem.NOT_POSSIBLE_NOW;
            }
        }
        return null;
    }

}
