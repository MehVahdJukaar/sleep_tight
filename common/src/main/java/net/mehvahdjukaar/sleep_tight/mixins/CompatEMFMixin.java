package net.mehvahdjukaar.sleep_tight.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.mehvahdjukaar.sleep_tight.common.entities.BedEntity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import traben.entity_model_features.models.animation.EMFAnimationEntityContext;
import traben.entity_model_features.utils.EMFEntity;

@Pseudo
@Mixin(EMFAnimationEntityContext.class)
public abstract class CompatEMFMixin {

    @Shadow
    public static @Nullable EMFEntity getEMFEntity() {
        return null;
    }

    @ModifyReturnValue(method = "isRiding",
            require = 0,
            at = @At("RETURN"), remap = false)
    private static boolean sleep_tight$cancelPlayerRiding(boolean original){
        if (original && getEMFEntity() instanceof LivingEntity le && le.getVehicle() instanceof BedEntity) {
            return false;
        }
        return original;
    }
}
