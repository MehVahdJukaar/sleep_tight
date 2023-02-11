package net.mehvahdjukaar.sleep_tight.common;

import com.mojang.datafixers.util.Either;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class SleepHelper {

    public static Either<Player.BedSleepingProblem, Unit> startSleepInBed(Player player, BlockPos pos, Direction dir, boolean offset) {
        var r = player.startSleepInBed(pos);
        if (r.right().isPresent()) {
            Vec3 v = Vec3.atCenterOf(pos);
            if(offset)v=v.relative(dir, 0.5);
            player.setPos(v.x, v.y, v.z);
        }
        return r;
    }
}
