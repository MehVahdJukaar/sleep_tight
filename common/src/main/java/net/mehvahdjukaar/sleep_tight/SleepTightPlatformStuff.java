package net.mehvahdjukaar.sleep_tight;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.mehvahdjukaar.sleep_tight.common.PlayerBedCapability;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Contract;

public class SleepTightPlatformStuff {

    @Contract
    @ExpectPlatform
    public static PlayerBedCapability getPlayerBedCap(Player player) {
        throw new AssertionError();
    }
}
