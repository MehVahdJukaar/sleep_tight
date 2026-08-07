package net.mehvahdjukaar.sleep_tight.mixins.fabric;

import net.mehvahdjukaar.sleep_tight.SleepTightClient;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
    protected LivingEntityMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    //getBedOrientation moved to the common LivingEntityMixin, neoforge needs it too

    @Inject(method = "aiStep", at = @At("HEAD"))
    private void sleepTight$onEntityTick(CallbackInfo ci) {
        if ( this.level().isClientSide()) {
            SleepTightClient.onEntityTick((LivingEntity) (Object) this);
        }
    }
}
