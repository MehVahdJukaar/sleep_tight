package net.mehvahdjukaar.sleep_tight.test.pathfinding;

import it.unimi.dsi.fastutil.longs.Long2FloatMap;
import it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.mehvahdjukaar.sleep_tight.test.controller.PerchingFlier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.pathfinder.FlyNodeEvaluator;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.PathType;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

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
    private Vec3i origin;
    // whether the mob had its feet down when the search began, see getStart
    private boolean startsGrounded;

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
        this.origin = mob.blockPosition();
        this.startsGrounded = mob instanceof PerchingFlier flier && flier.isGrounded();
        this.expansions = 0;
        this.generatedNeighbors = 0;
    }

    @Override
    public void done() {
        this.latticeNodes.clear();
        this.enclosures.clear();
        super.done();
    }

    /**
     * Vanilla picks a safe start cell (water surface, bounding box candidates); we only swap the node
     * for a lattice one carrying the heading the mob leaves along.
     * <p>
     * A bird in the air leaves along its body yaw, because that is where its airspeed points and
     * momentum is the whole reason this lattice exists. A bird with its feet down has no airspeed at
     * all, so it leaves whichever way the route wants and pays nothing for it: the start is marked
     * {@link BirdNode#freeHeading}, which lifts both the turn cap and the turn charge on the first
     * step. Making that true on the mob is the ground layer's job, see {@code BirdGaitControl}.
     * <p>
     * Seeding the heading from yaw regardless was a real bug: a perched bird facing away from the
     * only way out of a dead end had no legal horizontal move at all, since the two purely vertical
     * moves are the only ones exempt from the cap, and the search would climb straight out of a
     * corridor it could have walked down.
     */
    @Override
    public Node getStart() {
        Node vanillaStart = super.getStart();
        BirdNode start = this.getLatticeNode(vanillaStart.x, vanillaStart.y, vanillaStart.z,
                yawToBin(this.mob.getYRot()), this.startsGrounded);
        start.type = vanillaStart.type;
        start.costMalus = vanillaStart.costMalus;
        return start;
    }

    // targets stay plain vanilla nodes: reaching is checked by position only

    @Override
    public int getNeighbors(Node[] outputArray, Node node) {
        int count = this.collectMoves(outputArray, node, true);
        this.expansions++;
        this.generatedNeighbors += count;
        return count;
    }

    /**
     * Every move the lattice offers at a node, for the debug renderer to draw as the alternatives
     * the search weighed against the one it took. Unlike {@link #getNeighbors} this keeps states
     * that are already closed, since by the time a path exists most of what it turned down is, and
     * it does not count towards the search cost figures.
     * <p>
     * Only valid between {@code prepare} and {@code done}: it reads the same cell caches the search
     * does.
     */
    public List<Node> offeredMoves(Node node) {
        Node[] buffer = new Node[MOVES.length];
        return List.of(Arrays.copyOf(buffer, this.collectMoves(buffer, node, false)));
    }

    private int collectMoves(Node[] outputArray, Node node, boolean skipClosed) {
        int count = 0;
        int heading = node instanceof BirdNode bird ? bird.heading : yawToBin(this.mob.getYRot());
        boolean free = node instanceof BirdNode bird && bird.freeHeading;

        for (int[] move : MOVES) {
            int moveBin = move[3];
            if (!free && moveBin >= 0 && turnAmount(heading, moveBin) > BirdPathfindingConfig.maxTurnBins) {
                continue;
            }
            int newHeading = moveBin < 0 ? heading : moveBin;
            // a vertical hop off a perch stays free: it buys no airspeed to conserve, and the hover
            // it does cost is already charged as straightUpCost. So a 1-wide shaft can be climbed
            // and then left in any direction, rather than in whichever one the mob happened to face
            boolean stillFree = free && moveBin < 0;

            BirdNode neighbor = this.findAcceptedLatticeNode(
                    node.x + move[0], node.y + move[1], node.z + move[2], newHeading, stillFree);
            if (neighbor == null || (skipClosed && neighbor.closed)) {
                continue;
            }
            if (!this.hasClearance(node, move[0], move[1], move[2])) {
                continue;
            }
            outputArray[count++] = neighbor;
        }
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
        long key = this.packKey(x, y, z, 0, false);
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

    /**
     * Assigned rather than accumulated, unlike vanilla's {@code findAcceptedNode}, which does
     * {@code costMalus = max(costMalus, malus)} and then {@code ++} for WALKABLE on a node it keeps
     * cached for the whole search. Both terms here are pure functions of the cell, so the max is a
     * no-op and the increment is a bug: a cell is offered once per heading bin per parent, and every
     * one of those re-offers used to add another point of malus. A ground level cell the search
     * happened to brush past several times ended up several times more expensive than the identical
     * cell next to it, which is order dependence rather than terrain.
     */
    @Nullable
    private BirdNode findAcceptedLatticeNode(int x, int y, int z, int heading, boolean freeHeading) {
        PathType type = this.getCachedPathType(x, y, z);
        float malus = this.mob.getPathfindingMalus(type);
        if (malus < 0.0F) {
            return null;
        }
        BirdNode node = this.getLatticeNode(x, y, z, heading, freeHeading);
        node.type = type;
        // rides on the node so the throttle planner and the renderer can read it off the finished
        // path. The search itself charges for it in getEdgeCost, not from here
        node.enclosure = this.enclosure(x, y, z);
        // the +1 is vanilla's air preference: given the choice, fly rather than skim the ground.
        // Half of the altitude bias, the other half being the wall hug charge on the same cells,
        // which see for the arithmetic and for why the two are meant to stack
        node.costMalus = type == PathType.WALKABLE ? malus + 1.0F : malus;
        return node;
    }

    private BirdNode getLatticeNode(int x, int y, int z, int heading, boolean freeHeading) {
        return this.latticeNodes.computeIfAbsent(this.packKey(x, y, z, heading, freeHeading),
                key -> new BirdNode(x, y, z, heading, freeHeading));
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
    private long packKey(int x, int y, int z, int heading, boolean freeHeading) {
        long key = 0;
        key |= ((long) (x - origin.getX()) & 0x1FFF);          // 13 signed bits, +-4096
        key |= ((long) (y - origin.getY()) & 0x3FF) << 13;     // 10 signed bits
        key |= ((long) (z - origin.getZ()) & 0x1FFF) << 23;    // 13 signed bits
        key |= ((long) heading & 0x7) << 36;
        key |= (freeHeading ? 1L : 0L) << 39;            // free states are their own layer
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
