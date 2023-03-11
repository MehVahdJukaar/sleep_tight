package net.mehvahdjukaar.sleep_tight.mixins.fabric;

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

    @Unique
    private long oldTime;

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "net/minecraft/server/level/ServerLevel.setDayTime(J)V",
            shift = At.Shift.BEFORE))
    private void captureDayTime(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        oldTime = ((ServerLevel) (Object) this).getDayTime();
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "net/minecraft/server/level/ServerLevel.setDayTime(J)V",
            shift = At.Shift.AFTER))
    private void modifyWakeTime(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        ServerLevel level = (ServerLevel) (Object) this;
        long newTime = ModEvents.getWakeUpTimeWhenSlept(level, oldTime);
        if (newTime != oldTime) {
            level.setDayTime(newTime);
        }
    }
}