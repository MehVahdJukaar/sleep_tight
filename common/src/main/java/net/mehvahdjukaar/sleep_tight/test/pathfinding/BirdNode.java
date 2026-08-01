package net.mehvahdjukaar.sleep_tight.test.pathfinding;

import net.minecraft.world.level.pathfinder.Node;

public class BirdNode extends Node {

    /** Horizontal heading bin, 0..7 counter-clockwise from +X in 45 degree steps. */
    public final int heading;

    /**
     * No heading to conserve, so the turn charge does not apply to moves leaving this state.
     * <p>
     * True for a search started from a perched bird, and that is the only case it is true <i>for
     * free</i>. On the ground the body rotates and nothing translates, because the legs supply the
     * reaction: changing heading really does cost zero displacement, so charging nothing is not a
     * concession, it is the physics. In the air there is nothing to push against and every heading
     * change is an arc, which is why the turn ladder exists at all and why it is never lifted for an
     * airborne start.
     * <p>
     * It also survives the purely vertical moves straight off a perch, which is the one place the
     * "feet down" reading does not hold - three blocks up a shaft nothing is touching anything. The
     * justification there is different: a vertical move already pays {@code straightUpCost}, which is
     * the price of having no airspeed. The heading is free because the hover is not.
     * <p>
     * Part of the state identity: it is in the node key, in equals and in hashCode, so a free state
     * can never be reused as the ordinary state for the same cell and heading. Without that, one
     * relaxation reaching the start cell the normal way would inherit the freedom and the turn cap
     * would quietly stop applying mid-path.
     * <p>
     * When it is set, {@link #heading} is meaningless and only there to keep the key well defined.
     */
    public final boolean freeHeading;

    /**
     * How walled in this cell is, 0 in open air and 1 boxed in on all 26 sides. The raw measurement,
     * not the charge: the search turns it into a cost through {@code wallHugCost}, the throttle
     * planner reads it as "how much room is there to swing wide here", and the debug renderer
     * shades by it. Deliberately outside equals/hashCode, it is not part of the state identity.
     */
    public float enclosure;

    public BirdNode(int x, int y, int z, int heading, boolean freeHeading) {
        super(x, y, z);
        this.heading = heading;
        this.freeHeading = freeHeading;
    }

    @Override
    public Node cloneAndMove(int x, int y, int z) {
        BirdNode moved = new BirdNode(x, y, z, this.heading, this.freeHeading);
        moved.enclosure = this.enclosure;
        moved.type = this.type;
        moved.costMalus = this.costMalus;
        moved.walkedDistance = this.walkedDistance;
        moved.g = this.g;
        moved.h = this.h;
        moved.f = this.f;
        moved.cameFrom = this.cameFrom;
        moved.closed = this.closed;
        moved.heapIdx = this.heapIdx;
        return moved;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof BirdNode node && super.equals(other)
                && this.heading == node.heading && this.freeHeading == node.freeHeading;
    }

    @Override
    public int hashCode() {
        return (super.hashCode() * 31 + this.heading) * 31 + (this.freeHeading ? 1 : 0);
    }
}
