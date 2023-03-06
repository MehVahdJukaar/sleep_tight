package net.mehvahdjukaar.sleep_tight.common;

import net.mehvahdjukaar.sleep_tight.SleepTight;
import net.minecraft.client.model.SpiderModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation;
import net.minecraft.world.entity.animal.Turtle;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class BedbugEntity extends Monster {
    private static final EntityDataAccessor<Byte> DATA_FLAGS_ID = SynchedEntityData.defineId(BedbugEntity.class, EntityDataSerializers.BYTE);

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
        this.goalSelector.addGoal(3, new LeapAtTargetGoal(this, 0.4F));
        //this.goalSelector.addGoal(4, new BedbugEntity.SpiderAttackGoal(this));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        //this.targetSelector.addGoal(2, new BedbugEntity.SpiderTargetGoal(this, Player.class));
        //this.targetSelector.addGoal(3, new BedbugEntity.SpiderTargetGoal(this, IronGolem.class));
    }

    @Override
    public double getPassengersRidingOffset() {
        return (this.getBbHeight() * 0.5F);
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
       // this.yBodyRot = this.getYRot();
        super.tick();

        if (!this.level.isClientSide) {
            this.setClimbing(this.horizontalCollision);
        }
    }

    @Override
    public void setYBodyRot(float yBodyRot) {
     //   this.setYRot(yBodyRot);
        super.setYBodyRot(yBodyRot);
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

    @Override
    protected float getStandingEyeHeight(Pose pose, EntityDimensions dimensions) {
        return 0.65F;
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new BedbugNavigation(this, level);
    }


    static class SpiderAttackGoal extends MeleeAttackGoal {
        public SpiderAttackGoal(Spider spider) {
            super(spider, 1.0, true);
        }

        public boolean canUse() {
            return super.canUse() && !this.mob.isVehicle();
        }

        public boolean canContinueToUse() {
            float f = this.mob.getLightLevelDependentMagicValue();
            if (f >= 0.5F && this.mob.getRandom().nextInt(100) == 0) {
                this.mob.setTarget(null);
                return false;
            } else {
                return super.canContinueToUse();
            }
        }

        @Override
        protected double getAttackReachSqr(LivingEntity attackTarget) {
            return (4.0F + attackTarget.getBbWidth());
        }
    }

    static class SpiderTargetGoal<T extends LivingEntity> extends NearestAttackableTargetGoal<T> {
        public SpiderTargetGoal(Spider spider, Class<T> class_) {
            super(spider, class_, true);
        }

        public boolean canUse() {
            float f = this.mob.getLightLevelDependentMagicValue();
            return f >= 0.5F ? false : super.canUse();
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
            if(nodeType == BlockPathTypes.DOOR_OPEN || nodeType == BlockPathTypes.DOOR_WOOD_CLOSED ||
            nodeType == BlockPathTypes.WALKABLE_DOOR)return BlockPathTypes.OPEN;
            return super.evaluateBlockPathType(level, canOpenDoors, canEnterDoors, pos, nodeType);
        }
    }


    public static void trySpawning(BlockPos pos, ServerPlayer player) {
    }
}
