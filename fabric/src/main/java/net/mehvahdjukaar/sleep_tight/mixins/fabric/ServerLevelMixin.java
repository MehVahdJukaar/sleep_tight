package net.mehvahdjukaar.sleep_tight.mixins.fabric;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.mehvahdjukaar.sleep_tight.core.ModEvents;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(ServerLevel.class)
public class ServerLevelMixin {

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "net/minecraft/server/level/ServerLevel.setDayTime(J)V",
            shift = At.Shift.BEFORE))
    private void sleep_tight$captureDayTime(BooleanSupplier hasTimeLeft, CallbackInfo ci,
                                            @Share("oldTime") LocalRef<Long> oldTime) {
        oldTime.set(((ServerLevel) (Object) this).getDayTime());
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "net/minecraft/server/level/ServerLevel.setDayTime(J)V",
            shift = At.Shift.AFTER))
    private void sleep_tight$modifyWakeTime(BooleanSupplier hasTimeLeft, CallbackInfo ci,
                                            @Share("oldTime") LocalRef<Long> oldTime) {
        ServerLevel level = (ServerLevel) (Object) this;
        long newTime = ModEvents.getWakeUpTimeWhenSlept(level, oldTime.get());
        if (!oldTime.get().equals(newTime)) {
            level.setDayTime(newTime);
        }
    }
}