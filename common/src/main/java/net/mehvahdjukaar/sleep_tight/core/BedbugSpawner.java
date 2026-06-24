package net.mehvahdjukaar.sleep_tight.core;

import net.mehvahdjukaar.sleep_tight.STPlatStuff;
import net.mehvahdjukaar.sleep_tight.SleepTight;
import net.mehvahdjukaar.sleep_tight.common.blocks.DreamEssenceBlock;
import net.mehvahdjukaar.sleep_tight.common.entities.BedbugEntity;
import net.mehvahdjukaar.sleep_tight.configs.CommonConfigs;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Phantom-style ambient spawner with inverted insomnia logic: bedbugs appear when the player
 * has been sleeping consistently, not when they've gone without rest.
 */
public class BedbugSpawner implements CustomSpawner {

    /**
     * Same constant phantoms use ({@code random.nextInt(timeSinceRest) >= 72000}).
     * Here, higher {@link PlayerSleepData#getConsecutiveNightsSlept()} lowers this threshold.
     */
    private static final int PHANTOM_REST_TICKS = 72000;

    private int nextTick;

    @Override
    public int tick(ServerLevel level, boolean spawnEnemies, boolean spawnFriendlies) {
        // Phantoms require monster spawning; we require creature/friendly spawning instead.
        if (!spawnFriendlies) {
            return 0;
        }
        if (!level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)) {
            return 0;
        }
        if (!CommonConfigs.BEDBUG_AMBIENT_SPAWNER.get()) {
            return 0;
        }

        RandomSource random = level.random;
        this.nextTick--;
        if (this.nextTick > 0) {
            return 0;
        }
        this.nextTick += (60 + random.nextInt(60)) * 20;

        int spawned = 0;
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || player.isCreative()) {
                continue;
            }
            if (!shouldSpawnFor(player, random)) {
                continue;
            }
            if (CommonConfigs.PREVENTED_BY_DREAM_ESSENCE.get()
                    && DreamEssenceBlock.isInRange(player.blockPosition(), level)) {
                continue;
            }
            if (tryAmbientSpawn(player, level)) {
                spawned++;
            }
        }
        return spawned;
    }

    private boolean shouldSpawnFor(ServerPlayer player, RandomSource random) {
        PlayerSleepData data = STPlatStuff.getPlayerSleepData(player);
        int nights = data.getConsecutiveNightsSlept();
        int minNights = CommonConfigs.BEDBUG_AMBIENT_MIN_NIGHTS.get();
        if (nights < minNights) {
            return false;
        }

        // Mirror phantom roll: more consecutive nights => easier spawn (phantoms: longer awake => easier).
        int restedTicks = Mth.clamp((nights - minNights + 1) * 24000, 1, PHANTOM_REST_TICKS);
        return random.nextInt(PHANTOM_REST_TICKS) >= PHANTOM_REST_TICKS - restedTicks;
    }

static     boolean isValidSpawnTime(ServerLevel level, BlockPos pos) {
        if (!level.dimensionType().hasSkyLight()) {
            return true;
        }
        // Phantoms need deep night (skyDarken >= 5). Bedbugs prefer the opposite: daylight...
        if (level.getSkyDarken() < 5) {
            return true;
        }
        // ...or dim indoor spaces at night (where you'd actually find them).
        int maxLight = CommonConfigs.BEDBUG_MAX_LIGHT.get();
        if (maxLight >= 15) {
            return true;
        }
        return Math.max(level.getMaxLocalRawBrightness(pos), level.getMaxLocalRawBrightness(pos.above())) <= maxLight;
    }


    static boolean tryWakeUpSpawn(BlockPos bedPos, ServerPlayer player, BedData data) {
        ServerLevel level = (ServerLevel) player.level();
        if (!level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)) return false;

        double spawnChance = CommonConfigs.BEDBUG_SPAWN_CHANCE.get();
        if (level.random.nextFloat() >= spawnChance) {
            return false;
        }

        if (CommonConfigs.PREVENTED_BY_DREAM_ESSENCE.get()
                && DreamEssenceBlock.isInRange(bedPos, level)) {
            return false;
        }
        if (CommonConfigs.ONLY_WHEN_IN_HOME_BED.get()) {
            PlayerSleepData playerData = STPlatStuff.getPlayerSleepData(player);
            if (data == null || !playerData.isBedLastSleptInto(data)) return false;
        }

        int attempts = CommonConfigs.BEDBUG_TRIES.get();
        int min = CommonConfigs.BEDBUG_SPAWN_MIN_RANGE.get();
        int max = CommonConfigs.BEDBUG_SPAWN_MAX_RANGE.get();
        int maxAttempts = (int) (attempts * (1 + level.getCurrentDifficultyAt(bedPos).getSpecialMultiplier()));

        // spawn near the bed and tell the bug which bed to infest
        return trySpawnBedbugs(level, bedPos, min, max, maxAttempts, MobSpawnType.EVENT,
                null, false, bug -> bug.setBedTarget(bedPos));
    }

    static boolean tryAmbientSpawn(ServerPlayer player, ServerLevel level) {
        BlockPos center = player.blockPosition();
        int min = CommonConfigs.BEDBUG_AMBIENT_MIN_RANGE.get();
        int max = CommonConfigs.BEDBUG_AMBIENT_MAX_RANGE.get();
        int attempts = CommonConfigs.BEDBUG_TRIES.get();

        // ambient placement: gate each attempt on time/light and snap onto the ground,
        // and measure spawn distance from the player (natural-spawn persistence rules)
        return trySpawnBedbugs(level, center, min, max, attempts, MobSpawnType.NATURAL,
                player.position(), true, null);
    }

    /**
     * Shared core for both bedbug spawn flows: repeatedly pick a random nearby position, validate it as a
     * legal spawn, require a navigable path, then spawn. The flows differ only by parameters: the
     * {@code spawnType}, whether to apply {@code ambient} placement (time/light gate + ground snap), the
     * reference position for the natural-spawn distance check ({@code distanceFrom}, or the spawn position
     * itself when null), and an optional {@code postSpawn} hook run on the spawned bug.
     */
    private static boolean trySpawnBedbugs(ServerLevel level, BlockPos center, int min, int max, int maxAttempts,
                                           MobSpawnType spawnType, @Nullable Vec3 distanceFrom, boolean ambient,
                                           @Nullable Consumer<BedbugEntity> postSpawn) {
        BlockPos.MutableBlockPos mutable = center.mutable();
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            setRandomPosInDoughnout(center, mutable, level.random, min, max);
            if (ambient) {
                if (!BedbugSpawner.isValidSpawnTime(level, mutable)) {
                    continue;
                }
                mutable.set(findGroundPos(level, mutable, center.getY()));
            }
            Vec3 reference = distanceFrom != null ? distanceFrom : Vec3.atCenterOf(mutable);
            BedbugEntity bug = SpawnHelper. createValidMobToSpawn(reference, level, mutable,
                    SleepTight.BEDBUG_ENTITY.get(), spawnType);
            if (bug != null) {
                bug.setOnGround(true);
                if (bug.getNavigation().createPath(mutable, 0) != null) {
                    if (postSpawn != null) {
                        postSpawn.accept(bug);
                    }
                    SpawnHelper.   doSpawnMob(level, bug, spawnType);
                    return true;
                }
            }
        }
        return false;
    }

    static void setRandomPosInDoughnout(BlockPos center, BlockPos.MutableBlockPos mutable, RandomSource random,
                                        int min, int max) {
        int l = random.nextInt(min, max);
        Vec3 v = new Vec3(l, 0, 0).yRot(random.nextFloat() * Mth.PI * 2)
                .xRot(random.nextFloat() * Mth.PI);

        mutable.set(center.getX() + 0.5 + v.x, center.getY() + 0.5 + v.y, center.getZ() + 0.5 + v.z);
    }


    private static BlockPos findGroundPos(ServerLevel level, BlockPos.MutableBlockPos pos, int preferredY) {
        pos.setY(preferredY);
        for (int i = 0; i < 6; i++) {
            if (!level.getBlockState(pos.below()).isAir() && level.getBlockState(pos).isAir()) {
                return pos.immutable();
            }
            pos.move(0, -1, 0);
        }
        pos.setY(preferredY);
        return pos.immutable();
    }
}
