package net.mehvahdjukaar.sleep_tight.mixins;

import net.mehvahdjukaar.sleep_tight.common.tiles.IExtraBedDataProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(value = BlockEntity.class, priority = 1100)
public abstract class BlockEntityMixin {

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    protected void saveAdditional(CompoundTag tag, CallbackInfo ci) {
        if (this instanceof IExtraBedDataProvider provider) {
            var data = provider.st_getBedData();
            if (!data.isEmpty())
                tag.put("sleep_tight_data", data.serializeNBT());
        }
    }

    @Inject(method = "load", at = @At("TAIL"))
    public void load(CompoundTag tag, CallbackInfo ci) {
        if (this instanceof IExtraBedDataProvider provider) {
            var data = provider.st_getBedData();
            var c = tag.getCompound("sleep_tight_data");
            data.deserializeNBT(c);
        }
    }
}
