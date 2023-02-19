package net.mehvahdjukaar.sleep_tight.configs;

import net.mehvahdjukaar.moonlight.api.platform.configs.ConfigBuilder;
import net.mehvahdjukaar.moonlight.api.platform.configs.ConfigType;
import net.mehvahdjukaar.supplementaries.Supplementaries;

import java.util.function.Supplier;

public class ClientConfigs {

    public static final Supplier<Double> HAMMOCK_FREQUENCY;
    public static final Supplier<Double> HAMMOCK_MAX_ANGLE;
    public static final Supplier<Double> HAMMOCK_MIN_ANGLE;
    public static final Supplier<Double> DAMPING;
    public static final Supplier<Double> SWING_FORCE;

    static{
        ConfigBuilder builder = ConfigBuilder.create(Supplementaries.res("client"), ConfigType.CLIENT);

        builder.push("hammock");
        HAMMOCK_FREQUENCY = builder.comment("Oscillaton frequency of a hammock (oscillations /sec). Exact one will match this on small angles and will increase slightly on big one like a real pendulum")
                .define("oscillation_frequency", 0.25, 0, 2);
        HAMMOCK_MAX_ANGLE = builder.comment("Maximum angle a hammock can reach")
                        .define("max_angle", 45, 0., 360);
        HAMMOCK_MIN_ANGLE = builder.comment("Minimum angle a hammock can reach")
                .define("min_angle", 2, 0., 360);
        DAMPING = builder.comment("Hammock damping factor. Slows a hammock over time")
                .define("damping", 0.01, 0., 10);
        SWING_FORCE = builder.comment("Intensity of velocity increment that is applied when controlling a hammock")
                .define("swing_force", 0.01, 0., 10);
        builder.pop();
    }

    public static void init() {
    }
}
