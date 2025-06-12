package net.mehvahdjukaar.sleep_tight.neoforge;

import net.mehvahdjukaar.moonlight.api.platform.RegHelper;
import net.mehvahdjukaar.sleep_tight.SleepTight;
import net.mehvahdjukaar.sleep_tight.core.PlayerSleepData;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

//actual capability provider (which provides itself as a cap instance)
public class ForgePlayerSleepCapability extends PlayerSleepData implements INBTSerializable<CompoundTag> {

    public static final Supplier<AttachmentType<ForgePlayerSleepCapability>> SLEEP_ATTACHMENT =
            RegHelper.register(SleepTight.res("player_sleep_data"),
                    () -> AttachmentType.serializable(ForgePlayerSleepCapability::new).build(),
                    NeoForgeRegistries.Keys.ATTACHMENT_TYPES);

    public static void init() {

    }
}

