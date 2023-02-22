package net.mehvahdjukaar.sleep_tight.configs;

import net.mehvahdjukaar.moonlight.api.platform.configs.ConfigBuilder;
import net.mehvahdjukaar.moonlight.api.platform.configs.ConfigType;
import net.mehvahdjukaar.sleep_tight.SleepTight;

import java.util.function.Supplier;

public class CommonConfigs {

    public static final Supplier<Boolean> FIX_BED_POSITION;
    public static final Supplier<Integer> SLEEP_INTERVAL;
    public static final Supplier<Integer> NIGHTMARES_CONSECUTIVE_NIGHTS;
    public static final Supplier<Double> NIGHTMARE_CHANCE_INCREMENT_PER_NIGHT;
    public static final Supplier<Double> NIGHTMARE_SLEEP_TIME_MULTIPLIER;
    public static final Supplier<Integer> INSOMNIA_DURATION;

    static{
        ConfigBuilder builder = ConfigBuilder.create(SleepTight.MOD_ID, ConfigType.COMMON);

        builder.push("hammock");
        FIX_BED_POSITION = builder.comment("Fixes multiplayer players being positioned 2 pixels above a bed")
                .define("fix_bed_position", true);
        builder.pop();

        builder.push("bed");
        SLEEP_INTERVAL = builder.comment("Interval between two consecutive sleep times for them to not be considered consecutive")
                        .define("sleep_interval", 24000,0, 1000000);
        NIGHTMARES_CONSECUTIVE_NIGHTS = builder.comment("Amount of consecutive nights from which nightmares could start to happen")
                        .define("minimum_nights_for_nightmares", 4, 0, 100);
        NIGHTMARE_CHANCE_INCREMENT_PER_NIGHT = builder.define("nightmare_increment_per_night", 0.15, 0, 1);
        NIGHTMARE_SLEEP_TIME_MULTIPLIER = builder.comment("Multiplier applied to time slept after a nightmare")
                        .define("nightmare_sleep_time_multiplier", 0.5, 0.01, 1);
        INSOMNIA_DURATION = builder.comment("Refractory preiod after a nightmare in which you won't be able to sleep")
                        .define("insomnia_duration", 24000+12000,0, 1000000);
        builder.pop();

        builder.setSynced();
        builder.buildAndRegister();
    }

    public static void init() {
    }
}
