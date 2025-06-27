package net.mehvahdjukaar.sleep_tight.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import net.mehvahdjukaar.sleep_tight.STPlatStuff;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.SleepStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(SleepStatus.class)
public abstract class SleepStatusMixin {

    @Shadow
    private int activePlayers;

    @Inject(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;isSleeping()Z",
            shift = At.Shift.BEFORE))
    public void sleep_tight$removeOnCooldown(List<ServerPlayer> players, CallbackInfoReturnable<Boolean> cir,
                                             @Local ServerPlayer serverPlayer) {
        try {
            if (STPlatStuff.getPlayerSleepData(serverPlayer).isOnSleepCooldown(serverPlayer)) {
                this.activePlayers--;
            }
        }catch (Exception e){
            //just here since this can be called before cap is assigned. band-aid for dumb forge
        }
    }
}
