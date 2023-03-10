package net.mehvahdjukaar.sleep_tight.mixins;

import net.mehvahdjukaar.sleep_tight.core.ModEvents;
import net.mehvahdjukaar.sleep_tight.client.ClientEvents;
import net.mehvahdjukaar.sleep_tight.common.entities.BedEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {

    @Shadow
    public abstract boolean isSleeping();

    @Shadow
    public abstract boolean isDeadOrDying();

    protected LivingEntityMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "setPosToBed", at = @At("HEAD"), cancellable = true)
    public void setHammockPos(BlockPos pos, CallbackInfo ci) {
        BlockState state = this.level.getBlockState(pos);
        Vec3 v = ModEvents.getSleepingPosition(this, state, pos);
        if (v != null) {
            this.setPos(v);
            ci.cancel();
        }
    }
    @Inject(method = "isSleeping", at = @At(value = "HEAD"), cancellable = true)
    public void sleepOnEntity(CallbackInfoReturnable<Boolean> cir) {
        if (this.level.isClientSide && !this.isDeadOrDying() && this.getVehicle() instanceof BedEntity && ClientEvents.cameraHack) {
            cir.setReturnValue(true);
        }
    }
}
