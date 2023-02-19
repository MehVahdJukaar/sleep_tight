package net.mehvahdjukaar.sleep_tight.forge;


import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;

public class SleepTightPlatformStuffImpl {

    public static void setOrForcePose(Entity p, Pose pose) {
        if(p instanceof Player pl)pl.setForcedPose(pose);
        else p.setPose(pose);
    }
}
