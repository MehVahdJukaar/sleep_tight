package net.mehvahdjukaar.sleep_tight.mixins.fabric;

import net.mehvahdjukaar.sleep_tight.core.PlayerSleepData;
import net.mehvahdjukaar.sleep_tight.fabric.ISleepTightPlayer;
import net.mehvahdjukaar.sleep_tight.fabric.PlayerSleepDataImpl;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public class PlayerMixin implements ISleepTightPlayer {

    @Unique
    private final PlayerSleepData sleepData = new PlayerSleepDataImpl();

    @Override
    public PlayerSleepData getSleepData() {
        return sleepData;
    }

    @Inject(method = "addAdditionalSaveData", at = @At("HEAD"))
    public void saveAdditional(CompoundTag compound, CallbackInfo ci){
        compound.put("sleep_tight_data", sleepData.serializeNBT());
    }

    @Inject(method = "readAdditionalSaveData", at = @At("HEAD"))
    public void readAdditional(CompoundTag compound, CallbackInfo ci){
        sleepData.deserializeNBT(compound.getCompound("sleep_tight_data"));
    }
}
