package net.mehvahdjukaar.sleep_tight.core;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class SpawnHelper {


    static void doSpawnMob(ServerLevel level, Mob mob, MobSpawnType spawnType) {
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()), spawnType, null, null);
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
                                                      EntityType<T> entityType, MobSpawnType spawnType,
                                                      boolean naturalDistanceRules) {
        if (!level.isNaturalSpawningAllowed(pos)) return null;

        double d = pos.getX() + 0.5;
        double e = pos.getZ() + 0.5;
        double y = pos.getY();

        double f;
        if (naturalDistanceRules) {
            // Replicate vanilla NaturalSpawner#isRightDistanceToPlayerAndSpawnPoint: never spawn within
            // MIN_SPAWN_DISTANCE (24) blocks of the nearest non-creative/spectator player, nor within 24 of
            // the world spawn. This is the vanilla rule that keeps natural mobs from popping next to a player.
            Player nearest = level.getNearestPlayer(d, y, e, -1.0, false);
            if (nearest == null) return null;
            f = nearest.distanceToSqr(d, y, e);
            if (f <= 576.0 || level.getSharedSpawnPos().closerToCenterThan(new Vec3(d, y, e), 24.0)) {
                return null;
            }
        } else {
            f = centerPos.distanceToSqr(d, y, e);
        }

        if (!NaturalSpawner.isSpawnPositionOk(SpawnPlacements.getPlacementType(entityType), level, pos, entityType) ||
                !SpawnPlacements.checkSpawnRules(entityType, level, spawnType, pos, level.random) ||
                !level.noCollision(entityType.getAABB(d, y, e))) {
            return null;
        }
        Mob mob = NaturalSpawner.getMobForSpawn(level, entityType);
        if (mob == null) return null;

        mob.moveTo(d, y, e, level.random.nextFloat() * 360.0F, 0.0F);

        if (NaturalSpawner.isValidPositionForMob(level, mob, f)) {
            return (T) mob;
        }
        return null;
    }
}
