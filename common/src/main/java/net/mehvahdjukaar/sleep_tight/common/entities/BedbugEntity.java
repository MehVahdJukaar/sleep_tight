package net.mehvahdjukaar.sleep_tight.common.entities;

import net.mehvahdjukaar.sleep_tight.SleepTight;
import net.mehvahdjukaar.sleep_tight.common.blocks.InfestedBedBlock;
import net.mehvahdjukaar.sleep_tight.common.network.ClientBoundParticleMessage;
import net.mehvahdjukaar.sleep_tight.common.network.NetworkHandler;
import net.mehvahdjukaar.sleep_tight.configs.CommonConfigs;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
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
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Stream;

public class BedbugEntity extends Monster {
    private static final EntityDataAccessor<Byte> DATA_FLAGS_ID = SynchedEntityData.defineId(BedbugEntity.class, EntityDataSerializers.BYTE);
    private BlockPos targetBed;

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
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(1, new ClimbOnTopOfPowderSnowGoal(this, this.level));
        this.goalSelector.addGoal(2, new InfestBedGoal(this, 1, 20));
        this.goalSelector.addGoal(3, new BedbugLeapGoal(this, 0.25F));
        this.goalSelector.addGoal(4, new BedbugAttackGoal(this));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));

        // this.targetSelector.addGoal(8, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_FLAGS_ID, (byte) 0);
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

        if (this.getTarget() != null)
            this.getLookControl().setLookAt(this.getTarget());


        if (!this.level.isClientSide) {
            this.setClimbing(this.horizontalCollision);
        } else {
            this.prevBurrowingTicks = burrowingTicks;
        }

        if (this.isBurrowing()) {
            BlockPos pos = this.blockPosition();

            BlockState feetBlockState = this.level.getBlockState(pos);
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
                        if (InfestedBedBlock.infestBed(level, feetBlockState, pos)) {
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

    @Override
    public MobType getMobType() {
        return MobType.ARTHROPOD;
    }

    /**
     * Returns true if the WatchableObject (Byte) is 0x01 otherwise returns false. The WatchableObject is updated using setBesideClimableBlock.
     */
    public boolean isClimbing() {
        return (this.entityData.get(DATA_FLAGS_ID) & 1) != 0;
    }

    public boolean isSplattered() {
        return (this.entityData.get(DATA_FLAGS_ID) & 3) != 0;
    }

    public boolean isBurrowing() {
        return (this.entityData.get(DATA_FLAGS_ID) & 5) != 0;
    }

    /**
     * Updates the WatchableObject (Byte) created in entityInit(), setting it to 0x01 if par1 is true or 0x00 if it is false.
     */
    public void setClimbing(boolean climbing) {
        byte b = this.entityData.get(DATA_FLAGS_ID);
        if (climbing) {
            b = (byte) (b | 1);
        } else {
            b &= -2;
        }
        this.entityData.set(DATA_FLAGS_ID, b);
    }

    public void setSplattered(boolean splattered) {
        byte b = this.entityData.get(DATA_FLAGS_ID);
        if (splattered) {
            b = (byte) (b | 3);
        } else {
            b &= -3;
        }
        this.entityData.set(DATA_FLAGS_ID, b);
    }

    public void setBurrowing(boolean burrowing) {
        byte b = this.entityData.get(DATA_FLAGS_ID);
        if (burrowing) {
            b = (byte) (b | 5);
        } else {
            b &= -5;
        }
        this.entityData.set(DATA_FLAGS_ID, b);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new BedbugNavigation(this, level);
    }

    public void setBedTarget(BlockPos pos) {
        this.targetBed = new BlockPos(pos); //for mutable
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("targetBed")) {
            this.targetBed = NbtUtils.readBlockPos(compound.getCompound("targetBed"));
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        if (targetBed != null) {
            compound.put("targetBed", NbtUtils.writeBlockPos(targetBed));
        }
    }

    protected void onInsideBlock(BlockState state, BlockPos pos) {
        if (state.getBlock() instanceof DoorBlock) {
            //gets full shape
            VoxelShape voxelShape = state.getCollisionShape(this.level, pos);
            VoxelShape voxelShape2 = voxelShape.move(pos.getX(), pos.getY(), pos.getZ());
            if (Shapes.joinIsNotEmpty(voxelShape2, Shapes.create(this.getBoundingBox()), BooleanOp.AND)) {

                NetworkHandler.CHANNEL.sentToAllClientPlayersTrackingEntity(this, ClientBoundParticleMessage.bedbugDoor(pos));
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
        if (this.level.hasChunksAt(blockPos, blockPos2)) {
            BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();

            for (int i = blockPos.getX(); i <= blockPos2.getX(); ++i) {
                for (int j = blockPos.getY(); j <= blockPos2.getY(); ++j) {
                    for (int k = blockPos.getZ(); k <= blockPos2.getZ(); ++k) {
                        mutableBlockPos.set(i, j, k);
                        BlockState blockState = this.level.getBlockState(mutableBlockPos);

                        try {
                            blockState.entityInside(this.level, mutableBlockPos, this);
                            this.onInsideBlock(blockState, mutableBlockPos);
                        } catch (Exception e) {
                            CrashReport crashReport = CrashReport.forThrowable(e, "Colliding entity with block");
                            CrashReportCategory crashReportCategory = crashReport.addCategory("Block being collided with");
                            CrashReportCategory.populateBlockDetails(crashReportCategory, this.level, mutableBlockPos, blockState);
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

    static class BedbugLeapGoal extends LeapAtTargetGoal {

        private final Mob mob;

        public BedbugLeapGoal(Mob mob, float f) {
            super(mob, f);
            this.mob = mob;
        }

        @Override
        public void start() {
            //doesnt even work
            this.mob.getLookControl().setLookAt(this.mob.getTarget());
            super.start();
        }
    }

    public static boolean isValidBedForInfestation(BlockState state) {
        Block block = state.getBlock();
        return block instanceof BedBlock && block != SleepTight.INFESTED_BED && !state.getValue(BedBlock.OCCUPIED);
    }

    static class InfestBedGoal extends MoveToBlockGoal {

        private final List<BlockPos> blacklist = new ArrayList<>();

        private final BedbugEntity bedBug;
        private final int searchRange;
        private int ticksOnTarget = 0;
        private boolean reachedTarget;

        public InfestBedGoal(BedbugEntity pathfinderMob, double speed, int searchRange) {
            super(pathfinderMob, speed, searchRange);
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.LOOK, Flag.TARGET));
            this.bedBug = pathfinderMob;
            this.searchRange = searchRange;
        }

        @Override
        protected boolean isReachedTarget() {
            return reachedTarget;
        }

        @Override
        public void tick() {
            BlockPos blockPos = this.getMoveToTarget();
            double dist = blockPos.distToCenterSqr(this.mob.position());
            if (dist >= 1) {
                this.reachedTarget = false;
                ++this.tryTicks;
                if (this.shouldRecalculatePath()) {
                    double s = this.speedModifier;
                    if (dist < (1.5 * 1.5)) s /= 2;
                    this.mob.getNavigation().moveTo((blockPos.getX()) + 0.5, blockPos.getY(),
                            (blockPos.getZ()) + 0.5, s);
                }
            } else {
                this.reachedTarget = true;
                --this.tryTicks;
            }

            if (this.isReachedTarget()) {
                ticksOnTarget++;
                this.bedBug.setBurrowing(true);

            } else ticksOnTarget = 0;
        }

        @Override
        protected boolean isValidTarget(LevelReader level, BlockPos pos) {
            return isValidBedForInfestation(level.getBlockState(pos));
        }

        @Override
        protected int nextStartTick(PathfinderMob creature) {
            return super.nextStartTick(creature) * 100;
        }

        @Override
        protected boolean findNearestBlock() {
            if (bedBug.targetBed != null && this.isValidTarget(this.mob.level, bedBug.targetBed)) {
                this.blockPos = bedBug.targetBed;
                return true;
            }
            //TODO: account for occupied
            var v = findNearestBed();
            if (!v.isEmpty()) {
                bedBug.targetBed = v.get(0);
                this.blockPos = v.get(0);
                return true;
            }

            return false;// super.findNearestBlock();
        }

        private List<BlockPos> findNearestBed() {
            BlockPos pos = bedBug.blockPosition();
            ServerLevel level = (ServerLevel) bedBug.level;
            PoiManager poiManager = level.getPoiManager();
            Stream<PoiRecord> stream = poiManager.getInRange((h) ->
                    h.is(PoiTypes.HOME), pos, searchRange, PoiManager.Occupancy.ANY);
            return stream.map(PoiRecord::getPos)
                    .filter(p -> isValidTarget(level, p))
                    .sorted(Comparator.comparingDouble((p) -> p.distSqr(pos)))
                    .toList();
        }
    }


    static class BedbugAttackGoal extends MeleeAttackGoal {
        private final BedbugEntity bedbug;

        public BedbugAttackGoal(BedbugEntity spider) {
            super(spider, 1.0, true);
            this.bedbug = spider;
        }

        public boolean canUse() {
            return super.canUse();
        }

        public boolean canContinueToUse() {
            if (this.bedbug.targetBed != null && this.mob.getRandom().nextInt(100) == 0) {
                this.mob.setTarget(null);
                return false;
            } else {
                return super.canContinueToUse();
            }
        }
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
            BlockState state = level.getBlockState(blockPos);
            if (state.is(SleepTight.BEDBUG_WALK_THROUGH)) return blockPos.getY();
            VoxelShape voxelShape = state.getCollisionShape(level, blockPos);
            return blockPos.getY() + (voxelShape.isEmpty() ? 0.0 : voxelShape.max(Direction.Axis.Y));
        }

        @Override
        protected BlockPathTypes evaluateBlockPathType(BlockGetter blockGetter, BlockPos blockPos, BlockPathTypes nodeType) {
            if (nodeType == BlockPathTypes.DOOR_OPEN || nodeType == BlockPathTypes.DOOR_WOOD_CLOSED ||
                    nodeType == BlockPathTypes.WALKABLE_DOOR) return BlockPathTypes.OPEN;
            if(nodeType == BlockPathTypes.BLOCKED && level.getBlockState(blockPos).getBlock() instanceof BedBlock){
                return BlockPathTypes.WALKABLE;
            }
            return super.evaluateBlockPathType(level, blockPos, nodeType);
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
