package net.mehvahdjukaar.sleep_tight.test.navigator;

import net.mehvahdjukaar.sleep_tight.test.controller.PerchingFlier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;

/**
 * Vanilla walking, with the one thing it cannot know bolted on: that this mob can also fly.
 * <p>
 * Everything about following a ground path is inherited untouched. All this does is send a new
 * destination back through the mob's own choice, because while a walk is in progress this is what
 * {@code getNavigation()} hands back, and a goal asking to go somewhere thirty blocks away would
 * otherwise get thirty blocks of walking. The flying half has the mirror image of this override in
 * {@link BirdFlightNavigation}, so the answer is the same whichever half was installed when the
 * question arrived.
 */
public class BirdWalkNavigation extends GroundPathNavigation {

    public BirdWalkNavigation(Mob mob, Level level) {
        super(mob, level);
    }

    @Override
    public boolean moveTo(double x, double y, double z, double speed) {
        return this.mob instanceof PerchingFlier flier
                ? flier.travelTo(BlockPos.containing(x, y, z), 1, speed)
                : super.moveTo(x, y, z, speed);
    }

    @Override
    public boolean moveTo(double x, double y, double z, int accuracy, double speed) {
        return this.mob instanceof PerchingFlier flier
                ? flier.travelTo(BlockPos.containing(x, y, z), accuracy, speed)
                : super.moveTo(x, y, z, accuracy, speed);
    }

    @Override
    public boolean moveTo(Entity entity, double speed) {
        return this.mob instanceof PerchingFlier flier
                ? flier.travelTo(entity.blockPosition(), 1, speed)
                : super.moveTo(entity, speed);
    }
}
