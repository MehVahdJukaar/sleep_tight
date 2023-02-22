package net.mehvahdjukaar.sleep_tight.fabric;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BedBlockEntity;

public class SleepTightPlatformStuffImpl {
    public static void setOrForcePose(Entity p, Pose pose) {
        p.setPose(pose);
    }

    @org.jetbrains.annotations.Contract
    public static void increaseTimeSleptInBed(Player player, BedBlockEntity pos) {
    }

    public static void sleepingStarted(LivingEntity entity, BlockPos pos) {
    }
}
