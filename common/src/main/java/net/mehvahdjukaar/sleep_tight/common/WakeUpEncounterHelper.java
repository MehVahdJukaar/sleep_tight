package net.mehvahdjukaar.sleep_tight.common;

import net.mehvahdjukaar.sleep_tight.SleepTight;
import net.mehvahdjukaar.sleep_tight.configs.CommonConfigs;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public class WakeUpEncounterHelper {

    public static boolean tryPerformEncounter(ServerPlayer player, ServerLevel level, BlockPos bedPos) {

        BlockPos.MutableBlockPos mutable = bedPos.mutable();
        int monsterSpawnAttempts = 50;
        int maxCount = 1;
        int count = 0;

        //TODO: change to use local difficulty
        int maxAttempts = monsterSpawnAttempts * (int) Math.pow(2, level.getDifficulty().getId() - 1);

        for (int attempt = 0; attempt < maxAttempts && count < maxCount; attempt++) {
            setRandomPos(bedPos, mutable, level.random);

            if (attemptSpawningMob(player, level, mutable)) {
                count++;
            }
        }
        return count != 0;
    }

    private static void setRandomPos(BlockPos center, BlockPos.MutableBlockPos mutable, RandomSource random) {
        int max = CommonConfigs.ENCOUNTER_RADIUS.get();
        int min = CommonConfigs.ENCOUNTER_MIN_RADIUS.get();
        int height = CommonConfigs.ENCOUNTER_HEIGHT.get();

        int l = random.nextInt(min, max);
        Vec3 v = new Vec3(l, height * (random.nextDouble() - 0.5), 0).yRot(random.nextFloat() * Mth.PI * 2);

        mutable.set(center.getX() + 0.5 + v.x, center.getY() + 0.5 + v.y, center.getZ() + 0.5 + v.z);
    }

    //modified from NaturalSpawner spawnCategoryForPosition
    private static boolean attemptSpawningMob(ServerPlayer player, ServerLevel level, BlockPos.MutableBlockPos pos) {

        StructureManager structureManager = level.structureManager();
        ChunkGenerator chunkGenerator = level.getChunkSource().getGenerator();
        MobCategory category = MobCategory.MONSTER;


        if (level.isNaturalSpawningAllowed(pos)) {
            var optional = getFilteredRandomSpawnData(level, structureManager, chunkGenerator, category, pos);
            if (optional.isEmpty()) return false;
            MobSpawnSettings.SpawnerData spawnerData = optional.get();

            double d = pos.getX() + 0.5;
            double e = pos.getZ() + 0.5;
            double y = pos.getY();

            double f = player.distanceToSqr(d, y, e);

            if (NaturalSpawner.isValidSpawnPostitionForType(level, category, structureManager, chunkGenerator, spawnerData, pos, f)) {
                Mob mob = NaturalSpawner.getMobForSpawn(level, spawnerData.type);
                if (mob == null){
                    return false;
                }

                mob.moveTo(d, pos.getY(), e, level.random.nextFloat() * 360.0F, 0.0F);

                //config
                if (!mob.hasLineOfSight(player)){
                    return false;
                }
                //we are not checking if it can pathfind too since range mobs dont need to

                if (NaturalSpawner.isValidPositionForMob(level, mob, f)) {
                    mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()), MobSpawnType.EVENT, null, null);
                    level.addFreshEntityWithPassengers(mob);

                    setupMobToTargetPlayer(player, mob);

                    return true;
                }
            }
        }
        return false;
    }

    private static void setupMobToTargetPlayer(ServerPlayer player, Mob mob) {
        mob.lookAt(player, 360, 45);
        mob.yHeadRot = mob.getYRot();
        mob.yHeadRotO = mob.getYRot();
        mob.getLookControl().setLookAt(player);

        player.stopSleeping();
        player.lookAt(EntityAnchorArgument.Anchor.EYES, mob, EntityAnchorArgument.Anchor.EYES);

        mob.setOnGround(true);
        Path path = mob.getNavigation().createPath(player, 0);
        if (path != null) {
            mob.setTarget(player);
        }

        mob.playAmbientSound();
    }

    private static Optional<MobSpawnSettings.SpawnerData> getFilteredRandomSpawnData(
            ServerLevel level, StructureManager structureManager,
            ChunkGenerator chunkGenerator, MobCategory category, BlockPos pos) {
        return WeightedRandomList.create(NaturalSpawner
                .mobsAt(level, structureManager, chunkGenerator, category, pos, level.getBiome(pos))
                .unwrap().stream().filter(e -> !e.type.is(SleepTight.WAKE_UP_BLACKLIST))
                .toList()).getRandom(level.random);
    }

}
