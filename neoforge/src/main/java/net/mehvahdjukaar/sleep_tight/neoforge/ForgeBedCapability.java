package net.mehvahdjukaar.sleep_tight.neoforge;

import net.mehvahdjukaar.moonlight.api.platform.RegHelper;
import net.mehvahdjukaar.sleep_tight.SleepTight;
import net.mehvahdjukaar.sleep_tight.core.BedData;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.HashMap;
import java.util.function.Supplier;

//actual capability provider (which provides itself as a cap instance)
public class ForgeBedCapability extends BedData implements INBTSerializable<CompoundTag> {

    public static final Supplier<AttachmentType<ForgeBedCapability>> SLEEP_ATTACHMENT =
            RegHelper.register(SleepTight.res("bed_data"),
                    () -> AttachmentType.serializable(ForgeBedCapability::new).build(),
                    NeoForgeRegistries.Keys.ATTACHMENT_TYPES);

    public static void init() {

    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        return (CompoundTag) CODEC.encodeStart(NbtOps.INSTANCE, this).getOrThrow();
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        BedData newData = CODEC.decode(NbtOps.INSTANCE, nbt)
                .getOrThrow()
                .getFirst();
        this.id = newData.getId();
        this.bedBug = newData.getBedBug();
        this.bedLevel = new HashMap<>();
        this.bedLevel.putAll(newData.getBedLevels());
    }
}

