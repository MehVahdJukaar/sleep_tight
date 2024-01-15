package net.mehvahdjukaar.sleep_tight.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.mehvahdjukaar.sleep_tight.configs.CommonConfigs;
import net.mehvahdjukaar.sleep_tight.core.PlayerSleepData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Optional;

@Mixin(PlayerList.class)
public class PlayerListMixin {

    @WrapOperation(method = "respawn", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;findRespawnPositionAndUseSpawnBlock(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;FZZ)Ljava/util/Optional;"))
    private Optional<Vec3> sleep_tight$cancelSpawnWhenNotHomeBed(ServerLevel serverLevel, BlockPos spawnBlockPos,
                                                                 float playerOrientation, boolean isRespawnForced,
                                                                 boolean respawnAfterWinningTheGame,
                                                                 Operation<Optional<Vec3>> original, @Local ServerPlayer player) {
        if (!isRespawnForced && CommonConfigs.ONLY_RESPAWN_IN_HOME_BED.get()) {
            if (PlayerSleepData.getHomeBedIfHere(player, spawnBlockPos) == null) {
                return Optional.empty();
            }
        }
        return original.call(serverLevel, spawnBlockPos, playerOrientation, isRespawnForced, respawnAfterWinningTheGame);
    }
}
