package net.mehvahdjukaar.sleep_tight.test.pathfinding;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.pathfinder.FlyNodeEvaluator;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.PathType;
import org.jetbrains.annotations.Nullable;

/**
 * State-lattice evaluator for flying animals. Search states are (x, y, z, heading) instead of
 * plain cells: the heading is the direction of the last step, so the bird has momentum and the
 * pathfinder can charge for turning. Block checks, path types, maluses and the 26-way move
 * geometry are all reused from vanilla's flying evaluator.
 */
public class BirdNodeEvaluator extends FlyNodeEvaluator {

    // heading bin to horizontal step, counter-clockwise from +X (bin = atan2(dz, dx) / 45 deg)
    private static final int[] BIN_DX = {1, 1, 0, -1, -1, -1, 0, 1};
    private static final int[] BIN_DZ = {0, 1, 1, 1, 0, -1, -1, -1};

    // {dx, dy, dz, headingBin}; bin -1 marks the two purely vertical moves, which keep
    // the parent's heading. 8 horizontal bins x dy in {-1,0,1} + 2 verticals = vanilla's 26
    private static final int[][] MOVES = buildMoves();

    private final Long2ObjectMap<BirdNode> latticeNodes = new Long2ObjectOpenHashMap<>();
    // node keys are packed relative to this, so any real world coordinate fits
    private int originX, originY, originZ;

    // search cost of the last run, for the in game comparison against vanilla A* (see PathDebug)
    public int expansions;
    public int generatedNeighbors;

    private static int[][] buildMoves() {
        int[][] moves = new int[26][];
        int i = 0;
        for (int bin = 0; bin < 8; bin++) {
            for (int dy = -1; dy <= 1; dy++) {
                moves[i++] = new int[]{BIN_DX[bin], dy, BIN_DZ[bin], bin};
            }
        }
        moves[i++] = new int[]{0, 1, 0, -1};
        moves[i] = new int[]{0, -1, 0, -1};
        return moves;
    }

    @Override
    public void prepare(PathNavigationRegion level, Mob mob) {
        super.prepare(level, mob);
        this.latticeNodes.clear();
        BlockPos origin = mob.blockPosition();
        this.originX = origin.getX();
        this.originY = origin.getY();
        this.originZ = origin.getZ();
        this.expansions = 0;
        this.generatedNeighbors = 0;
    }

    @Override
    public void done() {
        this.latticeNodes.clear();
        super.done();
    }

    @Override
    public Node getStart() {
        // vanilla picks a safe start cell (water surface, bounding box candidates); we only
        // swap the node for a lattice one carrying the mob's current heading
        Node vanillaStart = super.getStart();
        BirdNode start = this.getLatticeNode(vanillaStart.x, vanillaStart.y, vanillaStart.z,
                yawToBin(this.mob.getYRot()));
        start.type = vanillaStart.type;
        start.costMalus = vanillaStart.costMalus;
        return start;
    }

    // targets stay plain vanilla nodes: reaching is checked by position only

    @Override
    public int getNeighbors(Node[] outputArray, Node node) {
        int count = 0;
        int heading = node instanceof BirdNode bird ? bird.heading : yawToBin(this.mob.getYRot());

        for (int[] move : MOVES) {
            int moveBin = move[3];
            if (moveBin >= 0 && turnAmount(heading, moveBin) > BirdPathfindingConfig.maxTurnBins) {
                continue;
            }
            int newHeading = moveBin < 0 ? heading : moveBin;

            BirdNode neighbor = this.findAcceptedLatticeNode(
                    node.x + move[0], node.y + move[1], node.z + move[2], newHeading);
            if (neighbor == null || neighbor.closed) {
                continue;
            }
            if (!this.hasClearance(node, move[0], move[1], move[2])) {
                continue;
            }
            outputArray[count++] = neighbor;
        }

        this.expansions++;
        this.generatedNeighbors += count;
        return count;
    }

    /**
     * Cost of the transition between two lattice states: turning plus purely vertical flight.
     * These depend on the parent, so they can't live in the node's costMalus (the same state is
     * reachable both by a diagonal climb and by a vertical hop from below).
     */
    public float getEdgeCost(Node from, Node to) {
        float cost = 0;
        if (from.x == to.x && from.z == to.z && from.y != to.y) {
            cost += to.y > from.y ? BirdPathfindingConfig.straightUpCost : BirdPathfindingConfig.straightDownCost;
        }
        if (from instanceof BirdNode a && to instanceof BirdNode b) {
            cost += switch (turnAmount(a.heading, b.heading)) {
                case 1 -> BirdPathfindingConfig.turnCost45;
                case 2 -> BirdPathfindingConfig.turnCost90;
                case 3 -> BirdPathfindingConfig.turnCost135;
                default -> 0.0F;
            };
        }
        return cost;
    }

    @Nullable
    private BirdNode findAcceptedLatticeNode(int x, int y, int z, int heading) {
        PathType type = this.getCachedPathType(x, y, z);
        float malus = this.mob.getPathfindingMalus(type);
        if (malus < 0.0F) {
            return null;
        }
        BirdNode node = this.getLatticeNode(x, y, z, heading);
        node.type = type;
        node.costMalus = Math.max(node.costMalus, malus);
        if (type == PathType.WALKABLE) {
            node.costMalus++; // like vanilla: prefer open air over ground-level cells
        }
        return node;
    }

    private BirdNode getLatticeNode(int x, int y, int z, int heading) {
        return this.latticeNodes.computeIfAbsent(this.packKey(x, y, z, heading),
                key -> new BirdNode(x, y, z, heading));
    }

    /**
     * Same clearance rules as vanilla's flying 26-connectivity: a multi-axis move needs all its
     * single-axis projections passable, and a full corner move also the three edge cells.
     */
    private boolean hasClearance(Node from, int dx, int dy, int dz) {
        int axes = (dx != 0 ? 1 : 0) + (dy != 0 ? 1 : 0) + (dz != 0 ? 1 : 0);
        if (axes < 2) {
            return true;
        }
        if (dx != 0 && !this.isClear(from.x + dx, from.y, from.z)) return false;
        if (dy != 0 && !this.isClear(from.x, from.y + dy, from.z)) return false;
        if (dz != 0 && !this.isClear(from.x, from.y, from.z + dz)) return false;
        if (axes == 3) {
            return this.isClear(from.x + dx, from.y + dy, from.z)
                    && this.isClear(from.x + dx, from.y, from.z + dz)
                    && this.isClear(from.x, from.y + dy, from.z + dz);
        }
        return true;
    }

    private boolean isClear(int x, int y, int z) {
        return this.mob.getPathfindingMalus(this.getCachedPathType(x, y, z)) >= 0.0F;
    }

    private long packKey(int x, int y, int z, int heading) {
        long key = 0;
        key |= ((long) (x - originX) & 0x1FFF);          // 13 signed bits, +-4096
        key |= ((long) (y - originY) & 0x3FF) << 13;     // 10 signed bits
        key |= ((long) (z - originZ) & 0x1FFF) << 23;    // 13 signed bits
        key |= ((long) heading & 0x7) << 36;
        return key;
    }

    // how many 45 degree bins apart two headings are, 0..4
    static int turnAmount(int headingA, int headingB) {
        int diff = Math.abs(headingA - headingB);
        return diff > 4 ? 8 - diff : diff;
    }

    // mc yaw convention: 0 faces +Z (south), 90 faces -X (west)
    static int yawToBin(float yRot) {
        return Math.floorMod(Math.round((yRot + 90.0F) / 45.0F), 8);
    }
}
