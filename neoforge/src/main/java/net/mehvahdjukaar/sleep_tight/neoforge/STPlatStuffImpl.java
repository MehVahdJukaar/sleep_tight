package net.mehvahdjukaar.sleep_tight.neoforge;

import com.mojang.datafixers.util.Either;
import net.mehvahdjukaar.sleep_tight.core.PlayerSleepData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.Contract;

import java.util.List;

public class STPlatStuffImpl {

    @Contract
    public static PlayerSleepData getPlayerSleepData(Player player) {
        return player.getData(ForgePlayerSleepCapability.SLEEP_ATTACHMENT);
    }

    @Contract
    public static Either<Player.BedSleepingProblem, Unit> invokeSleepChecksEvents(ServerPlayer player, BlockPos bedPos) {
        Either<Player.BedSleepingProblem, Unit> vanillaResult = getVanillaSleepChecks(player, bedPos);
        vanillaResult = EventHooks.canPlayerStartSleeping(player, bedPos, vanillaResult);
        if (vanillaResult.left().isPresent()) {
            return vanillaResult;
        }
        return Either.right(Unit.INSTANCE);
    }


    //Stripped down version of vanilla one that doesbt check for the bed since we dont have it yet
    @Contract
    public static Either<Player.BedSleepingProblem, Unit> getVanillaSleepChecks(Player player, BlockPos bedPos) {
        if (player.isSleeping() || !player.isAlive()) {
            return Either.left(Player.BedSleepingProblem.OTHER_PROBLEM);
        } else if (!player.level().dimensionType().natural()) {
            return Either.left(Player.BedSleepingProblem.NOT_POSSIBLE_HERE);
        } else {
            if (player.level().isDay()) {
                return Either.left(Player.BedSleepingProblem.NOT_POSSIBLE_NOW);
            } else {
                if (!player.isCreative()) {
                    Vec3 vec3 = Vec3.atBottomCenterOf(bedPos);
                    List<Monster> list = player.level()
                            .getEntitiesOfClass(
                                    Monster.class,
                                    new AABB(vec3.x() - 8.0, vec3.y() - 5.0, vec3.z() - 8.0, vec3.x() + 8.0, vec3.y() + 5.0, vec3.z() + 8.0),
                                    argx -> argx.isPreventingPlayerRest(player)
                            );
                    if (!list.isEmpty()) {
                        return Either.left(Player.BedSleepingProblem.NOT_SAFE);
                    }
                }

                return Either.right(Unit.INSTANCE);
            }
        }
    }

}
