package net.mehvahdjukaar.sleep_tight;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BedBlockEntity;
import org.jetbrains.annotations.Contract;

public class SleepTightPlatformStuff {


    @Contract
    @ExpectPlatform
    public static void increaseTimeSleptInBed(Player player, BedBlockEntity bed) {
        throw new AssertionError();
    }
}
