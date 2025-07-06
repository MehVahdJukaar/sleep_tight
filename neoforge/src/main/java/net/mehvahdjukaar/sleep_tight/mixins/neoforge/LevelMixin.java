package net.mehvahdjukaar.sleep_tight.mixins.neoforge;

import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.mehvahdjukaar.sleep_tight.core.ModEvents;
import net.mehvahdjukaar.sleep_tight.neoforge.ForgeBedCapability;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;

@Mixin(Level.class)
public class LevelMixin {

    @Shadow
    @Final
    private ArrayList<BlockEntity> freshBlockEntities;

    @Inject(method = "tickBlockEntities", at = @At(value = "INVOKE",
            target = "Ljava/util/ArrayList;forEach(Ljava/util/function/Consumer;)V",
    shift = At.Shift.AFTER))
    private void sleepTight$initializeDataAttachmentsWhyIsntThereAnEvent(CallbackInfo ci) {
     this.freshBlockEntities.forEach(be -> {
            if (be != null) {
                if (ModEvents.shouldHaveBedData(be)) {
                    be.getData(ForgeBedCapability.SLEEP_ATTACHMENT);
                }
            }
        });

    }
}
