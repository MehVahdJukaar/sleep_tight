package net.mehvahdjukaar.sleep_tight.fabric;

import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.mehvahdjukaar.sleep_tight.core.ModEvents;
import net.mehvahdjukaar.sleep_tight.core.PlayerSleepData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class SleepTightPlatformStuffImpl {
    @org.jetbrains.annotations.Contract
    public static PlayerSleepData getPlayerSleepData(Player player) {
        return ((ISleepTightPlayer) player).st$getSleepData();
    }

    @org.jetbrains.annotations.Contract
    public static Player.BedSleepingProblem invokeSleepChecksEvents(Player player, BlockPos pos) {
        if( !ModEvents.checkExtraSleepConditions(player, pos)){
            return null; //idk why but we need this here to match forge (called by event there)
        }

        if (!player.isSleeping() && player.isAlive()) {
            Level level = player.level();
            if (!level.dimensionType().natural()) {
                return Player.BedSleepingProblem.NOT_POSSIBLE_HERE;
            }

            if (!player.isCreative()) {
                Vec3 vec3 = Vec3.atBottomCenterOf(pos);
                List<Monster> list = level.getEntitiesOfClass(Monster.class,
                        new AABB(vec3.x() - 8.0, vec3.y() - 5.0, vec3.z() - 8.0, vec3.x() + 8.0, vec3.y() + 5.0, vec3.z() + 8.0),
                        m -> m.isPreventingPlayerRest(player)
                );
                if (!hasNoMonstersNearby(player, list, pos)) {
                    return Player.BedSleepingProblem.NOT_SAFE;
                }
            }

            if (isDay(player, pos)) {
                return Player.BedSleepingProblem.NOT_POSSIBLE_NOW;
            }
        }
        return null;
    }

    //same as fabric mixin

    private static boolean isDay(Player player, BlockPos pos) {
        boolean day = player.level().isDay();
        InteractionResult result = EntitySleepEvents.ALLOW_SLEEP_TIME.invoker().allowSleepTime(player, pos, !day);

        if (result != InteractionResult.PASS) {
            return !result.consumesAction(); // true from the event = night-like conditions, so we have to invert
        }

        return day;
    }

    private static boolean hasNoMonstersNearby(Player player, List<Monster> monsters, BlockPos pos) {
        boolean vanillaResult = monsters.isEmpty();
        InteractionResult result = EntitySleepEvents.ALLOW_NEARBY_MONSTERS.invoker().allowNearbyMonsters(player, pos, vanillaResult);
        return result != InteractionResult.PASS ? result.consumesAction() : vanillaResult;
    }
}
