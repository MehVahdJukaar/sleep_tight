package net.mehvahdjukaar.sleep_tight.mixins.neoforge;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.mehvahdjukaar.sleep_tight.SleepTightPlatformStuff;
import net.mehvahdjukaar.sleep_tight.configs.CommonConfigs;
import net.mehvahdjukaar.sleep_tight.core.PlayerSleepData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public class ServerPlayerMixin {


    //Better here than event for compat since it makes vanilla logic run
    @WrapOperation(method = "lambda$startSleepInBed$13", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/dimension/DimensionType;natural()Z"))
    public boolean  sleep_tight$canSleepInDimension(DimensionType instance, Operation<Boolean> original) {
        boolean n = instance.natural();
        if (!n && !CommonConfigs.EXPLOSION_BEHAVIOR.get().canExplode()) {
            return true;
        }
        return original.call(instance);
    }
}
