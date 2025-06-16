package net.mehvahdjukaar.sleep_tight.mixins.fabric;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.mehvahdjukaar.sleep_tight.core.ModEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.portal.DimensionTransition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PlayerList.class)
public class PlayerListMixin {

    @WrapOperation(method = "respawn", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayer;findRespawnPositionAndUseSpawnBlock(ZLnet/minecraft/world/level/portal/DimensionTransition$PostDimensionTransition;)Lnet/minecraft/world/level/portal/DimensionTransition;"))
    private DimensionTransition sleep_tight$cancelSpawnWhenNotHomeBed(
            ServerPlayer instance, boolean optional, DimensionTransition.PostDimensionTransition postDimensionTransition,
            Operation<DimensionTransition> operation, @Local(argsOnly = true) ServerPlayer sp) {
        var transition = operation.call(instance, optional, postDimensionTransition);

        if (ModEvents.shouldCancelRespawnHere(sp, transition)) {
            return DimensionTransition.missingRespawnBlock(sp.server.overworld(),
                    sp, transition.postDimensionTransition());
        }
        return transition;
    }
}
