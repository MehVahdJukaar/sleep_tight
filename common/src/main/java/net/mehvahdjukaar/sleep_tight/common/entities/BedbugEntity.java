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
import net.minecraft.core.Direction;
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
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfindingContext;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.*;

public class BedbugEntity extends Monster {
    private static final EntityDataAccessor<Byte> DATA_FLAGS_ID = SynchedEntityData.defineId(BedbugEntity.class, EntityDataSerializers.BYTE);

    private static final ImmutableList<? extends SensorType<? extends Sensor<? super BedbugEntity>>> SENSOR_TYPES =
            ImmutableList.of(SensorType.NEAREST_LIVING_ENTITIES, SensorType.NEAREST_PLAYERS);
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
            MemoryModuleType.ATTACK_COOLING_DOWN);

    //client
    private int burrowingTicks = 0;
    private int prevBurrowingTicks = 0;

    public BedbugEntity(EntityType<? extends Monster> entityType, Level level) {
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
        boolean hurt = super.hurt(source, amount);
        // Provoked retaliation only when there's no bed to run to; otherwise it keeps fleeing toward the bed.
        if (!this.level().isClientSide && hurt && !this.hasBed()
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
        } else {
            this.prevBurrowingTicks = burrowingTicks;
        }

        if (this.isBurrowing()) {
            BlockPos pos = this.blockPosition();

            BlockState feetBlockState = level.getBlockState(pos);
            if (!(feetBlockState.getBlock() instanceof BedBlock)) {
                this.setBurrowing(false);
            } else {
                burrowingTicks++;
                if (level.isClientSide) {
                    for (int i = 0; i < 6 + level.random.nextInt(10); i++) {
                        float x = pos.getX() + level.random.nextFloat();
                        float z = pos.getZ() + level.random.nextFloat();
                        float y = pos.getY() + 9 / 16f;
                        level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, feetBlockState),
                                x, y, z, 0, 0, 0);
                    }
                } else {
                    if (burrowingTicks > 40) {
                        if (BedbugEggsItem.infestBed(level, pos, this)) {
                            this.spawnAnim();
                            this.discard();
                            level.playSound(null, pos, SoundEvents.WOOL_BREAK, SoundSource.HOSTILE, 1, 1);
                        } else {
                            this.setBurrowing(false);
                        }
                    } else {
                        if (burrowingTicks % 4 == 0)
                            level.playSound(null, pos, SoundEvents.WOOL_HIT, SoundSource.HOSTILE, 0.5f, 1.2f);
                    }
                }
            }
        } else if (burrowingTicks > 0) {
            this.burrowingTicks = Math.max(0, this.burrowingTicks - 4);
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

    public static boolean isValidBedForInfestation(BlockState state) {
        Block block = state.getBlock();
        return block instanceof BedBlock && !state.getValue(BedBlock.OCCUPIED);
    }

    public static AttributeSupplier.Builder makeAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 9.0)
                .add(Attributes.MOVEMENT_SPEED, 0.325).add(Attributes.ATTACK_DAMAGE, 1.0);
    }

    private static class BedbugNavigation extends WallClimberNavigation {
        BedbugNavigation(BedbugEntity frog, Level level) {
            super(frog, level);
        }

        @Override
        protected PathFinder createPathFinder(int maxVisitedNodes) {
            this.nodeEvaluator = new BedbugNodeEvaluator();
            this.nodeEvaluator.setCanPassDoors(true);
            return new PathFinder(this.nodeEvaluator, maxVisitedNodes);
        }
    }

    private static class BedbugNodeEvaluator extends WalkNodeEvaluator {

        public BedbugNodeEvaluator() {
            super();
        }

        @Override
        protected double getFloorLevel(BlockPos pos) {
            BlockPos blockPos = pos.below();
            BlockGetter blockGetter = this.currentContext.level();

            BlockState state = blockGetter.getBlockState(blockPos);
            if (state.is(SleepTight.BEDBUG_WALK_THROUGH)) return blockPos.getY();
            VoxelShape voxelShape = state.getCollisionShape(blockGetter, blockPos);
            return blockPos.getY() + (voxelShape.isEmpty() ? 0.0 : voxelShape.max(Direction.Axis.Y));
        }

        //same as super
        @Override
        public Set<PathType> getPathTypeWithinMobBB(PathfindingContext context, int x, int y, int z) {
            EnumSet<PathType> enumSet = EnumSet.noneOf(PathType.class);

            for(int i = 0; i < this.entityWidth; ++i) {
                for(int j = 0; j < this.entityHeight; ++j) {
                    for(int k = 0; k < this.entityDepth; ++k) {
                        int l = i + x;
                        int m = j + y;
                        int n = k + z;
                        PathType pathType = this.getPathType(context, l, m, n);
                        pathType = modifyPathType(context, l, m, n, pathType);
                        BlockPos blockPos = this.mob.blockPosition();
                        boolean bl = this.canPassDoors();
                        if (pathType == PathType.DOOR_WOOD_CLOSED && this.canOpenDoors() && bl) {
                            pathType = PathType.WALKABLE_DOOR;
                        }

                        if (pathType == PathType.DOOR_OPEN && !bl) {
                            pathType = PathType.BLOCKED;
                        }

                        if (pathType == PathType.RAIL && this.getPathType(context, blockPos.getX(), blockPos.getY(), blockPos.getZ()) != PathType.RAIL && this.getPathType(context, blockPos.getX(), blockPos.getY() - 1, blockPos.getZ()) != PathType.RAIL) {
                            pathType = PathType.UNPASSABLE_RAIL;
                        }

                        enumSet.add(pathType);
                    }
                }
            }

            return enumSet;
        }

        protected PathType modifyPathType(PathfindingContext blockGetter, int x, int y, int z, PathType nodeType) {
            if (nodeType == PathType.DOOR_OPEN || nodeType == PathType.DOOR_WOOD_CLOSED ||
                    nodeType == PathType.WALKABLE_DOOR) return PathType.OPEN;
            if (nodeType == PathType.BLOCKED && blockGetter.getBlockState(BlockPos.containing(x,y,z))
                    .getBlock() instanceof BedBlock) {
                return PathType.WALKABLE;
            }
            return nodeType;
        }
    }

    public static boolean checkMonsterSpawnRules(EntityType<? extends Monster> type, ServerLevelAccessor level, MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        if (spawnType == MobSpawnType.EVENT) {
            if (level.getDifficulty() != Difficulty.PEACEFUL) {
                int maxLight = CommonConfigs.BEDBUG_MAX_LIGHT.get();
                if (maxLight < 15) {
                    int light = Math.max(level.getBrightness(LightLayer.BLOCK, pos), level.getBrightness(LightLayer.SKY, pos));
                    return light <= maxLight;
                }
                return true;
            }
        }
        return Monster.checkMonsterSpawnRules(type, level, spawnType, pos, random);
    }

}
