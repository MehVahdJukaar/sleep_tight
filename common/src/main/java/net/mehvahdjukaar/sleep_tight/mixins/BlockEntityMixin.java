package net.mehvahdjukaar.sleep_tight.mixins;

import net.mehvahdjukaar.sleep_tight.common.tiles.IExtraBedDataProvider;
import net.mehvahdjukaar.sleep_tight.core.BedData;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


//better compat if here. or something. idk actually
//yes we could have used data attachments on forge
@Mixin(value = BlockEntity.class, priority = 1100)
public abstract class BlockEntityMixin {

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
        if (this instanceof IExtraBedDataProvider provider) {
            var data = provider.st_getBedData();
            var nbt = BedData.CODEC.encodeStart(NbtOps.INSTANCE, data);
            if (nbt.result().isPresent()) {
                tag.put("sleep_tight_data", nbt.result().get());
            }
        }
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    public void load(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
        if (this instanceof IExtraBedDataProvider provider) {
            var nbt = tag.get("sleep_tight_data");
            if (nbt!= null) {
                var data = BedData.CODEC.parse(NbtOps.INSTANCE, nbt);
                if (data.result().isPresent()) {
                    provider.st_setBedData(data.result().get());
                }
            }
        }
    }
}
