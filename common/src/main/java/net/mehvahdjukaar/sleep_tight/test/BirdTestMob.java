package net.mehvahdjukaar.sleep_tight.test;

import net.mehvahdjukaar.sleep_tight.test.controller.BirdLookControl;
import net.mehvahdjukaar.sleep_tight.test.controller.BirdMoveControl;
import net.mehvahdjukaar.sleep_tight.test.navigator.BirdPathNavigation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;
import org.jetbrains.annotations.Nullable;

/**
 * Scratch bird for testing the lattice pathfinder in game. Not part of the mod content.
 * <p>
 * It flies with {@link BirdPathNavigation} but has no goal that ever sets a walk target: paths are
 * computed on demand by {@link BirdDebug}, drawn, and then flown at a crawl so the shape of the
 * lattice path can actually be watched. The mob's yaw seeds the search heading, so the first turn
 * of a new path depends on where it happened to be pointing when the query was made.
 */
public class BirdTestMob extends PathfinderMob implements FlyingAnimal {

    // a thrust fraction once multiplied by FLYING_SPEED, not a speed. 0.7 * 0.4 = 0.28 of full
    // throttle: deliberately a crawl, the point is to watch the path being flown
    private static final double FOLLOW_SPEED = 0.7;

    // server side only, kept around so the client side debug renderer entry can be refreshed
    @Nullable
    private Path debugPath;

    public BirdTestMob(EntityType<? extends BirdTestMob> entityType, Level level) {
        super(entityType, level);
        this.moveControl = new BirdMoveControl(this);
        this.lookControl = new BirdLookControl(this);
        this.setNoGravity(true);
    }

    /**
     * Only implemented for the drag: {@code LivingEntity.travel} gives a FlyingAnimal 0.91 vertical
     * drag and everything else 0.98, which is a 50 tick time constant and lets a no-gravity flier
     * build up several times its horizontal speed on the way up or down.
     */
    @Override
    public boolean isFlying() {
        return !this.onGround();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(1, new RandomLookAroundGoal(this));
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new BirdPathNavigation(this, level);
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        // keep resending so the highlighted next node and the "thinking" overlay track the mob live
        if (this.debugPath != null && !this.level().isClientSide && this.tickCount % 5 == 0) {
            BirdDebug.broadcastPath(this, this.debugPath);
        }
    }

    /** Draws the path and starts flying it. Passing null just clears whatever was being followed. */
    public void followPath(@Nullable Path path) {
        this.debugPath = path;
        // clear first: moveTo keeps the old path when the new one compares equal, and an already
        // finished one would make it bail out
        this.getNavigation().stop();
        if (path != null) {
            this.getNavigation().moveTo(path, FOLLOW_SPEED);
        }
    }

    public static AttributeSupplier.Builder makeAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FLYING_SPEED, 0.4)
                // doubles as the pathfinder's range cap and node budget (16 nodes per block of
                // follow range), so it needs to be generous enough to path across a test arena
                .add(Attributes.FOLLOW_RANGE, 64.0);
    }
}
