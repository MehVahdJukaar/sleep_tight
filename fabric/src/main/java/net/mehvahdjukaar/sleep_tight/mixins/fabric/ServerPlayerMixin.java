package net.mehvahdjukaar.sleep_tight.mixins.fabric;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.mehvahdjukaar.sleep_tight.STPlatStuff;
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


    @Inject(method = "tick", at = @At("HEAD"))
    public void sleep_tight$tickData(CallbackInfo ci) {

        PlayerSleepData data = STPlatStuff.getPlayerSleepData((Player) (Object) this);
        if (data != null) {
            data.tick((ServerPlayer) (Object) this);
        }
    }

    @WrapOperation(method = "startSleepInBed", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/dimension/DimensionType;natural()Z"))
    public boolean  sleep_tight$canSleepInDimension(DimensionType instance, Operation<Boolean> original) {
        boolean n = instance.natural();
        if (!n && !CommonConfigs.EXPLOSION_BEHAVIOR.get().canExplode()) {
            return true;
        }
        return original.call(instance);
    }
}

//1.20 belo

@Mixin(ServerPlayer.class)
public class ServerPlayerMixin {


    @Inject(method = "tick", at = @At("HEAD"))
    public void tick(CallbackInfo ci) {

        PlayerSleepData data = STPlatStuff.getPlayerSleepData((Player) (Object) this);
        if (data != null) {
            data.tick((ServerPlayer) (Object) this);
        }
    }
}

