package net.mehvahdjukaar.sleep_tight.configs;

import net.mehvahdjukaar.moonlight.api.platform.configs.ConfigBuilder;
import net.mehvahdjukaar.moonlight.api.platform.configs.ConfigSpec;
import net.mehvahdjukaar.moonlight.api.platform.configs.ConfigType;
import net.mehvahdjukaar.sleep_tight.SleepTight;
import net.minecraft.util.Mth;

import java.util.function.Supplier;

public class ClientConfigs {

    public static final Supplier<Double> HAMMOCK_FREQUENCY;
    public static final Supplier<Double> HAMMOCK_MAX_ANGLE;
    public static final Supplier<Double> HAMMOCK_MIN_ANGLE;
    public static final Supplier<Double> DAMPING;
    public static final Supplier<Double> SWING_FORCE;
    public static final Supplier<Double> CAMERA_ROLL_INTENSITY;
    public static final Supplier<Boolean> HAMMOCK_ANIMATION;
    public static final Supplier<Boolean> HAMMOCK_FALL;

    public static final Supplier<Double> PARTICLE_ALPHA;
    public static final Supplier<Integer> PARTICLE_LIFETIME;
    public static final Supplier<Double> PARTICLE_SPAWN_FREQUENCY;


    public static final Supplier<Boolean> INSOMNIA_TIMER;
    public static final Supplier<Boolean> INSOMNIA_COOLDOWN;
    public static final Supplier<Boolean> SHOW_TIME;
    public static final Supplier<Boolean> TIME_FORMAT_24H;
    public static final ConfigSpec SPEC;

    static {
        ConfigBuilder builder = ConfigBuilder.create(SleepTight.MOD_ID, ConfigType.CLIENT);

        builder.push("hammock");
        HAMMOCK_ANIMATION = builder.comment("Completely turns off the animation")
                .define("animation", true);
        HAMMOCK_FREQUENCY = builder.comment("Oscillation frequency of a hammock (oscillations /sec). Exact one will match this on small angles and will increase slightly on big one like a real pendulum")
                .define("oscillation_frequency", 0.25, 0, 2);
        HAMMOCK_MAX_ANGLE = builder.comment("Maximum angle a hammock can reach")
                .define("max_angle", 100, 0., 360);
        HAMMOCK_MIN_ANGLE = builder.comment("Minimum angle a hammock can reach")
                .define("min_angle", 5, 0., 360);
        DAMPING = builder.comment("Hammock damping factor. Slows a hammock over time")
                .define("damping", 0.2, 0., 10);
        SWING_FORCE = builder.comment("Intensity of velocity increment that is applied when controlling a hammock")
                .define("swing_force", 0.008, 0., 10);
        CAMERA_ROLL_INTENSITY = builder.comment("Camera roll intensity when swinging on a hammock. Set to 0 to turn it off entirely")
                .define("camera_roll_intensity", 1, 0, 1f);
        HAMMOCK_FALL = builder.comment("Swinging too much on a hammock will make you fall. Disable to do a barrel roll")
                .define("hammock_fall",true);
        builder.pop();

        builder.push("dream_essence");
        PARTICLE_ALPHA = builder.comment("How subtle the effect will be essentially. Set to one for bring fancy particles")
                .define("particle_alpha", 0.1, 0, 1);
        PARTICLE_LIFETIME = builder.comment("Affects the plume height. lower to make the plume shorter")
                .define("particle_lifetime", 380, 1, 10000);
        PARTICLE_SPAWN_FREQUENCY = builder.comment("Makes particles spawn more often. Set to 0 to disable")
                .define("particle_spawn_chance", 0.15, 0, 1);
        TIME_FORMAT_24H = builder.define("24h_time_format", true);
        builder.pop();


        builder.push("misc");
        INSOMNIA_TIMER = builder.comment("Show insomnia missing time when laying on a bed")
                .define("show_insomnia_timer", false);
        INSOMNIA_COOLDOWN = builder.comment("Show insomnia cooldown as a small bed icon above crossair when aiming at a bed or in one")
                .define("crossair_insomnia_cooldown", true);
        SHOW_TIME = builder.comment("Displays current time when sleeping")
                .define("show_time_when_sleeping", true);
        builder.pop();

        builder.onChange(ClientConfigs::onChange);
        SPEC = builder.buildAndRegister();
    }

    public static void init() {
    }

    private static void onChange() {
        double frequency = ClientConfigs.HAMMOCK_FREQUENCY.get();
        k = (float) Math.pow(2 * Math.PI * frequency, 2);
        maxAngleEnergy = angleToEnergy(k, ClientConfigs.HAMMOCK_MAX_ANGLE.get());
        minAngleEnergy = angleToEnergy(k, ClientConfigs.HAMMOCK_MIN_ANGLE.get());
    }

    private static float angleToEnergy(float k, double degrees) {
        return k * (1 - Mth.cos((float) Math.toRadians(degrees)));
    }

    private static float k;
    private static float maxAngleEnergy;
    private static float minAngleEnergy;

    public static float getK() {
        return k;
    }

    public static float getMaxAngleEnergy() {
        return maxAngleEnergy;
    }

    public static float getMinAngleEnergy() {
        return minAngleEnergy;
    }
}
