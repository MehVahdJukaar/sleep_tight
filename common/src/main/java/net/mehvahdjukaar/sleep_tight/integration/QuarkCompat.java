package net.mehvahdjukaar.sleep_tight.integration;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Contract;

public class QuarkCompat {


    @Contract
    @ExpectPlatform
    public static boolean isVerticalPost(BlockState facingState) {
        throw new AssertionError();
    }
}
