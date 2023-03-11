package net.mehvahdjukaar.sleep_tight.fabric;

import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.mehvahdjukaar.sleep_tight.core.PlayerSleepData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;

public class SleepTightPlatformStuffImpl {
    @org.jetbrains.annotations.Contract
    public static PlayerSleepData getPlayerSleepData(Player player) {
        return ((ISleepTightPlayer) player).getSleepData();
    }

    @org.jetbrains.annotations.Contract
    public static Player.BedSleepingProblem invokeSleepChecksEvents(Player player, BlockPos pos) {
        if (!player.isSleeping() && player.isAlive()) {
            if (!player.level.dimensionType().natural()) {
                return Player.BedSleepingProblem.NOT_POSSIBLE_HERE;
            }
            if (isDay(player, pos)) {
                return Player.BedSleepingProblem.NOT_POSSIBLE_NOW;
            }
        }
        return null;
    }

    //same as fabric mixin
    private static boolean isDay(Player player, BlockPos pos) {
        boolean day = player.level.isDay();
        InteractionResult result = EntitySleepEvents.ALLOW_SLEEP_TIME.invoker().allowSleepTime(player, pos, !day);

        if (result != InteractionResult.PASS) {
            return !result.consumesAction(); // true from the event = night-like conditions, so we have to invert
        }

        return day;
    }
}
