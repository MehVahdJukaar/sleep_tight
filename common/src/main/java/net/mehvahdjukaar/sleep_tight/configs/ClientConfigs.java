package net.mehvahdjukaar.sleep_tight.configs;

import net.mehvahdjukaar.moonlight.api.platform.configs.ConfigBuilder;
import net.mehvahdjukaar.moonlight.api.platform.configs.ConfigType;
import net.mehvahdjukaar.sleep_tight.SleepTight;
import net.mehvahdjukaar.sleep_tight.client.HammockBlockTileRenderer;
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

    static{
        ConfigBuilder builder = ConfigBuilder.create(SleepTight.MOD_ID, ConfigType.CLIENT);

        builder.push("hammock");
        HAMMOCK_ANIMATION = builder.comment("Completely turns off the animation")
                .define("animation", true);
        HAMMOCK_FREQUENCY = builder.comment("Oscillation frequency of a hammock (oscillations /sec). Exact one will match this on small angles and will increase slightly on big one like a real pendulum")
                .define("oscillation_frequency", 0.25, 0, 2);
        HAMMOCK_MAX_ANGLE = builder.comment("Maximum angle a hammock can reach")
                        .define("max_angle", 65, 0., 360);
        HAMMOCK_MIN_ANGLE = builder.comment("Minimum angle a hammock can reach")
                .define("min_angle", 5, 0., 360);
        DAMPING = builder.comment("Hammock damping factor. Slows a hammock over time")
                .define("damping", 0.2, 0., 10);
        SWING_FORCE = builder.comment("Intensity of velocity increment that is applied when controlling a hammock")
                .define("swing_force", 0.012, 0., 10);
        CAMERA_ROLL_INTENSITY = builder.comment("Camera roll intensity when swinging on a hammock. Set to 0 to turn it off entirely")
                        .define("camera_roll_intensity", 1, 0, 1f);
        builder.pop();

        builder.onChange(ClientConfigs::onChange);
        builder.buildAndRegister();
    }

    public static void init() {
    }

    private static void onChange(){
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

    public static float getK(){
        return k;
    }

    public static float getMaxAngleEnergy() {
        return maxAngleEnergy;
    }

    public static float getMinAngleEnergy() {
        return minAngleEnergy;
    }
}
