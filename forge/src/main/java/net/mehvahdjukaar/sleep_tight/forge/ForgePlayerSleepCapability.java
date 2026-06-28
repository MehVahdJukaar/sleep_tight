package net.mehvahdjukaar.sleep_tight.forge;

import net.mehvahdjukaar.sleep_tight.core.PlayerSleepData;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;

//actual capability provider (which provides itself as a cap instance)
public class ForgePlayerSleepCapability extends PlayerSleepData implements ICapabilitySerializable<CompoundTag> {

    public static final Capability<ForgePlayerSleepCapability> TOKEN = CapabilityManager.get(new CapabilityToken<>() {
    });

    private final LazyOptional<ForgePlayerSleepCapability> optional = LazyOptional.of(() -> this);

    @Nonnull
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, Direction facing) {
        return capability == TOKEN ?
                optional.cast() : LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        return (CompoundTag) CODEC.encodeStart(NbtOps.INSTANCE, this).getOrThrow(
                false, a -> new RuntimeException("Failed to serialize PlayerSleepData")
        );
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        PlayerSleepData newData = CODEC.parse(NbtOps.INSTANCE, nbt).getOrThrow(
                false, a -> new RuntimeException("Failed to deserialize PlayerSleepData")
        );
        this.copyFrom(newData);
    }
}
