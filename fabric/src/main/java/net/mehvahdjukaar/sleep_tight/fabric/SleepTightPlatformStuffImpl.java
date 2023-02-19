package net.mehvahdjukaar.sleep_tight.fabric;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;

public class SleepTightPlatformStuffImpl {
    public static void setOrForcePose(Entity p, Pose pose) {
        p.setPose(pose);
    }
}
