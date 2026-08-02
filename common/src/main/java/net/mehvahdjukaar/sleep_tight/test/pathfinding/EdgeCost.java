package net.mehvahdjukaar.sleep_tight.test.pathfinding;

import net.minecraft.world.level.pathfinder.Node;

/**
 * The full price of one step, split into the terms that produced it. This is exactly what the
 * search compared when it picked one successor over the others offered at the same node, which is
 * otherwise invisible: the finished path only shows the winner, not what it beat or by how much.
 * <p>
 * {@link BirdNodeEvaluator#getEdgeCost} is the sum of the four lattice terms, so the two cannot
 * drift apart.
 */
public record EdgeCost(float distance, float malus, float clearance, float vertical, float turn,
                       float pitch) {

    public static final EdgeCost NONE = new EdgeCost(0, 0, 0, 0, 0, 0);

    /**
     * Reads clearance off the destination node rather than remeasuring it, so this works on a
     * finished path with no evaluator or level around. The evaluator stamps the node with the same
     * measurement it charges for.
     */
    public static EdgeCost between(Node from, Node to) {
        float clearance = (to instanceof BirdNode bird ? bird.enclosure : 0) * BirdPathfindingConfig.wallHugCost;

        float vertical = 0;
        if (from.x == to.x && from.z == to.z && from.y != to.y) {
            vertical = to.y > from.y ? BirdPathfindingConfig.straightUpCost : BirdPathfindingConfig.straightDownCost;
        }

        // a state with no heading to conserve charges nothing to leave in any direction: its own
        // heading and climb are undefined, so the bin differences against them would be noise
        float turn = 0;
        float pitch = 0;
        if (from instanceof BirdNode a && to instanceof BirdNode b && !a.freeHeading) {
            turn = switch (BirdNodeEvaluator.turnAmount(a.heading, b.heading)) {
                case 1 -> BirdPathfindingConfig.turnCost45;
                case 2 -> BirdPathfindingConfig.turnCost90;
                case 3 -> BirdPathfindingConfig.turnCost135;
                case 4 -> BirdPathfindingConfig.turnCost180;
                default -> 0.0F;
            };
            pitch = switch (BirdNodeEvaluator.pitchAmount(a.climb, b.climb)) {
                case 1 -> BirdPathfindingConfig.pitchCost45;
                case 2 -> BirdPathfindingConfig.pitchCost90;
                default -> 0.0F;
            };
        }
        return new EdgeCost(from.distanceTo(to), to.costMalus, clearance, vertical, turn, pitch);
    }

    /** Everything the lattice adds on top of the step length a plain flying A* would have paid. */
    public float extras() {
        return this.clearance + this.vertical + this.turn + this.pitch;
    }

    public float total() {
        return this.distance + this.malus + this.extras();
    }

    public EdgeCost plus(EdgeCost other) {
        return new EdgeCost(this.distance + other.distance, this.malus + other.malus,
                this.clearance + other.clearance, this.vertical + other.vertical,
                this.turn + other.turn, this.pitch + other.pitch);
    }
}
