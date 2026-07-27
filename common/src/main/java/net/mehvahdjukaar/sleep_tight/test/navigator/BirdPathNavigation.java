package net.mehvahdjukaar.sleep_tight.test.navigator;

import net.mehvahdjukaar.sleep_tight.test.controller.BirdFlightConfig;
import net.mehvahdjukaar.sleep_tight.test.controller.BirdMoveControl;
import net.mehvahdjukaar.sleep_tight.test.pathfinding.BirdNodeEvaluator;
import net.mehvahdjukaar.sleep_tight.test.pathfinding.BirdPathFinder;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Drop-in flying navigation using the bird lattice pathfinder. Hook it to a mob by
 * returning this from {@code Mob.createNavigation}, and pair it with {@link BirdMoveControl};
 * vanilla's FlyingMoveControl will not fly these paths as planned.
 * <p>
 * Besides swapping in the finder this replaces the whole waypoint rule with a {@link PathRuler}.
 * Vanilla's is built for mobs that can stop on a dime and turn instantly, neither of which is true
 * here. This class only wires the ruler up; the geometry lives in the ruler and the steering in
 * {@link BirdMoveControl}.
 */
public class BirdPathNavigation extends FlyingPathNavigation {

    @Nullable
    private PathRuler ruler;
    @Nullable
    private Path ruledPath;

    public BirdPathNavigation(Mob mob, Level level) {
        super(mob, level);
    }

    @Override
    protected PathFinder createPathFinder(int maxVisitedNodes) {
        this.nodeEvaluator = new BirdNodeEvaluator();
        this.nodeEvaluator.setCanPassDoors(true);
        return new BirdPathFinder((BirdNodeEvaluator) this.nodeEvaluator, maxVisitedNodes);
    }

    /**
     * Vanilla aims the move control at the next node. Aim it at the ruler's lookahead point instead,
     * which is the whole reason the path gets flown as a curve rather than as a sequence of headings.
     * Navigation ticks before the move control, so this is what the steering ends up seeing.
     */
    @Override
    public void tick() {
        super.tick();
        if (!this.isDone() && this.ruler != null) {
            Vec3 carrot = this.ruler.lookaheadPoint(BirdFlightConfig.lookahead);
            this.mob.getMoveControl().setWantedPosition(
                    carrot.x, this.getGroundY(carrot), carrot.z, this.speedModifier);
        }
    }

    /**
     * No acceptance test at all: the cursor is wherever the mob projects onto the path, and the
     * path's node index is dragged along behind it. Missing a node is no longer an event, so
     * nothing can send the mob back around for one, and the cursor reaching the end of the ruler
     * is what makes the path report itself done.
     * <p>
     * Vanilla's corner cutting (canCutCorner plus shouldTargetNextNodeInDirection) is deliberately
     * not carried over: it skips a node whenever the one after it is closer, which on a banked arc
     * is most of them, and straightens exactly the curve the lattice was built to produce.
     */
    @Override
    protected void followThePath() {
        Vec3 pos = this.getTempMobPos();
        PathRuler ruler = this.ruler();
        ruler.advanceCursorTo(pos, BirdFlightConfig.projectionWindow);
        this.path.setNextNodeIndex(ruler.nextNodeIndex());
        // acceptance spheres are gone, so this now only sizes the markers in the debug path renderer
        this.maxDistanceToWaypoint = (float) BirdFlightConfig.lookahead;
        this.doStuckDetection(pos);
    }

    private PathRuler ruler() {
        if (this.ruler == null || this.ruledPath != this.path) {
            this.ruledPath = this.path;
            this.ruler = new PathRuler(this.path, this.mob);
        }
        return this.ruler;
    }

    /** How far along the current path the mob's projected position sits. 0 if there is no ruler yet. */
    public double getRulerCursor() {
        return this.ruler != null ? this.ruler.cursor() : 0.0;
    }

    /** Total arc length of the current path. 0 if there is no ruler yet. */
    public double getRulerLength() {
        return this.ruler != null ? this.ruler.length() : 0.0;
    }

    @Override
    public void stop() {
        super.stop();
        this.ruler = null;
        this.ruledPath = null;
    }
}
