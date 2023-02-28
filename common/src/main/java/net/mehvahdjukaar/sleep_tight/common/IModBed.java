package net.mehvahdjukaar.sleep_tight.common;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public interface IModBed {

    Vec3 getSleepingPosition(BlockState state, BlockPos pos);

   default void onLeftBed(BlockState state, BlockPos pos, Player entity){}

    boolean canSetSpawn();

    boolean canHaveNightmares();

    long getCooldown();
}
