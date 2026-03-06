package net.mehvahdjukaar.sleep_tight.fabric;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Unit;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.mehvahdjukaar.sleep_tight.core.BedData;
import net.mehvahdjukaar.sleep_tight.core.ModEvents;
import net.mehvahdjukaar.sleep_tight.core.PlayerSleepData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class STPlatStuffImpl {

    @org.jetbrains.annotations.Contract
    public static Either<Player.BedSleepingProblem, Unit> invokeSleepChecksEvents(ServerPlayer player, BlockPos pos) {
        if (!ModEvents.checkExtraSleepConditions(player, pos)) {
            return Either.right(Unit.INSTANCE); //idk why but we need this here to match forge (called by event there)
        }

        if (!player.isSleeping() && player.isAlive()) {
            Level level = player.level();
            if (!level.dimensionType().natural()) {
                return Either.left(Player.BedSleepingProblem.NOT_POSSIBLE_HERE);
            }

            if (!player.isCreative()) {
                Vec3 vec3 = Vec3.atBottomCenterOf(pos);
                List<Monster> list = level.getEntitiesOfClass(Monster.class,
                        new AABB(vec3.x() - 8.0, vec3.y() - 5.0, vec3.z() - 8.0, vec3.x() + 8.0, vec3.y() + 5.0, vec3.z() + 8.0),
                        m -> m.isPreventingPlayerRest(player)
                );
                if (!hasNoMonstersNearby(player, list, pos)) {
                    return Either.left(Player.BedSleepingProblem.NOT_SAFE);
                }
            }

            if (isDay(player, pos)) {
                return Either.left(Player.BedSleepingProblem.NOT_POSSIBLE_NOW);
            }
        }
        return Either.right(Unit.INSTANCE);
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
