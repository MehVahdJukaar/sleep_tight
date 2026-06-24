package net.mehvahdjukaar.sleep_tight.core;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class SpawnHelper {


    static void doSpawnMob(ServerLevel level, Mob mob, MobSpawnType spawnType) {
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()), spawnType, null);
        level.addFreshEntityWithPassengers(mob);
    }

    /**
     * Modified from vanilla {@code NaturalSpawner#spawnCategoryForPosition}, which is private and only does
     * random category-based spawning during chunk ticks. We need to force a specific {@code entityType} at a
     * specific {@code pos} while still running the real spawn validation, so the inner check sequence is copied
     * here. The category {@code SpawnerData} path ({@code NaturalSpawner#isValidSpawnPostitionForType}) is
     * replaced by the equivalent per-type checks ({@link SpawnPlacements} + collision) since we know the type.
     * {@code centerPos} is the reference point for the distance/persistence check ({@code isValidPositionForMob}).
     * If vanilla changes its spawn validation, re-sync this against {@code spawnCategoryForPosition}.
     */
    @Nullable
    static <T extends Entity> T createValidMobToSpawn(Vec3 centerPos, ServerLevel level, BlockPos.MutableBlockPos pos,
                                                      EntityType<T> entityType, MobSpawnType spawnType) {
        if (level.isNaturalSpawningAllowed(pos)) {

            double d = pos.getX() + 0.5;
            double e = pos.getZ() + 0.5;
            double y = pos.getY();

            double f = centerPos.distanceToSqr(d, y, e);

            if (!SpawnPlacements.isSpawnPositionOk(entityType, level, pos) ||
                    !SpawnPlacements.checkSpawnRules(entityType, level, spawnType, pos, level.random) ||
                    !level.noCollision(entityType.getSpawnAABB(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5))) {
                return null;
            }
            Mob mob = NaturalSpawner.getMobForSpawn(level, entityType);
            if (mob == null) return null;

            mob.moveTo(d, pos.getY(), e, level.random.nextFloat() * 360.0F, 0.0F);

            if (NaturalSpawner.isValidPositionForMob(level, mob, f)) {
                return (T) mob;
            }
        }
        return null;
    }
}
