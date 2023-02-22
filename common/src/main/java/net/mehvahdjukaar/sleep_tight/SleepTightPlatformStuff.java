package net.mehvahdjukaar.sleep_tight;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.mehvahdjukaar.sleep_tight.common.PlayerSleepCapability;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Contract;

public class SleepTightPlatformStuff {

    @Contract
    @ExpectPlatform
    public static PlayerSleepCapability getPlayerSleepCap(Player player) {
        throw new AssertionError();
    }
}
