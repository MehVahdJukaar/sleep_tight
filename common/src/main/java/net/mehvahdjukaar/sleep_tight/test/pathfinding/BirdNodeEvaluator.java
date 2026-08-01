package net.mehvahdjukaar.sleep_tight.test.pathfinding;

import it.unimi.dsi.fastutil.longs.Long2FloatMap;
import it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap;
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

    /**
     * How many discrete headings a cell can be entered with, and therefore how many search states
     * exist per cell. The whole lattice is sized off this: the move table, the key packing, and the
     * node budget the navigation has to ask for.
     */
    public static final int HEADING_BINS = 8;

    // heading bin to horizontal step, counter-clockwise from +X (bin = atan2(dz, dx) / 45 deg)
    private static final int[] BIN_DX = {1, 1, 0, -1, -1, -1, 0, 1};
    private static final int[] BIN_DZ = {0, 1, 1, 1, 0, -1, -1, -1};

    // {dx, dy, dz, headingBin}; bin -1 marks the two purely vertical moves, which keep
    // the parent's heading. 8 horizontal bins x dy in {-1,0,1} + 2 verticals = vanilla's 26
    private static final int[][] MOVES = buildMoves();

    // a touching cell's share of the wall hug charge is its inverse distance: faces are 1 block
    // away, edges 1.41, corners 1.73. A cell walled in on all 26 sides would total this weight
    private static final float EDGE_WEIGHT = (float) (1 / Math.sqrt(2));
    private static final float CORNER_WEIGHT = (float) (1 / Math.sqrt(3));
    private static final float FULLY_ENCLOSED_WEIGHT = 6 + 12 * EDGE_WEIGHT + 8 * CORNER_WEIGHT;
    private static final float NOT_MEASURED = -1.0F;

    private final Long2ObjectMap<BirdNode> latticeNodes = new Long2ObjectOpenHashMap<>();
    // how walled in each cell is. Keyed per cell rather than per lattice state, because a cell is
    // offered as a neighbor once per heading bin and once per parent, and each measurement is 26
    // path type lookups. Same relative packing as latticeNodes, cleared alongside it
    private final Long2FloatMap enclosures = new Long2FloatOpenHashMap();
    // node keys are packed relative to this, so any real world coordinate fits
    private int originX, originY, originZ;

    public BirdNodeEvaluator() {
        this.enclosures.defaultReturnValue(NOT_MEASURED);
    }

    // search cost of the last run, for the in game comparison against vanilla A* (see PathDebug)
    public int expansions;
    public int generatedNeighbors;

    private static int[][] buildMoves() {
        int[][] moves = new int[26][];
        int i = 0;
        for (int bin = 0; bin < HEADING_BINS; bin++) {
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
        this.enclosures.clear();
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
        this.enclosures.clear();
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
     * Everything charged for arriving at {@code to} beyond the step length: turning, purely
     * vertical flight, and how walled in the destination is.
     * <p>
     * Turning and vertical cost depend on the parent, so they can't live in the node's costMalus
     * (the same state is reachable both by a diagonal climb and by a vertical hop from below).
     * Wall clearance is a plain property of the destination cell and would belong in costMalus,
     * but vanilla's findAcceptedNode pattern <i>accumulates</i> into that field
     * ({@code costMalus = max(costMalus, malus)} then {@code ++} for WALKABLE) on a cached node, so
     * a clearance charge added there would grow every time the cell is re-offered. Charging it here
     * is recomputed per relaxation and stays idempotent.
     */
    public float getEdgeCost(Node from, Node to) {
        // the split lives in EdgeCost so the debug renderer can show the same terms the search
        // weighed here, rather than a second copy of the formula that can quietly go stale
        return EdgeCost.between(from, to).extras();
    }

    /**
     * How walled in a cell is, 0 in open air and 1 boxed in on all 26 sides. Sums the blocked cells
     * touching it, each weighted by distance, so being cornered on several sides scores higher than
     * running alongside one flat surface. Memoized: neighbouring cells' shells overlap heavily and
     * every lookup goes through vanilla's per-search path type cache, so the marginal cost is far
     * below the nominal 26 queries per cell.
     * <p>
     * Measured independently of {@code wallHugCost}, which only decides whether the search is
     * <i>charged</i> for it. The throttle planner reads the raw number to work out how much room a
     * corner has to be swung wide into, and that has to keep working with the search bias turned
     * off. {@code measureClearance} is the switch that disables the measurement itself.
     */
    private float enclosure(int x, int y, int z) {
        if (!BirdPathfindingConfig.measureClearance) {
            return 0;
        }
        long key = this.packKey(x, y, z, 0);
        float cached = this.enclosures.get(key);
        if (cached != NOT_MEASURED) {
            return cached;
        }
        float blocked = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    int axes = Math.abs(dx) + Math.abs(dy) + Math.abs(dz);
                    if (axes == 0 || this.isClear(x + dx, y + dy, z + dz)) {
                        continue;
                    }
                    blocked += axes == 1 ? 1.0F : axes == 2 ? EDGE_WEIGHT : CORNER_WEIGHT;
                }
            }
        }
        float measured = blocked / FULLY_ENCLOSED_WEIGHT;
        this.enclosures.put(key, measured);
        return measured;
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
        // rides on the node so the throttle planner and the renderer can read it off the finished
        // path. The search itself charges for it in getEdgeCost, not from here
        node.enclosure = this.enclosure(x, y, z);
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

    // the 3 heading bits below are the one place HEADING_BINS is not read from the constant: raising
    // it past 8 needs a wider field here (and one fewer bit of coordinate range)
    private long packKey(int x, int y, int z, int heading) {
        long key = 0;
        key |= ((long) (x - originX) & 0x1FFF);          // 13 signed bits, +-4096
        key |= ((long) (y - originY) & 0x3FF) << 13;     // 10 signed bits
        key |= ((long) (z - originZ) & 0x1FFF) << 23;    // 13 signed bits
        key |= ((long) heading & 0x7) << 36;
        return key;
    }

    // how many 45 degree bins apart two headings are, 0..HEADING_BINS/2
    static int turnAmount(int headingA, int headingB) {
        int diff = Math.abs(headingA - headingB);
        return diff > HEADING_BINS / 2 ? HEADING_BINS - diff : diff;
    }

    // mc yaw convention: 0 faces +Z (south), 90 faces -X (west)
    static int yawToBin(float yRot) {
        return Math.floorMod(Math.round((yRot + 90.0F) / (360.0F / HEADING_BINS)), HEADING_BINS);
    }
}
