package net.mehvahdjukaar.sleep_tight.configs;

import net.mehvahdjukaar.moonlight.api.platform.configs.ConfigBuilder;
import net.mehvahdjukaar.moonlight.api.platform.configs.ConfigType;
import net.mehvahdjukaar.sleep_tight.SleepTight;
import net.minecraft.core.Registry;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.world.entity.EntityType;

import java.util.function.Supplier;

public class CommonConfigs {
    public static final Supplier<Integer> SLEEP_INTERVAL;

    public static final Supplier<Boolean> FIX_BED_POSITION;
    public static final Supplier<Integer> HOME_BED_REQUIRED_NIGHTS;

    public static final Supplier<Integer> NIGHTMARES_CONSECUTIVE_NIGHTS;
    public static final Supplier<Double> NIGHTMARE_CHANCE_INCREMENT_PER_NIGHT;
    public static final Supplier<Double> NIGHTMARE_SLEEP_TIME_MULTIPLIER;
    public static final Supplier<Integer> NIGHTMARE_INSOMNIA_DURATION;

    public static final Supplier<SimpleWeightedRandomList<EntityType<?>>> ENCOUNTER_WHITELIST;
    public static final Supplier<Integer> ENCOUNTER_RADIUS;
    public static final Supplier<Integer> ENCOUNTER_MIN_RADIUS;
    public static final Supplier<Integer> ENCOUNTER_HEIGHT;
    public static final Supplier<Integer> ENCOUNTER_TRIES;
    public static final Supplier<Integer> ENCOUNTER_MAX_COUNT;
    public static final Supplier<Double> ENCOUNTER_SLEEP_TIME_MULTIPLIER;
    public static final Supplier<Integer> ENCOUNTER_INSOMNIA_DURATION;

    static {
        ConfigBuilder builder = ConfigBuilder.create(SleepTight.MOD_ID, ConfigType.COMMON);

        builder.push("hammock");
        FIX_BED_POSITION = builder.comment("Fixes multiplayer players being positioned 2 pixels above a bed")
                .define("fix_bed_position", true);
        builder.pop();

        builder.push("bed");


        builder.push("home_bed");
        SLEEP_INTERVAL = builder.comment("Interval between two consecutive sleep times for them to not be considered consecutive")
                .define("sleep_interval", 24000, 0, 1000000);
        HOME_BED_REQUIRED_NIGHTS = builder.comment("Amount of nights needed to mark a bed as home bed")
                .define("home_bed_required_nights", 8, 1, 50);
        builder.pop();

        builder.push("nightmares");
        NIGHTMARES_CONSECUTIVE_NIGHTS = builder.comment("Amount of consecutive nights from which nightmares could start to happen")
                .define("minimum_nights_cutoff", 4, 0, 100);
        NIGHTMARE_CHANCE_INCREMENT_PER_NIGHT = builder.define("nightmare_increment_per_night", 0.15, 0, 1);
        NIGHTMARE_SLEEP_TIME_MULTIPLIER = builder.comment("Multiplier applied to time slept after a nightmare")
                .define("sleep_time_multiplier", 0.5, 0.01, 1);
        NIGHTMARE_INSOMNIA_DURATION = builder.comment("Refractory period after a nightmare in which you won't be able to sleep")
                .define("insomnia_duration", 24000 + 12000, 0, 1000000);

        builder.pop();

        builder.push("wake_up_encounters");

        ENCOUNTER_TRIES = builder.comment("The game will perform x attempts to spawn a mod around each player every time they sleep." +
                        "Increases likelihood of finding one. Note that actual value will also depend on local difficulty")
                .define("tries", 50, 0, 1000);
        ENCOUNTER_MAX_COUNT = builder.comment("Max amount of mobs per encounter")
                .define("max_count", 1, 0, 20);
        ENCOUNTER_RADIUS = builder.define("spawn_radius", 10, 1, 32);
        ENCOUNTER_MIN_RADIUS = builder.define("min_radius", 1, 0, 32);
        ENCOUNTER_HEIGHT = builder.define("height", 3, 1, 10);
        ENCOUNTER_WHITELIST = builder.comment("""
                        Mobs that can randomly wake up the player if sleeping in a dark place. Leave empty to use default spawning behavior. Add a weighted list in the following format (replace line bellow):
                        [[bed.wake_up_encounters.alternative_whitelist]]
                        \t\t\tdata = "minecraft:vindicator"
                        \t\t\tweight = 2
                        [[bed.wake_up_encounters.alternative_whitelist]]
                        \t\t\tdata = "minecraft:creeper"
                        \t\t\tweight = 2
                        """)
                .defineObject("alternative_whitelist", () -> SimpleWeightedRandomList.<EntityType<?>>builder().build(),
                        SimpleWeightedRandomList.wrappedCodecAllowingEmpty(Registry.ENTITY_TYPE.byNameCodec()));
        ENCOUNTER_SLEEP_TIME_MULTIPLIER = builder.comment("Multiplier applied to time slept after an encounter")
                .define("sleep_time_multiplier", 0.5, 0.01, 1);
        ENCOUNTER_INSOMNIA_DURATION = builder.comment("Refractory period after an encounter in which you won't be able to sleep")
                .define("insomnia_duration", 12000, 0, 1000000);

        builder.pop();

        builder.pop();

        builder.setSynced();
        builder.buildAndRegister();
    }

    public static void init() {
    }
}
