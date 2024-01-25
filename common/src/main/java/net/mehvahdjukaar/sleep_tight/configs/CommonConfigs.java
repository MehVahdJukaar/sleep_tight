package net.mehvahdjukaar.sleep_tight.configs;

import net.mehvahdjukaar.moonlight.api.platform.configs.ConfigBuilder;
import net.mehvahdjukaar.moonlight.api.platform.configs.ConfigSpec;
import net.mehvahdjukaar.moonlight.api.platform.configs.ConfigType;
import net.mehvahdjukaar.sleep_tight.SleepTight;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.Contract;

import java.util.List;
import java.util.function.Supplier;

import static net.mehvahdjukaar.sleep_tight.SleepTight.EASY_MODE;

public class CommonConfigs {


    public static final Supplier<Boolean> FIX_BED_POSITION;
    public static final Supplier<ExplosionBehavior> EXPLOSION_BEHAVIOR;
    public static final Supplier<Integer> SLEEP_INTERVAL;
    public static final Supplier<Boolean> DOUBLE_BED;
    public static final Supplier<Boolean> LAY_WHEN_ON_COOLDOWN;
    public static final Supplier<Boolean> NIGHT_BAG_BORING;
    public static final Supplier<Boolean> SLEEP_IMMEDIATELY;

    public static final Supplier<HeartstoneMode> HEARTSTONE_MODE;
    public static final Supplier<List<EffectData>> HEARTSTONE_EFFECT;

    public static final Supplier<Integer> HOME_BED_REQUIRED_NIGHTS;
    public static final Supplier<Integer> HOME_BED_MAX_LEVEL;
    public static final Supplier<Boolean> ONLY_RESPAWN_IN_HOME_BED;
    public static final Supplier<Double> INVIGORATED_XP;


    public static final Supplier<Boolean> NIGHTMARES_BED;
    public static final Supplier<Boolean> NIGHTMARES_HAMMOCK;
    public static final Supplier<Boolean> NIGHTMARES_NIGHT_BAG;
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

    public static final Supplier<Integer> BED_COOLDOWN;
    public static final Supplier<Integer> HAMMOCK_COOLDOWN;
    public static final Supplier<Integer> NIGHT_BAG_COOLDOWN;

    public static final Supplier<BedStatus> BED_BENEFITS;
    public static final Supplier<EffectIntensity> HEALING;
    public static final Supplier<EffectIntensity> EFFECT_CLEARING;
    public static final Supplier<PotionClearing> EFFECT_CLEARING_TYPE;
    public static final Supplier<List<EffectData>> WAKE_UP_EFFECTS;

    public static final Supplier<Boolean> REQUIREMENT_BED;
    public static final Supplier<Boolean> REQUIREMENT_HAMMOCK;
    public static final Supplier<Boolean> REQUIREMENT_NIGHT_BAG;
    public static final Supplier<Boolean> NEED_FULL_HUNGER;
    public static final Supplier<Integer> XP_COST;

    public static final Supplier<Boolean> PENALTIES_BED;
    public static final Supplier<Boolean> PENALTIES_HAMMOCK;
    public static final Supplier<Boolean> PENALTIES_NIGHT_BAG;
    public static final Supplier<HungerMode> CONSUME_HUNGER_MODE;
    public static final Supplier<Double> CONSUMED_HUNGER;

    public static final Supplier<Double> BEDBUG_SPAWN_CHANCE;
    public static final Supplier<Integer> BEDBUG_SPAWN_MAX_RANGE;
    public static final Supplier<Integer> BEDBUG_SPAWN_MIN_RANGE;
    public static final Supplier<Integer> BEDBUG_TRIES;
    public static final Supplier<Integer> BEDBUG_MAX_LIGHT;
    public static final Supplier<Boolean> PREVENTED_BY_DREAM_CATCHER;
    public static final Supplier<Boolean> ONLY_WHEN_IN_HOME_BED;

    public enum ExplosionBehavior {
        DEFAULT, TINY_EXPLOSION, ALLOWS_SLEEPING, ALLOWS_SLEEPING_NO_RESPAWN;

        public boolean canRespawn() {
            return this == ALLOWS_SLEEPING;
        }

        public boolean canExplode() {
            return this == DEFAULT || this == TINY_EXPLOSION;
        }
    }

    public enum EffectIntensity {
        NONE, TIME_BASED, MAX
    }

    public enum PotionClearing {
        ALL, BENEFICIAL, HARMFUL
    }

    public enum BedStatus {
        NONE, ALWAYS, HOME_BED
    }

    public enum HungerMode {
        CONSTANT, TIME_BASED, DIFFICULTY_BASED, TIME_DIFFICULTY_BASED
    }

    public enum HeartstoneMode {
        HEARTSTONE_PLAYER_OR_VILLAGER(true, true),
        HEARTSTONE_PLAYER(true, false),
        ANY_PLAYER_OR_VILLAGER(false, true),
        ANY_PLAYER(false, false),
        OFF(false, false);

        public final boolean needsHeartstone;
        public final boolean allowVillagers;

        HeartstoneMode(boolean needsHeartstone, boolean allowVillagers) {
            this.needsHeartstone = needsHeartstone;
            this.allowVillagers = allowVillagers;
        }
    }


    public static final ConfigSpec SPEC;

    static {
        ConfigBuilder builder = ConfigBuilder.create(SleepTight.res(EASY_MODE ? "common_ez" : "common"), ConfigType.COMMON);

        builder.push("misc");

        FIX_BED_POSITION = builder.comment("Fixes multiplayer players being positioned 2 pixels above a bed")
                .define("fix_bed_position", true);
        LAY_WHEN_ON_COOLDOWN = builder.comment("Allows laying on a bed when you are on sleeping cooldown")
                .define("lay_when_on_cooldown", true);
        DOUBLE_BED = builder.comment("Allows player to sleep in the middle of two beds")
                .define("queen_size_bed", true);
        EXPLOSION_BEHAVIOR = builder.comment("Disables damage from bed explosion when used in another dimension. Can also be used to disable explosion entirely allowing one to sleep in those dimensions")
                .define("behavior_in_dimension", ExplosionBehavior.DEFAULT);
        SLEEP_INTERVAL = builder.comment("Interval between two consecutive sleep times for them to not be considered consecutive")
                .define("sleep_interval", 24000, 0, 1000000);
        NIGHT_BAG_BORING = builder.comment("Makes night bag less unique by allowing you to place them normally on the ground")
                .define("boring_night_bags", false);

        SLEEP_IMMEDIATELY = builder.comment("Immediately attempt sleeping after laying down on a bed")
                .define("sleep_immediately", false);
        builder.push("heartstone_mod_integration");
        HEARTSTONE_MODE = builder.comment("Gives some benefit when sleeping next to somebody else. By default only works in conjunction with heartstone mod")
                .define("enabled", HeartstoneMode.HEARTSTONE_PLAYER_OR_VILLAGER);
        HEARTSTONE_EFFECT = builder.comment("Effect to give to players when they wake up")
                .defineObjectList("effects", () -> List.of(new EffectData(MobEffects.REGENERATION,
                        0, 0, 30 * 60, 20)), EffectData.CODEC);

        builder.pop();

        builder.pop();

        builder.push("bedbugs");
        BEDBUG_SPAWN_CHANCE = builder.comment("Base spawn chance every time you wake up, increases with difficulty")
                .define("spawn_chance", 0.11, 0, 1);
        BEDBUG_SPAWN_MAX_RANGE = builder.comment("max radius at which they can spawn")
                .define("max_spawn_radius", 24, 1, 64);
        BEDBUG_SPAWN_MIN_RANGE = builder.comment("min radius from which they can spawn")
                .define("min_spawn_radius", 12, 1, 64);
        BEDBUG_MAX_LIGHT = builder.comment("Max light level that a bedbug can spawn at")
                .define("max_allowed_light_level", 15, 0, 15);
        BEDBUG_TRIES = builder.comment("The game will perform x attempts to spawn a bedbug around each player when they wake up. This already takes into account the chance config." +
                        "High values will decrease failed attempts")
                .define("tries", 20, 0, 1000);
        PREVENTED_BY_DREAM_CATCHER = builder.comment("Prevents bedbugs when using dream essence")
                .define("prevented_by_dream_essence", false);
        ONLY_WHEN_IN_HOME_BED = builder.comment("Only spawns bedbugs when sleeping in your home bed")
                .define("only_when_in_home_bed", false);
        builder.pop();

        builder.push("sleep_cooldown");
        HAMMOCK_COOLDOWN = builder.comment("Time before you can sleep/rest again after you've slept in a hammock")
                .define("hammock", 6000, 0, 1000000);
        NIGHT_BAG_COOLDOWN = builder.comment("Time before you can sleep/rest again after you've successfully slept in a bed")
                .define("night_bag", 6000, 0, 1000000);
        BED_COOLDOWN = builder.comment("Time before you can sleep/rest again after you've successfully slept in a bed")
                .define("bed", 6000, 0, 1000000);
        builder.pop();


        builder.push("sleep_benefits");
        BED_BENEFITS = builder.comment("Which type of beds will apply benefits on wake up")
                .define("active_for", BedStatus.HOME_BED);
        HEALING = builder.comment("Healing applied on wake up")
                .define("healing", EffectIntensity.MAX);
        EFFECT_CLEARING = builder.comment("")
                .define("effect_clearing", EffectIntensity.MAX);
        EFFECT_CLEARING_TYPE = builder.comment("")
                .define("effect_clearing_types", PotionClearing.ALL);
        WAKE_UP_EFFECTS = builder.comment("Effects to apply when player wakes up. You can add more entries, this is a list")
                .defineObject("effects_on_wake_up", () -> List.of(
                                new EffectData(SleepTight.INVIGORATED.get(), 0, 0.075f, 2 * 60 * 20, 30 * 20)),
                        EffectData.CODEC.listOf());
        builder.pop();

        builder.push("sleep_penalties");
        PENALTIES_BED = builder.define("apply_to_beds", true);
        PENALTIES_HAMMOCK = builder.define("apply_to_hammock", true);
        PENALTIES_NIGHT_BAG = builder.define("apply_to_night_bags", true);
        CONSUME_HUNGER_MODE = builder.comment("Method to calculate hunger loss. Can be based off time slept, difficulty or constant")
                .define("consumed_hunger_mode", HungerMode.TIME_DIFFICULTY_BASED);
        CONSUMED_HUNGER = builder.comment("Base hunger decrement value. Depends on other config. Set to 0 to disable")
                .define("base_value", diff(5, 0), 0f, 20);
        builder.pop();

        builder.push("sleep_requirements");
        REQUIREMENT_BED = builder.define("apply_to_beds", true);
        REQUIREMENT_HAMMOCK = builder.define("apply_to_hammock", true);
        REQUIREMENT_NIGHT_BAG = builder.define("apply_to_night_bags", true);

        NEED_FULL_HUNGER = builder.comment("Requires player to have full hunger bar before being able to sleep")
                .define("require_full_hunger", false);
        XP_COST = builder.comment("Xp cost for sleeping. Does not affect peaceful")
                .define("xp_cost", 0, 0, 200);
        builder.pop();


        builder.push("home_bed");

        HOME_BED_REQUIRED_NIGHTS = builder.comment("Amount of nights needed to mark a bed as home bed")
                .define("required_nights", diff(8, 6), 1, 50);
        INVIGORATED_XP = builder.comment("Percentage of xp added per tier of the effect. Setting to 1 doubles the effect")
                .define("invigorated_effect_xp", 0.1, 0, 1);
        HOME_BED_MAX_LEVEL = builder.comment("Home bed level cap. Each night slept increases this number")
                .define("max_level_cap", 100, 0, 1000);
        ONLY_RESPAWN_IN_HOME_BED = builder.comment("Makes respawning only possible when using your own Home Bed. Fight back the night!")
                .define("only_respawn_in_home_bed", false);
        builder.pop();

        builder.push("nightmares");
        NIGHTMARES_BED = builder.define("apply_to_beds", true);
        NIGHTMARES_HAMMOCK = builder.define("apply_to_hammock", false);
        NIGHTMARES_NIGHT_BAG = builder.define("apply_to_night_bags", false);

        NIGHTMARES_CONSECUTIVE_NIGHTS = builder.comment("Amount of consecutive nights slept after which nightmares could start to happen")
                .define("appear_after_consecutive_nights", diff(3, 4), 0, 100);
        NIGHTMARE_CHANCE_INCREMENT_PER_NIGHT = builder.define("nightmare_increment_per_night", 0.16, 0, 1);
        NIGHTMARE_SLEEP_TIME_MULTIPLIER = builder.comment("Multiplier applied to time slept after a nightmare")
                .define("sleep_time_multiplier", diff(0.5, 1), 0.01, 1);
        NIGHTMARE_INSOMNIA_DURATION = builder.comment("Refractory period after a nightmare in which you won't be able to sleep")
                .define("insomnia_duration", 24000 + 12000, 0, 1000000);

        builder.pop();

        builder.push("wake_up_encounters");

        ENCOUNTER_TRIES = builder.comment("The game will perform x attempts to spawn a mod around each player every time they sleep." +
                        "Increases likelihood of finding one. Note that actual value will also depend on local difficulty")
                .define("tries", 25, 0, 1000);
        ENCOUNTER_MAX_COUNT = builder.comment("Max amount of mobs per encounter")
                .define("max_count", 1, 0, 20);
        ENCOUNTER_RADIUS = builder.define("spawn_radius", 10, 1, 32);
        ENCOUNTER_MIN_RADIUS = builder.define("min_radius", 2, 0, 32);
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
                        SimpleWeightedRandomList.wrappedCodecAllowingEmpty(BuiltInRegistries.ENTITY_TYPE.byNameCodec()));
        ENCOUNTER_SLEEP_TIME_MULTIPLIER = builder.comment("Multiplier applied to time slept after an encounter")
                .define("sleep_time_multiplier", 0.5, 0.01, 1);
        ENCOUNTER_INSOMNIA_DURATION = builder.comment("Refractory period after an encounter in which you won't be able to sleep")
                .define("insomnia_duration", 12000, 0, 1000000);

        builder.pop();

        builder.setSynced();
        SPEC = builder.buildAndRegister();
    }

    public static void init() {
    }

    @Contract
    private static boolean diff(boolean def, boolean easy) {
        return EASY_MODE ? easy : def;
    }

    @Contract
    private static double diff(double def, double easy) {
        return EASY_MODE ? easy : def;
    }

    @Contract
    private static int diff(int def, int easy) {
        return EASY_MODE ? easy : def;
    }

}
