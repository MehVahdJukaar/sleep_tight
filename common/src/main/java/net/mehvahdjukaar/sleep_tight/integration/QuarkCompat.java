package net.mehvahdjukaar.sleep_tight.integration;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.world.level.block.state.BlockState;

public class QuarkCompat {


    @ExpectPlatform
    public static boolean isVerticalPost(BlockState facingState) {
        throw new AssertionError();
    }
}
