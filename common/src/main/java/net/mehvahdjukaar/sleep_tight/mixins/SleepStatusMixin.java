package net.mehvahdjukaar.sleep_tight.mixins;

import net.mehvahdjukaar.sleep_tight.SleepTightPlatformStuff;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.SleepStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Iterator;
import java.util.List;

@Mixin(SleepStatus.class)
public abstract class SleepStatusMixin {

    @Shadow private int activePlayers;

    @Inject(method = "update",at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;isSleeping()Z",
    shift = At.Shift.BEFORE), locals = LocalCapture.CAPTURE_FAILEXCEPTION)
    public void sleep_tight$removeOnCooldown(List<ServerPlayer> players, CallbackInfoReturnable<Boolean> cir, int i, int j,
                                 Iterator var4, ServerPlayer serverPlayer){
        if(SleepTightPlatformStuff.getPlayerSleepData(serverPlayer).isOnSleepCooldown(serverPlayer)){
            this.activePlayers--;
        }
    }
}
