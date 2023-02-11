package net.mehvahdjukaar.sleep_tight.mixins;

import net.mehvahdjukaar.sleep_tight.common.HammockBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {

    protected LivingEntityMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Shadow
    public abstract Optional<BlockPos> getSleepingPos();

    @Inject(method = "checkBedExists", at = @At("RETURN"), cancellable = true)
    public void checkForHammock(CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) {
            var pos = this.getSleepingPos();
            if (pos.isPresent() && this.level.getBlockState(pos.get()).getBlock() instanceof HammockBlock) {
                cir.setReturnValue(true);
            }
        }
    }
}
