package net.mehvahdjukaar.sleep_tight.mixins.fabric;

import net.mehvahdjukaar.sleep_tight.common.entities.BedEntity;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
    protected LivingEntityMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(
            method = {"getBedOrientation"},
            at = {@At("HEAD")},
            cancellable = true
    )
    private void getBedDirection(CallbackInfoReturnable<Direction> cir) {
        if (this.getVehicle() instanceof BedEntity) cir.setReturnValue(Direction.UP);
    }
}
