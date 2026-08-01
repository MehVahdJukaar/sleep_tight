package net.mehvahdjukaar.sleep_tight.test.pathfinding;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

/**
 * A finished {@link Path} sampled into the two things the flying layers need from it: the world point
 * to fly through at each node, and how walled in that node was. Read once, since the search is over
 * by the time a path exists and the shape never changes afterwards.
 * <p>
 * It exists so the follower and the throttle planner cannot disagree about where the line is. They
 * sampled the path separately before, which is fine right up until one of them corrects for something
 * the other does not, and then the profile is describing a line the mob is not flying.
 */
public record FlightLine(Vec3[] points, float[] enclosure) {

    public static FlightLine of(Path path, Entity entity) {
        int count = path.getNodeCount();
        Vec3[] points = new Vec3[count];
        float[] enclosure = new float[count];
        double lift = verticalNodeOffset(entity);
        for (int i = 0; i < count; i++) {
            points[i] = path.getEntityPosAtNode(entity, i).add(0.0, lift, 0.0);
            // a path that went through trimPath can contain plain vanilla nodes, so this is optional
            enclosure[i] = path.getNode(i) instanceof BirdNode bird ? bird.enclosure : 0.0F;
        }
        return new FlightLine(points, enclosure);
    }

    /**
     * How far above vanilla's node position a flier should actually sit, and equally the room it then
     * has either side of the line, which is the same number by construction.
     * <p>
     * {@code Path.getEntityPosAtNode} centres a node horizontally in the footprint the search
     * certified but pins it to {@code node.y}, the cell's floor plane. An entity's position is its
     * feet, so that leaves a flier scraping the bottom of its cell with all the headroom above it
     * unused: right for a walker, whose feet really are on the ground, and wrong for anything
     * occupying a cell of air. Lifting by this centres the mob's hitbox in the cells that were tested
     * for it, and turns a corridor with nothing below the line and 0.25 above it into a symmetric one
     * with 0.125 either way.
     * <p>
     * Measured against the height {@code NodeEvaluator.prepare} certifies, {@code floor(bbHeight + 1)}
     * rather than the mob's own, so this only ever claims room the search actually checked. Which does
     * mean a mob exactly one block tall gets half a block of lift, because two whole cells were tested
     * for it.
     */
    public static double verticalNodeOffset(Entity entity) {
        return (Mth.floor(entity.getBbHeight() + 1.0F) - entity.getBbHeight()) * 0.5;
    }

    public int size() {
        return this.points.length;
    }
}
