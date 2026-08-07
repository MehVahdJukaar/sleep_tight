package net.mehvahdjukaar.sleep_tight.mixins;

import net.mehvahdjukaar.sleep_tight.core.ModEvents;
import net.mehvahdjukaar.sleep_tight.client.ClientEvents;
import net.mehvahdjukaar.sleep_tight.common.entities.BedEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
    public abstract boolean isDeadOrDying();

    protected LivingEntityMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "setPosToBed", at = @At("HEAD"), cancellable = true)
    public void sleep_tight$setHammockPos(BlockPos pos, CallbackInfo ci) {
        BlockState state = this.level().getBlockState(pos);
        Vec3 v = ModEvents.getSleepingPosition(this, state, pos);
        if (v != null) {
            this.setPos(v);
            ci.cancel();
        }
    }

    //nudge whatever the game (or another mod) landed on rather than recomputing the position from the bed pos,
    //so mods that relocate beds keep working. skipped entirely when the hook above already took over
    @Inject(method = "setPosToBed", at = @At("TAIL"))
    public void sleep_tight$offsetBedPos(BlockPos pos, CallbackInfo ci) {
        Vec3 offset = ModEvents.getSleepingPositionOffset(this, this.level().getBlockState(pos));
        if (offset != null) {
            this.setPos(this.position().add(offset));
        }
    }
    /**
     * Laying on a bed has no sleeping pos, so vanilla reports no bed orientation and skips the sleeping
     * transform entirely. UP is the "no orientation" value the renderer treats as a no-op translate, and
     * keeping it non-null is what lets our own laying transform run. Neoforge patches this in already, but
     * relying on that made the whole feature depend on a loader patch we don't control.
     */
    @Inject(method = "getBedOrientation", at = @At("HEAD"), cancellable = true)
    private void sleep_tight$bedEntityOrientation(CallbackInfoReturnable<Direction> cir) {
        if (this.getVehicle() instanceof BedEntity) {
            cir.setReturnValue(Direction.UP);
        }
    }

    @Inject(method = "isSleeping", at = @At(value = "HEAD"), cancellable = true)
    public void sleep_tight$sleepOnEntity(CallbackInfoReturnable<Boolean> cir) {
        if (this.level().isClientSide && !this.isDeadOrDying() && this.getVehicle() instanceof BedEntity && ClientEvents.cameraHack) {
            cir.setReturnValue(true);
        }
    }
}
