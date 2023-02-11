package net.mehvahdjukaar.sleep_tight.fabric;

import net.mehvahdjukaar.sleep_tight.common.BreakMemory;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class FabricRamBreakingHandler {

    private static final int MAX_TIME = 20 * 10;


    private static final Map<ServerLevel, Map<BlockPos, BreakMemory>> breakProgress = new HashMap<>();

    public static void tick(MinecraftServer minecraftServer) {
        minecraftServer.getAllLevels().forEach(FabricRamBreakingHandler::validateAll);
    }

    public static void validateAll(ServerLevel serverLevel) {
        var sm = getBreakMap(serverLevel);
        if (!sm.isEmpty()) {
            var values = new ArrayList<>(sm.keySet());
            for (BlockPos pos : values) {
                var m = sm.get(pos);
                if (m == null) {
                    continue;
                }
                if (serverLevel.getBlockState(pos) != m.getState() || serverLevel.getGameTime() - m.getTimestamp() > MAX_TIME) {
                    serverLevel.destroyBlockProgress(m.getBreakerId(), pos, -1);
                    sm.remove(pos);
                }
            }
        }
    }

    @NotNull
    private static Map<BlockPos, BreakMemory> getBreakMap(ServerLevel serverLevel) {
        return breakProgress.computeIfAbsent(serverLevel, s -> new HashMap<>());
    }

    public static BreakMemory getOrCreateBreakMemory(ServerLevel level, BlockPos pos, BlockState state) {
        var bm = getBreakMap(level);
        var memory = bm.get(pos);
        if (memory == null || memory.getState() != state) {
            memory = new BreakMemory(state, pos);
            bm.put(pos, memory);
        }
        return memory;
    }

}