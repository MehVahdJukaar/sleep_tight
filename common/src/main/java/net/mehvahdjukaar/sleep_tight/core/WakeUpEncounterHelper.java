package net.mehvahdjukaar.sleep_tight.core;

import net.mehvahdjukaar.sleep_tight.SleepTight;
import net.mehvahdjukaar.sleep_tight.common.blocks.DreamEssenceBlock;
import net.mehvahdjukaar.sleep_tight.common.entities.BedEntity;
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
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class WakeUpEncounterHelper {

    public static boolean tryPerformEncounter(ServerPlayer player, ServerLevel level, BlockPos bedPos) {

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
            var entity = createValidMobToSpawn(player.position(), level, mutable, spawnData.get(), MobSpawnType.NATURAL);
            if (entity instanceof Mob mob) {

                //config
                if (mob.hasLineOfSight(player)) {

                    doSpawnMob(level, mob);

                    setupMobToTargetPlayer(player, mob);

                    count++;
                }
            }
        }
        return count != 0;
    }

    private static void doSpawnMob(ServerLevel level, Mob mob) {
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()),
                MobSpawnType.EVENT, null, null);
        level.addFreshEntityWithPassengers(mob);
    }

    private static void setRandomPosCircle(BlockPos center, BlockPos.MutableBlockPos mutable, RandomSource random,
                                           int min, int max) {
        int l = random.nextInt(min, max);
        Vec3 v = new Vec3(l, 0, 0).yRot(random.nextFloat() * Mth.PI * 2)
                .xRot(random.nextFloat() * Mth.PI);

        mutable.set(center.getX() + 0.5 + v.x, center.getY() + 0.5 + v.y, center.getZ() + 0.5 + v.z);
    }

    private static void setRandomPosCyl(BlockPos center, BlockPos.MutableBlockPos mutable, RandomSource random,
                                        int min, int max, int height) {
        int l = random.nextInt(min, max);
        Vec3 v = new Vec3(l, height * (random.nextDouble() - 0.5), 0).yRot(random.nextFloat() * Mth.PI * 2);

        mutable.set(center.getX() + 0.5 + v.x, center.getY() + 0.5 + v.y, center.getZ() + 0.5 + v.z);
    }

    //modified from NaturalSpawner spawnCategoryForPosition. basically just checks positon and spawning constraints
    @Nullable
    private static <T extends Entity> T createValidMobToSpawn(Vec3 centerPos, ServerLevel level, BlockPos.MutableBlockPos pos,
                                                              EntityType<T> entityType, MobSpawnType spawnType) {
        if (level.isNaturalSpawningAllowed(pos)) {

            double d = pos.getX() + 0.5;
            double e = pos.getZ() + 0.5;
            double y = pos.getY();

            double f = centerPos.distanceToSqr(d, y, e);

            //calling instead of isValidSpawnPositionForType as we already checked the spawn data validity
            SpawnPlacements.Type type = SpawnPlacements.getPlacementType(entityType);
            if (!NaturalSpawner.isSpawnPositionOk(type, level, pos, entityType) ||
                    !SpawnPlacements.checkSpawnRules(entityType, level, spawnType, pos, level.random) ||
                    !level.noCollision(entityType.getAABB(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5))) {
                return null;
            }
            Mob mob = NaturalSpawner.getMobForSpawn(level, entityType);
            if (mob == null) return null;

            mob.moveTo(d, pos.getY(), e, level.random.nextFloat() * 360.0F, 0.0F);

            //we are not checking if it can pathfind too since range mobs dont need to

            if (NaturalSpawner.isValidPositionForMob(level, mob, f)) {
                return (T) mob;
            }
        }
        return null;
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


    public static boolean trySpawningBedbug(BlockPos bedPos, ServerPlayer player, BedData data) {
        ServerLevel level = (ServerLevel) player.level;
        if (!level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)) return false;

        double spawnChance = CommonConfigs.BEDBUG_SPAWN_CHANCE.get();
        if (level.random.nextFloat() < spawnChance) {

            if (CommonConfigs.PREVENTED_BY_DREAM_CATCHER.get()) {
                if (DreamEssenceBlock.isInRange(bedPos, level)) return false;
            }
            if (CommonConfigs.ONLY_WHEN_IN_HOME_BED.get()) {
                if (data == null || !data.isHomeBedFor(player)) return false;
            }

            BlockPos.MutableBlockPos mutable = bedPos.mutable();
            int monsterSpawnAttempts = CommonConfigs.BEDBUG_TRIES.get();
            int min = CommonConfigs.BEDBUG_SPAWN_MIN_RANGE.get();
            int max = CommonConfigs.BEDBUG_SPAWN_MAX_RANGE.get();

            int maxAttempts = (int) (monsterSpawnAttempts * (1 + level.getCurrentDifficultyAt(bedPos).getSpecialMultiplier()));

            for (int attempt = 0; attempt < maxAttempts; attempt++) {
                setRandomPosCircle(bedPos, mutable, level.random, min, max);

                var mob = createValidMobToSpawn(Vec3.atCenterOf(mutable), level, mutable,
                        SleepTight.BEDBUG_ENTITY.get(), MobSpawnType.EVENT);
                if (mob != null) {
                    mob.setOnGround(true);
                    Path path = mob.getNavigation().createPath(mutable, 0);
                    if (path != null) {

                        mob.setBedTarget(mutable);

                        doSpawnMob(level, mob);

                        return true;
                    }
                }
            }
        }
        return false;
    }


}
