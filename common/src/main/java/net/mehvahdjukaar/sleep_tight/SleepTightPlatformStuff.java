package net.mehvahdjukaar.sleep_tight;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import org.jetbrains.annotations.Contract;

public class SleepTightPlatformStuff {


    @Contract
    @ExpectPlatform
    public static void setOrForcePose(Entity p, Pose pose) {
        throw new AssertionError();
    }
}
