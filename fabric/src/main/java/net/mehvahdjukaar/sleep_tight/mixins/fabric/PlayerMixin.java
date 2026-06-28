package net.mehvahdjukaar.sleep_tight.mixins.fabric;

import net.mehvahdjukaar.sleep_tight.SleepTight;
import net.mehvahdjukaar.sleep_tight.core.PlayerSleepData;
import net.mehvahdjukaar.sleep_tight.fabric.ISleepTightPlayer;
import net.mehvahdjukaar.sleep_tight.fabric.PlayerSleepDataImpl;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public class PlayerMixin implements ISleepTightPlayer {

    @Unique
    private final PlayerSleepData sleep_tight$sleepData = new PlayerSleepDataImpl();

    @Override
    public PlayerSleepData st$getSleepData() {
        return sleep_tight$sleepData;
    }

    @Inject(method = "addAdditionalSaveData", at = @At("HEAD"))
    public void sleep_tight$saveAdditional(CompoundTag compound, CallbackInfo ci) {
        PlayerSleepData.CODEC.encodeStart(NbtOps.INSTANCE, sleep_tight$sleepData)
                .resultOrPartial(SleepTight.LOGGER::error)
                .ifPresent(tag -> compound.put("sleep_tight_data", tag));
    }

    @Inject(method = "readAdditionalSaveData", at = @At("HEAD"))
    public void sleep_tight$readAdditional(CompoundTag compound, CallbackInfo ci) {
        if (compound.contains("sleep_tight_data")) {
            PlayerSleepData.CODEC.parse(NbtOps.INSTANCE, compound.get("sleep_tight_data"))
                    .resultOrPartial(SleepTight.LOGGER::error)
                    .ifPresent(sleep_tight$sleepData::copyFrom);
        }
    }

}
