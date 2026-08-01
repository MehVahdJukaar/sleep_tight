package net.mehvahdjukaar.sleep_tight.configs;

import net.mehvahdjukaar.moonlight.api.client.anim.PendulumAnimation;
import net.mehvahdjukaar.moonlight.api.platform.configs.ConfigBuilder;
import net.mehvahdjukaar.moonlight.api.platform.configs.ConfigType;
import net.mehvahdjukaar.moonlight.api.platform.configs.ModConfigHolder;
import net.mehvahdjukaar.sleep_tight.SleepTight;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;

import java.util.function.Supplier;

public class ClientConfigs {

    public enum SleepTimeDisplay {
        OFF,
        ALWAYS,
        WITH_CLOCK;

        public boolean shouldShow(Player player) {
            return switch (this) {
                case OFF -> false;
                case ALWAYS -> true;
                case WITH_CLOCK -> player.getInventory().contains(SleepTight.CLOCKS);
            };
        }
    }

    public static final Supplier<Double> SWING_FORCE;
    public static final Supplier<Double> CAMERA_ROLL_INTENSITY;
    public static final Supplier<Boolean> HAMMOCK_ANIMATION;
    public static final Supplier<PendulumAnimation.Config> HAMMOCK_ANIMATION_PARAM;
    public static final Supplier<Boolean> HAMMOCK_FALL;
    public static final Supplier<Boolean> VILLAGER_SLEEP;

    public static final Supplier<Double> PARTICLE_ALPHA;
    public static final Supplier<Integer> PARTICLE_LIFETIME;
    public static final Supplier<Double> PARTICLE_SPAWN_FREQUENCY;
    public static final Supplier<Boolean> ZZZ_PARTICLES;


    // the bird pathfinder test harness overlay, see net.mehvahdjukaar.sleep_tight.test.debug
    public static final Supplier<Boolean> PATH_DEBUG;
    public static final Supplier<Boolean> PATH_DEBUG_NODE_LABELS;
    public static final Supplier<Boolean> PATH_DEBUG_EDGE_COSTS;
    public static final Supplier<Boolean> PATH_DEBUG_SPEED_ARROWS;
    public static final Supplier<Boolean> PATH_DEBUG_MOB_VECTORS;
    public static final Supplier<Boolean> PATH_DEBUG_MOB_STATUS;
    public static final Supplier<Boolean> PATH_DEBUG_TRAIL;
    public static final Supplier<Boolean> PATH_DEBUG_SEARCHED_CELLS;
    public static final Supplier<Boolean> PATH_DEBUG_CONSIDERED_NODES;
    public static final Supplier<Double> PATH_DEBUG_TEXT_SCALE;
    public static final Supplier<Double> PATH_DEBUG_NODE_BOX_SCALE;
    public static final Supplier<Double> PATH_DEBUG_SPEED_ARROW_SCALE;
    public static final Supplier<Double> PATH_DEBUG_RENDER_DISTANCE;

    public static final Supplier<Boolean> INSOMNIA_TIMER;
    public static final Supplier<Boolean> INSOMNIA_COOLDOWN;
    public static final Supplier<SleepTimeDisplay> SHOW_TIME;
    public static final Supplier<Boolean> TIME_FORMAT_24H;
    public static final ModConfigHolder SPEC;

    static {
        ConfigBuilder builder = ConfigBuilder.create(SleepTight.MOD_ID, ConfigType.CLIENT);

        builder.icon("hammock_red").push("hammock");
        HAMMOCK_ANIMATION = builder.comment("Completely turns off the animation")
                .define("animation", true);
        HAMMOCK_ANIMATION_PARAM = builder.comment("Swing animation parameters")
                .defineObject("animation_parameters", ()->
                                new PendulumAnimation.Config(5, 100, 0.2f, 0.25f, true, 1, 1),
                        PendulumAnimation.Config.CODEC);
        SWING_FORCE = builder.comment("Intensity of velocity increment that is applied when controlling a hammock")
                .define("swing_force", 0.008, 0., 10);
        CAMERA_ROLL_INTENSITY = builder.comment("Camera roll intensity when swinging on a hammock. Set to 0 to turn it off entirely")
                .define("camera_roll_intensity", 1, 0, 1d);
        HAMMOCK_FALL = builder.comment("Swinging too much on a hammock will make you fall. Disable to do a barrel roll")
                .define("hammock_fall",true);
        builder.pop();

        builder.icon("dreamer_essence").push("dream_essence");
        PARTICLE_ALPHA = builder.comment("How subtle the effect will be essentially. Set to one for bring fancy particles")
                .define("particle_alpha", 0.1, 0, 1);
        PARTICLE_LIFETIME = builder.comment("Affects the plume height. lower to make the plume shorter")
                .define("particle_lifetime", 380, 1, 10000);
        PARTICLE_SPAWN_FREQUENCY = builder.comment("Makes particles spawn more often. Set to 0 to disable")
                .define("particle_spawn_chance", 0.15, 0, 1);
        builder.pop();


        builder.push("path_debug");
        PATH_DEBUG = builder.comment("Master switch for the bird pathfinding overlay. Everything below only matters while this is on")
                .define("enabled", true);
        PATH_DEBUG_NODE_LABELS = builder.comment("Path type, cost malus and wall hug charge written on each node of the path")
                .define("node_labels", true);
        PATH_DEBUG_EDGE_COSTS = builder.comment("What each step of the path cost the search, written on the step, plus the route total")
                .define("edge_costs", true);
        PATH_DEBUG_SPEED_ARROWS = builder.comment("An arrow per node as long as the speed the throttle profile allows there, plus the profile summary")
                .define("speed_arrows", true);
        PATH_DEBUG_MOB_VECTORS = builder.comment("Arrows from the mob for where it is pointing and where it is actually going")
                .define("mob_vectors", true);
        PATH_DEBUG_MOB_STATUS = builder.comment("The block of live navigation and steering text following the mob")
                .define("mob_status", true);
        PATH_DEBUG_TRAIL = builder.comment("The line the mob actually flew, sampled every tick")
                .define("flown_trail", true);
        PATH_DEBUG_SEARCHED_CELLS = builder.comment("Tiles under every cell the search touched. Needs BirdPathfindingConfig#collectDebugData on the server")
                .define("searched_cells", true);
        PATH_DEBUG_CONSIDERED_NODES = builder.comment("Squares on the moves that were offered at each path node but not taken, colored by what they would have cost")
                .define("considered_nodes", true);
        PATH_DEBUG_TEXT_SCALE = builder.comment("Size of every label in the overlay. Turn down when it gets crowded")
                .define("text_scale", 0.007, 0.001, 0.05);
        PATH_DEBUG_NODE_BOX_SCALE = builder.comment("Size of the node boxes, as a fraction of the mob's width")
                .define("node_box_scale", 0.45, 0.05, 2.0);
        PATH_DEBUG_SPEED_ARROW_SCALE = builder.comment("Blocks of arrow per block per tick of speed")
                .define("speed_arrow_scale", 7.0, 0.5, 50.0);
        PATH_DEBUG_RENDER_DISTANCE = builder.comment("How far from the camera the overlay is drawn, in blocks")
                .define("render_distance", 80.0, 8.0, 256.0);
        builder.pop();

        builder.push("misc");
        INSOMNIA_TIMER = builder.comment("Show insomnia missing time when laying on a bed")
                .define("show_insomnia_timer", false);
        INSOMNIA_COOLDOWN = builder.comment("Show insomnia cooldown as a small bed icon above crossair when aiming at a bed or in one")
                .define("crossair_insomnia_cooldown", true);
        SHOW_TIME = builder.comment("Displays current time when sleeping. WITH_CLOCK only shows it while a clock is in the inventory")
                .define("show_time_when_sleeping", SleepTimeDisplay.ALWAYS);
        TIME_FORMAT_24H = builder.comment("Shows the sleep time in 24h format instead of AM/PM")
                .define("24h_time_format", true);
        VILLAGER_SLEEP = builder.comment("Makes villagers close their eyes when sleeping")
                        .define("sleeping_villagers_eyes", true);
        ZZZ_PARTICLES = builder.comment("Spawn particles when sleeping. Set to 0 to disable")
                .define("zzz_particles", true);
        builder.pop();

        SPEC = builder.build();
        SPEC.forceLoad();
    }

    public static void init() {
    }

}
