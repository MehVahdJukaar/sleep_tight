package net.mehvahdjukaar.sleep_tight.forge;


import net.mehvahdjukaar.sleep_tight.common.PlayerSleepCapability;
import net.minecraft.world.entity.player.Player;

public class SleepTightPlatformStuffImpl {


    @org.jetbrains.annotations.Contract
    public static PlayerSleepCapability getPlayerSleepCap(Player player) {
        return player.getCapability(ForgePlayerSleepCapability.TOKEN).orElse(null);
    }


}
