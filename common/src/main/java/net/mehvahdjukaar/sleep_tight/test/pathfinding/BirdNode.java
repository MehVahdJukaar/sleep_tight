package net.mehvahdjukaar.sleep_tight.test.pathfinding;

import net.minecraft.world.level.pathfinder.Node;

/**
 * A pathfinding node whose identity includes the heading it was entered with, so momentum
 * survives the search: the same cell reached flying east and flying north are two different
 * states with different costs and different reachable neighbors.
 */
public class BirdNode extends Node {

    /** Horizontal heading bin, 0..7 counter-clockwise from +X in 45 degree steps. */
    public final int heading;

    public BirdNode(int x, int y, int z, int heading) {
        super(x, y, z);
        this.heading = heading;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof BirdNode node && super.equals(other) && this.heading == node.heading;
    }

    @Override
    public int hashCode() {
        return super.hashCode() * 31 + this.heading;
    }
}
