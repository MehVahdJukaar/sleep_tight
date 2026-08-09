package net.mehvahdjukaar.sleep_tight.core;

import net.mehvahdjukaar.sleep_tight.SleepTight;
import net.mehvahdjukaar.sleep_tight.configs.CommonConfigs;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedEntry;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public class WakeUpEncounterHelper {

    public static boolean tryPerformEncounter(ServerPlayer player, ServerLevel level, BlockPos bedPos) {

        if (!CommonConfigs.ENCOUNTERS_ENABLED.get()) return false;
        if (!level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)) return false;

        BlockPos.MutableBlockPos mutable = bedPos.mutable();
        int monsterSpawnAttempts = CommonConfigs.ENCOUNTER_TRIES.get();
        int maxCount = CommonConfigs.ENCOUNTER_MAX_COUNT.get();
        int max = CommonConfigs.ENCOUNTER_RADIUS.get();
        int min = CommonConfigs.ENCOUNTER_MIN_RADIUS.get();
        int height = CommonConfigs.ENCOUNTER_HEIGHT.get();
        int count = 0;

        StructureManager struct = level.structureManager();
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        MobCategory category = MobCategory.MONSTER;

        int maxAttempts = (int) (monsterSpawnAttempts * level.getCurrentDifficultyAt(bedPos).getEffectiveDifficulty());

        for (int attempt = 0; attempt < maxAttempts && count < maxCount; attempt++) {
            setRandomPosCyl(bedPos, mutable, level.random, min, max, height);
            var spawnData = getRandomEncounterData(level, struct, generator, category, mutable);
            if (spawnData.isEmpty()) continue;
            var entity = SpawnHelper.createValidMobToSpawn(player.position(), level, mutable, spawnData.get(), MobSpawnType.NATURAL, false);
            if (entity instanceof Mob mob) {

                //config
                if (mob.hasLineOfSight(player)) {

                    // generic encounter mobs keep finalizing as EVENT (unchanged behaviour)
                    SpawnHelper.doSpawnMob(level, mob, MobSpawnType.EVENT);

                    setupMobToTargetPlayer(player, mob);

                    count++;
                }
            }
        }
        return count != 0;
    }

    private static void setRandomPosCyl(BlockPos center, BlockPos.MutableBlockPos mutable, RandomSource random,
                                        int min, int max, int height) {
        int l = random.nextInt(min, max);
        Vec3 v = new Vec3(l, height * (random.nextDouble() - 0.5), 0).yRot(random.nextFloat() * Mth.PI * 2);

        mutable.set(center.getX() + 0.5 + v.x, center.getY() + 0.5 + v.y, center.getZ() + 0.5 + v.z);
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

    private static Optional<EntityType<?>> getRandomEncounterData(
            ServerLevel level, StructureManager structureManager,
            ChunkGenerator chunkGenerator, MobCategory category, BlockPos pos) {

        var list = CommonConfigs.ENCOUNTER_WHITELIST.get();
        if (!list.isEmpty()) {
            return list.getRandom(level.random).map(WeightedEntry.Wrapper::getData);
        }
        return WeightedRandomList.create(NaturalSpawner
                .mobsAt(level, structureManager, chunkGenerator, category, pos, level.getBiome(pos))
                .unwrap().stream().filter(e -> !e.type.is(SleepTight.WAKE_UP_BLACKLIST))
                .toList()).getRandom(level.random).map(e -> e.type);
    }


}
