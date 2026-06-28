package net.mehvahdjukaar.sleep_tight.forge;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Unit;
import net.mehvahdjukaar.sleep_tight.core.BedData;
import net.mehvahdjukaar.sleep_tight.core.PlayerSleepData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;
import org.jetbrains.annotations.Contract;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public class STPlatStuffImpl {

    @Contract
    public static PlayerSleepData getPlayerSleepData(Player player) {
        if (player.isDeadOrDying()) {
            player.reviveCaps();
        }
        return player.getCapability(ForgePlayerSleepCapability.TOKEN).orElseThrow(
                () -> new IllegalStateException("Player sleep capability was null. How? ")
        );
    }

    public static @Nullable BedData getBedDataFromThis(BlockEntity be) {
        return be.getCapability(ForgeBedCapability.TOKEN).orElse(null);
    }

    @Contract
    public static Either<Player.BedSleepingProblem, Unit> invokeSleepChecksEvents(ServerPlayer player, BlockPos pos) {
        Player.BedSleepingProblem ret = ForgeEventFactory.onPlayerSleepInBed(player, Optional.of(pos));
        if (ret != null) {
            return Either.left(ret);
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
                if (!list.isEmpty()) {
                    return Either.left(Player.BedSleepingProblem.NOT_SAFE);
                }
            }

            if (!ForgeEventFactory.fireSleepingTimeCheck(player, Optional.of(pos))) {
                return Either.left(Player.BedSleepingProblem.NOT_POSSIBLE_NOW);
            }
        }
        return Either.right(Unit.INSTANCE);
    }

}
