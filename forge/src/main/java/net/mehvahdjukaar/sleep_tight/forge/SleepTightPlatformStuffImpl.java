package net.mehvahdjukaar.sleep_tight.forge;


import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BedBlockEntity;

public class SleepTightPlatformStuffImpl {

    public static void setOrForcePose(Entity p, Pose pose) {
        if(p instanceof Player pl)pl.setForcedPose(pose);
        else p.setPose(pose);
    }

    @org.jetbrains.annotations.Contract
    public static void increaseTimeSleptInBed(Player player, BedBlockEntity bed) {
        ModBedCapability bedCap = bed.getCapability(ModBedCapability.TOKEN).orElse(null);
        PlayerBedCapability playerCap = player.getCapability(PlayerBedCapability.TOKEN).orElse(null);
        playerCap.assignHomeBed(player, bedCap.getId());
        bedCap.increaseTimeSlept(player);
    }


}
