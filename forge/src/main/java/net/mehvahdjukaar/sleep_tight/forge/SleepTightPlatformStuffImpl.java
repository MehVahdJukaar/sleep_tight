package net.mehvahdjukaar.sleep_tight.forge;


import net.mehvahdjukaar.sleep_tight.common.PlayerBedCapability;
import net.minecraft.world.entity.player.Player;

public class SleepTightPlatformStuffImpl {


    @org.jetbrains.annotations.Contract
    public static PlayerBedCapability getPlayerBedCap(Player player) {
        return player.getCapability(ForgePlayerBedCapability.TOKEN).orElse(null);
    }


}
