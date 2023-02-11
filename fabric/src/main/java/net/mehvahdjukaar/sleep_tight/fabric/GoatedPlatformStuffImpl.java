package net.mehvahdjukaar.sleep_tight.fabric;

import net.mehvahdjukaar.sleep_tight.common.BreakMemory;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

public class GoatedPlatformStuffImpl {
    public static BreakMemory getBreakMemory(ServerLevel level, BlockPos toBreakPos, BlockState toBreak) {
        return FabricRamBreakingHandler.getOrCreateBreakMemory(level, toBreakPos, toBreak);
    }
}
