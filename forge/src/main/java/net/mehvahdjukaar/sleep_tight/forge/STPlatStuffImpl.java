package net.mehvahdjukaar.sleep_tight.forge;


import net.mehvahdjukaar.sleep_tight.core.BedData;
import net.mehvahdjukaar.sleep_tight.core.PlayerSleepData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.ForgeEventFactory;
import org.jetbrains.annotations.Contract;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public class STPlatStuffImpl {


    @Contract
    public static PlayerSleepData getPlayerSleepData(Player player) {
        return player.getCapability(ForgePlayerSleepCapability.TOKEN).orElseThrow(
                () -> new IllegalStateException("Player sleep capability was null. How? ")
        );
    }

    public static @Nullable BedData getBedDataFromThis(BlockEntity be) {
        LazyOptional<ForgeBedCapability> capability = be.getCapability(ForgeBedCapability.TOKEN);
        if (capability.isPresent()) {
            return capability.orElseThrow(() -> new IllegalStateException("Bed capability was null. How? "));
        }
        return null;
    }

    @Contract
    @Nullable
    public static Player.BedSleepingProblem invokeSleepChecksEvents(Player player, BlockPos pos) {
        Player.BedSleepingProblem ret = ForgeEventFactory.onPlayerSleepInBed(player, Optional.of(pos));
        if (ret != null) {
            return ret;
        }
        if (!player.isSleeping() && player.isAlive()) {
            Level level = player.level();
            if (!level.dimensionType().natural()) {
                return Player.BedSleepingProblem.NOT_POSSIBLE_HERE;
            }

            if (!player.isCreative()) {
                Vec3 vec3 = Vec3.atBottomCenterOf(pos);
                List<Monster> list = level.getEntitiesOfClass(Monster.class,
                        new AABB(vec3.x() - 8.0, vec3.y() - 5.0, vec3.z() - 8.0, vec3.x() + 8.0, vec3.y() + 5.0, vec3.z() + 8.0),
                        m -> m.isPreventingPlayerRest(player)
                );
                if (!list.isEmpty()) {
                    return Player.BedSleepingProblem.NOT_SAFE;
                }
            }

            if (!ForgeEventFactory.fireSleepingTimeCheck(player, Optional.of(pos))) {
                return Player.BedSleepingProblem.NOT_POSSIBLE_NOW;
            }
        }
        return null;
    }


}
