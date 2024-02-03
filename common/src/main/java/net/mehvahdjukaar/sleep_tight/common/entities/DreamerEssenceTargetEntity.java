package net.mehvahdjukaar.sleep_tight.common.entities;

import dev.architectury.injectables.annotations.PlatformOnly;
import net.mehvahdjukaar.moonlight.api.platform.network.NetworkHelper;
import net.mehvahdjukaar.sleep_tight.SleepTight;
import net.mehvahdjukaar.sleep_tight.configs.ClientConfigs;
import net.mehvahdjukaar.sleep_tight.common.network.ClientBoundParticleMessage;
import net.mehvahdjukaar.sleep_tight.common.network.ModMessages;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class DreamerEssenceTargetEntity extends LivingEntity {


    public DreamerEssenceTargetEntity(Level level, BlockPos pos) {
        super(SleepTight.DREAMER_ESSENCE_ENTITY.get(), level);
        this.setPos(Vec3.atBottomCenterOf(pos));
    }

    public DreamerEssenceTargetEntity(EntityType<? extends LivingEntity> entityEntityType, Level level) {
        super(entityEntityType, level);
    }

    //@Override
    @PlatformOnly(PlatformOnly.FORGE)
    public ItemStack getPickedResult(HitResult target) {
        return new ItemStack(SleepTight.DREAMER_ESSENCE.get());
    }

    @Override
    public boolean isAttackable() {
        return true;
    }

    @Override
    public boolean skipAttackInteraction(Entity entity) {
        return true;
    }

    @Override
    protected float getStandingEyeHeight(Pose pose, EntityDimensions dimensions) {
        return dimensions.height;
    }

    @Override
    public void tick() {
        Level level = level();
        if (level.getBlockState(this.blockPosition()).getBlock() != SleepTight.DREAMER_ESSENCE.get()) {
            this.discard();
        }
        if (level.isClientSide && random.nextFloat() < ClientConfigs.PARTICLE_SPAWN_FREQUENCY.get()) {
            if (Minecraft.getInstance().cameraEntity.distanceToSqr(this) < 30 * 30d) {
                BlockPos pos = this.blockPosition();
                long l = level.getGameTime();
                float period = 100;
                float h = (Math.floorMod((pos.getX() * 7L + pos.getY() * 9L + pos.getZ() * 13L) + l, (long) period)) / period;
                level.addParticle(SleepTight.DREAM_PARTICLE.get(), pos.getX() + 0.5f,
                        pos.getY() + 10 / 16f, pos.getZ() + 0.5f, h, 0, 1);
            }
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!this.isRemoved() && !level().isClientSide) {
            NetworkHelper.sentToAllClientPlayersTrackingEntity(this, ClientBoundParticleMessage.dreamEssence(this.blockPosition()));
        }
        super.remove(reason);
    }

    public static void spawnDeathParticles(Level level, BlockPos pos) {
        if (level.isClientSide()) {
            for (int i = 0; i < Mth.randomBetween(level.getRandom(), 30, 50); i++) {
                level.addParticle(SleepTight.DREAM_PARTICLE.get(), pos.getX() + 0.5,
                        pos.getY() + 0.3125, pos.getZ() + 0.5, 0, 0, 2);
            }
        }
    }

    @Override
    public void baseTick() {
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof Phantom) {
            return true;
        }
        return false;
    }


    @Override
    public boolean mayInteract(Level level, BlockPos pos) {
        return false;
    }

    @Override
    public void move(MoverType type, Vec3 pos) {
    }

    @Override
    public void setHealth(float health) {
    }

    @Override
    public Iterable<ItemStack> getArmorSlots() {
        return List.of();
    }

    @Override
    public ItemStack getItemBySlot(EquipmentSlot slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
    }

    @Override
    public HumanoidArm getMainArm() {
        return HumanoidArm.RIGHT;
    }

    @Override
    public void setDeltaMovement(Vec3 motionIn) {
    }

    @Override
    public void knockback(double strength, double x, double z) {
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    protected boolean isImmobile() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void markHurt() {
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource damageSource) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGroundIn, BlockState state, BlockPos pos) {
    }

    @Override
    public void setNoGravity(boolean ignored) {
        super.setNoGravity(true);
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHitIn) {
    }

    @Override
    public SoundEvent getHurtSound(DamageSource ds) {
        return null;
    }

    public static AttributeSupplier.Builder makeAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.FOLLOW_RANGE, 16.0D)
                .add(Attributes.MOVEMENT_SPEED, 0D)
                .add(Attributes.MAX_HEALTH, 40D)
                .add(Attributes.ARMOR, 0D)
                .add(Attributes.ATTACK_DAMAGE, 0D)
                .add(Attributes.FLYING_SPEED, 0D);
    }

}
