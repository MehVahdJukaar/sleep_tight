package net.mehvahdjukaar.sleep_tight.forge;

import net.mehvahdjukaar.sleep_tight.core.BedData;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;

//capability provider that holds a BedData instance (BedData has a private ctor so we compose instead of extend)
public class ForgeBedCapability implements ICapabilitySerializable<CompoundTag> {

    public static final Capability<BedData> TOKEN = CapabilityManager.get(new CapabilityToken<>() {
    });

    private BedData data = BedData.initializeWithRandomId();
    private final LazyOptional<BedData> optional = LazyOptional.of(() -> this.data);

    @Nonnull
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, Direction facing) {
        return capability == TOKEN ?
                optional.cast() : LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        return (CompoundTag) BedData.CODEC.encodeStart(NbtOps.INSTANCE, this.data).getOrThrow(
                false, a -> new RuntimeException("Failed to serialize BedData")
        );
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        this.data = BedData.CODEC.parse(NbtOps.INSTANCE, nbt).getOrThrow(
                false, a -> new RuntimeException("Failed to deserialize BedData")
        );
    }
}
