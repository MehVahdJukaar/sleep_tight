package net.mehvahdjukaar.sleep_tight.common.entities;

import net.mehvahdjukaar.moonlight.api.block.MimicBlock;
import net.mehvahdjukaar.moonlight.api.platform.network.NetworkHelper;
import net.mehvahdjukaar.sleep_tight.SleepTight;
import net.mehvahdjukaar.sleep_tight.common.items.BedbugEggsItem;
import net.mehvahdjukaar.sleep_tight.common.network.ClientBoundParticleMessage;
import net.mehvahdjukaar.sleep_tight.common.network.ModNetworking;
import net.mehvahdjukaar.sleep_tight.configs.CommonConfigs;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Dynamic;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.ClimbOnTopOfPowderSnowGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.*;

public class BedbugEntity extends PathfinderMob {
    private static final EntityDataAccessor<Byte> DATA_FLAGS_ID = SynchedEntityData.defineId(BedbugEntity.class, EntityDataSerializers.BYTE);

    private static final ImmutableList<? extends SensorType<? extends Sensor<? super BedbugEntity>>> SENSOR_TYPES =
            ImmutableList.of(SensorType.NEAREST_LIVING_ENTITIES, SensorType.NEAREST_PLAYERS, SensorType.HURT_BY);
    private static final ImmutableList<? extends MemoryModuleType<?>> MEMORY_TYPES = ImmutableList.of(
            MemoryModuleType.HOME,
            MemoryModuleType.NEAREST_LIVING_ENTITIES,
            MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES,
            MemoryModuleType.NEAREST_VISIBLE_PLAYER,
            MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER,
            MemoryModuleType.LOOK_TARGET,
            MemoryModuleType.WALK_TARGET,
            MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE,
            MemoryModuleType.PATH,
            MemoryModuleType.ATTACK_TARGET,
            MemoryModuleType.ATTACK_COOLING_DOWN,
            MemoryModuleType.HURT_BY,
            MemoryModuleType.HURT_BY_ENTITY);

    //client
    private int burrowingTicks = 0;
    private int prevBurrowingTicks = 0;

    public BedbugEntity(EntityType<? extends BedbugEntity> entityType, Level level) {
        super(entityType, level);
    }

    public BedbugEntity(Level level) {
        super(SleepTight.BEDBUG_ENTITY.get(), level);
    }

    @Override
    protected void registerGoals() {
        // Reactive, non-navigation goals coexist fine with the brain (neither drives WALK_TARGET).
        // Everything that moves the bedbug now lives in the brain (see makeBrain).
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(1, new ClimbOnTopOfPowderSnowGoal(this, this.level()));
    }

    @Override
    protected Brain.Provider<BedbugEntity> brainProvider() {
        return Brain.provider(MEMORY_TYPES, SENSOR_TYPES);
    }

    @Override
    protected Brain<?> makeBrain(Dynamic<?> dynamic) {
        return BedbugAi.makeBrain(this.brainProvider().makeBrain(dynamic));
    }

    @Override
    @SuppressWarnings("unchecked")
    public Brain<BedbugEntity> getBrain() {
        return (Brain<BedbugEntity>) super.getBrain();
    }

    @Override
    protected void customServerAiStep() {
        this.getBrain().tick((ServerLevel) this.level(), this);
        BedbugAi.updateActivity(this);
        super.customServerAiStep();
    }

    public boolean hasBed() {
        return this.getBrain().hasMemoryValue(MemoryModuleType.HOME);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        float healthBefore = this.getHealth();
        boolean hurt = super.hurt(source, amount);
        this.setBurrowing(false);
        if (!this.level().isClientSide && hurt && !this.isAlive() && healthBefore >= this.getMaxHealth()) {
            this.setSplattered(true);
        }
        // Provoked retaliation only when there's no bed to run to; otherwise it keeps fleeing toward the bed.
        if (!this.level().isClientSide && hurt && this.isAlive() && this.level().getDifficulty() != Difficulty.PEACEFUL
                && !this.hasBed()
                && source.getEntity() instanceof LivingEntity attacker && this.canAttack(attacker)) {
            this.getBrain().eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
            this.getBrain().setMemoryWithExpiry(MemoryModuleType.ATTACK_TARGET, attacker, 200L);
        }
        return hurt;
    }

    @Override
    public LivingEntity getTarget() {
        return this.getTargetFromBrain();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_FLAGS_ID, (byte) 0);
    }

    public float getBurrowing(float partialTicks) {
        return Mth.lerp(partialTicks, prevBurrowingTicks, burrowingTicks);
    }

    @Override
    public int getMaxHeadYRot() {
        return 0;
    }

    @Override
    public int getMaxHeadXRot() {
        return 20;
    }

    @Override
    public void tick() {
        super.tick();

        Level level = this.level();
        if (!level.isClientSide) {
            this.setClimbing(this.horizontalCollision);

            // Server is authoritative over the burrow: it re-checks the bed under the bug each tick,
            // advances the timer, infests, plays the (broadcast) sounds, and clears the flag once the
            // bug is no longer on a valid bed. FLAG_BURROWING is synced, so clients drive the animation
            // straight off it (below) rather than re-deciding for themselves.
            if (this.isBurrowing()) {
                BlockPos pos = this.findBedToBurrow();
                if (pos == null) {
                    this.setBurrowing(false);
                } else {
                    burrowingTicks++;
                    if (burrowingTicks > 40) {
                        if (BedbugEggsItem.infestBed(level, pos, this)) {
                            this.spawnAnim();
                            this.discard();
                            level.playSound(null, pos, SoundEvents.WOOL_BREAK, SoundSource.HOSTILE, 1, 1);
                        } else {
                            this.setBurrowing(false);
                        }
                    } else if (burrowingTicks % 4 == 0) {
                        level.playSound(null, pos, SoundEvents.WOOL_HIT, SoundSource.HOSTILE, 0.5f, 1.2f);
                    }
                }
            } else if (burrowingTicks > 0) {
                this.burrowingTicks = Math.max(0, this.burrowingTicks - 4);
            }
        } else {
            this.prevBurrowingTicks = burrowingTicks;

            // Drive the burrow animation purely off the synced flag. Re-deriving the bed client-side is
            // fragile: the bug's interpolated position (and, briefly, the bed's OCCUPIED state) can lag
            // the server while it walks onto the bed, which would stall or flicker the animation even
            // though the server is burrowing. Particles still need a bed block, so they stay best-effort.
            if (this.isBurrowing()) {
                burrowingTicks++;
                BlockPos pos = this.findBedToBurrow();
                if (pos != null) {
                    BlockState bedState = level.getBlockState(pos);
                    for (int i = 0; i < 6 + level.random.nextInt(10); i++) {
                        float x = pos.getX() + level.random.nextFloat();
                        float z = pos.getZ() + level.random.nextFloat();
                        float y = pos.getY() + 9 / 16f;
                        level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, bedState),
                                x, y, z, 0, 0, 0);
                    }
                }
            } else if (burrowingTicks > 0) {
                this.burrowingTicks = Math.max(0, this.burrowingTicks - 4);
            }
        }

    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SleepTight.BEDBUG_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SleepTight.BEDBUG_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SleepTight.BEDBUG_DEATH.get();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.SILVERFISH_STEP, 0.15F, 1.0F);
    }

    @Override
    public boolean onClimbable() {
        return this.isClimbing();
    }

    @Override
    public void makeStuckInBlock(BlockState state, Vec3 motionMultiplier) {
        if (!state.is(Blocks.COBWEB)) {
            super.makeStuckInBlock(state, motionMultiplier);
        }
    }

    /**
     * Returns true if the WatchableObject (Byte) is 0x01 otherwise returns false. The WatchableObject is updated using setBesideClimableBlock.
     */
    // independent bit flags: climbing = bit0 (1), splattered = bit1 (2), burrowing = bit2 (4).
    // they must NOT overlap: the per-tick setClimbing(horizontalCollision) would otherwise clobber the burrow flag.
    private static final int FLAG_CLIMBING = 1;
    private static final int FLAG_SPLATTERED = 2;
    private static final int FLAG_BURROWING = 4;

    public boolean isClimbing() {
        return (this.entityData.get(DATA_FLAGS_ID) & FLAG_CLIMBING) != 0;
    }

    public boolean isSplattered() {
        return (this.entityData.get(DATA_FLAGS_ID) & FLAG_SPLATTERED) != 0;
    }

    public boolean isBurrowing() {
        return (this.entityData.get(DATA_FLAGS_ID) & FLAG_BURROWING) != 0;
    }

    private void setFlag(int flag, boolean value) {
        byte b = this.entityData.get(DATA_FLAGS_ID);
        if (value) {
            b = (byte) (b | flag);
        } else {
            b = (byte) (b & ~flag);
        }
        this.entityData.set(DATA_FLAGS_ID, b);
    }

    public void setClimbing(boolean climbing) {
        setFlag(FLAG_CLIMBING, climbing);
    }

    public void setSplattered(boolean splattered) {
        setFlag(FLAG_SPLATTERED, splattered);
    }

    public void setBurrowing(boolean burrowing) {
        setFlag(FLAG_BURROWING, burrowing);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new BedbugNavigation(this, level);
    }

    /**
     * Seeds the bedbug's initial target bed (e.g. the bed a player just slept in). Stored as the brain's
     * HOME memory; once this bed is infested or becomes invalid, AcquirePoi finds and claims a new one.
     * HOME persists across save/load via the brain, so no manual NBT is needed.
     */
    public void setBedTarget(BlockPos pos) {
        this.getBrain().setMemory(MemoryModuleType.HOME, GlobalPos.of(this.level().dimension(), pos.immutable()));
    }

    protected void onInsideBlock(BlockState state, BlockPos pos) {
        if (state.getBlock() instanceof DoorBlock) {
            //gets full shape
            VoxelShape voxelShape = state.getCollisionShape(this.level(), pos);
            VoxelShape voxelShape2 = voxelShape.move(pos.getX(), pos.getY(), pos.getZ());
            if (Shapes.joinIsNotEmpty(voxelShape2, Shapes.create(this.getBoundingBox()), BooleanOp.AND)) {

                NetworkHelper.sendToAllClientPlayersTrackingEntity(this, ClientBoundParticleMessage.bedbugDoor(pos));
                this.makeStuckInBlock(state, new Vec3(0.5, 0.5, 0.5));
            }
        }
        super.onInsideBlock(state);
    }

    @Override
    protected void checkInsideBlocks() {
        AABB aABB = this.getBoundingBox();
        BlockPos blockPos = BlockPos.containing(aABB.minX + 0.001, aABB.minY + 0.001, aABB.minZ + 0.001);
        BlockPos blockPos2 = BlockPos.containing(aABB.maxX - 0.001, aABB.maxY - 0.001, aABB.maxZ - 0.001);
        Level level = level();
        if (level.hasChunksAt(blockPos, blockPos2)) {
            BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();

            for (int i = blockPos.getX(); i <= blockPos2.getX(); ++i) {
                for (int j = blockPos.getY(); j <= blockPos2.getY(); ++j) {
                    for (int k = blockPos.getZ(); k <= blockPos2.getZ(); ++k) {
                        mutableBlockPos.set(i, j, k);
                        BlockState blockState = level.getBlockState(mutableBlockPos);

                        try {
                            blockState.entityInside(level, mutableBlockPos, this);
                            this.onInsideBlock(blockState, mutableBlockPos);
                        } catch (Exception e) {
                            CrashReport crashReport = CrashReport.forThrowable(e, "Colliding entity with block");
                            CrashReportCategory crashReportCategory = crashReport.addCategory("Block being collided with");
                            CrashReportCategory.populateBlockDetails(crashReportCategory, level, mutableBlockPos, blockState);
                            throw new ReportedException(crashReport);
                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean canCollideWith(Entity entity) {
        return super.canCollideWith(entity);
    }

    @Override
    public boolean isColliding(BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof DoorBlock) {
            return false;
        }
        return super.isColliding(pos, state);
    }

    /**
     * Finds the bed this bedbug is standing on and burrowing straight down into: the block at its feet, or
     * the block directly below (when it rests on top of the bed). Strictly vertical - a bed merely next to
     * the bug does not count, since it buries into the bed it stands on. Null clears the burrowing state.
     */
    @Nullable
    private BlockPos findBedToBurrow() {
        Level level = this.level();
        BlockPos feet = this.blockPosition();
        if (isValidBedForInfestation(level.getBlockState(feet))) return feet;
        BlockPos below = feet.below();
        if (isValidBedForInfestation(level.getBlockState(below))) return below;
        return null;
    }

    public static boolean isValidBedForInfestation(BlockState state) {
        Block block = state.getBlock();
        return block instanceof BedBlock && !state.getValue(BedBlock.OCCUPIED);
    }

    public static AttributeSupplier.Builder makeAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 9.0)
                .add(Attributes.MOVEMENT_SPEED, 0.325).add(Attributes.ATTACK_DAMAGE, 1.0)
                // pathfinding reach is bounded by follow range, so without this the bug detects beds it can
                // never path to and never claims them (gives up ~16 blocks). Kept below AcquirePoi.SCAN_RANGE
                // (48) to cap pathfinding cost - beds farther than this just won't get infested.
                .add(Attributes.FOLLOW_RANGE, 38.0);
    }

    public static boolean checkBedbugSpawnRules(EntityType<? extends BedbugEntity> type, ServerLevelAccessor level,
                                                MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        if (spawnType != MobSpawnType.EVENT && spawnType != MobSpawnType.NATURAL) {
            return false;
        }
        int maxLight = CommonConfigs.BEDBUG_MAX_LIGHT.get();
        if (maxLight < 15) {
            int light = Math.max(level.getBrightness(LightLayer.BLOCK, pos), level.getBrightness(LightLayer.SKY, pos));
            return light <= maxLight;
        }
        return true;
    }

}
