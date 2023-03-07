package net.mehvahdjukaar.sleep_tight.common;

import net.mehvahdjukaar.sleep_tight.SleepTight;
import net.mehvahdjukaar.sleep_tight.configs.CommonConfigs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumSet;

public class BedbugEntity extends Monster {
    private static final EntityDataAccessor<Byte> DATA_FLAGS_ID = SynchedEntityData.defineId(BedbugEntity.class, EntityDataSerializers.BYTE);
    private BlockPos targetBed;

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

        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_FLAGS_ID, (byte) 0);
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
        this.setYRot(this.yHeadRot);

        if (!this.level.isClientSide) {
            this.setClimbing(this.horizontalCollision);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SILVERFISH_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.SILVERFISH_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SILVERFISH_DEATH;
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

    static class InfestBedGoal extends MoveToBlockGoal {

        private final BedbugEntity bedBug;

        public InfestBedGoal(BedbugEntity pathfinderMob, double d, int i) {
            super(pathfinderMob, d, i);
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.LOOK, Flag.TARGET));
            this.bedBug = pathfinderMob;
        }

        @Override
        protected boolean isValidTarget(LevelReader level, BlockPos pos) {
            return level.getBlockState(pos).is(SleepTight.VANILLA_BEDS);
        }

        @Override
        protected boolean findNearestBlock() {
            if (bedBug.targetBed != null && this.isValidTarget(this.mob.level, bedBug.targetBed)) {
                this.blockPos = bedBug.targetBed;
                return true;
            }
            return super.findNearestBlock();
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
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 12.0)
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
        protected BlockPathTypes evaluateBlockPathType(BlockGetter level, boolean canOpenDoors, boolean canEnterDoors, BlockPos pos, BlockPathTypes nodeType) {
            if (nodeType == BlockPathTypes.DOOR_OPEN || nodeType == BlockPathTypes.DOOR_WOOD_CLOSED ||
                    nodeType == BlockPathTypes.WALKABLE_DOOR) return BlockPathTypes.OPEN;
            return super.evaluateBlockPathType(level, canOpenDoors, canEnterDoors, pos, nodeType);
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