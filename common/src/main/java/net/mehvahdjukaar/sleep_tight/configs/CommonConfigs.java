package net.mehvahdjukaar.sleep_tight.configs;

import net.mehvahdjukaar.moonlight.api.platform.configs.ConfigBuilder;
import net.mehvahdjukaar.moonlight.api.platform.configs.ConfigType;
import net.mehvahdjukaar.sleep_tight.SleepTight;
import net.minecraft.util.Mth;

import java.util.function.Supplier;

public class CommonConfigs {

    public static final Supplier<Boolean> FIX_BED_POSITION;

    static{
        ConfigBuilder builder = ConfigBuilder.create(SleepTight.MOD_ID, ConfigType.COMMON);

        builder.push("hammock");
        FIX_BED_POSITION = builder.comment("Fixes multiplayer players being positioned 2 pixels above a bed")
                .define("fix_bed_position", true);
        builder.pop();

        builder.setSynced();
        builder.buildAndRegister();
    }

    public static void init() {
    }
}
