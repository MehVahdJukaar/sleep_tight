package net.mehvahdjukaar.sleep_tight.test.pathfinding;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathFinder;

/**
 * Drop-in flying navigation using the bird lattice pathfinder. Hook it to a mob by
 * returning this from {@code Mob.createNavigation}.
 */
public class BirdPathNavigation extends FlyingPathNavigation {

    public BirdPathNavigation(Mob mob, Level level) {
        super(mob, level);
    }

    @Override
    protected PathFinder createPathFinder(int maxVisitedNodes) {
        this.nodeEvaluator = new BirdNodeEvaluator();
        this.nodeEvaluator.setCanPassDoors(true);
        return new BirdPathFinder((BirdNodeEvaluator) this.nodeEvaluator, maxVisitedNodes);
    }
}
