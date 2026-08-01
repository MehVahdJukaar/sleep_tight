package net.mehvahdjukaar.sleep_tight.test.debug;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.mehvahdjukaar.sleep_tight.test.pathfinding.BirdPathfindingConfig;
import net.mehvahdjukaar.sleep_tight.test.pathfinding.EdgeCost;
import net.minecraft.Util;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Our copy of vanilla's PathfindingRenderer, driven by {@link ClientBoundPathDebugMessage} instead
 * of the vanilla debug channel. Entries expire on their own, so the server has to keep resending a
 * path for it to stay on screen.
 */
public class PathDebugRenderer {

    public static final PathDebugRenderer INSTANCE = new PathDebugRenderer();

    // toggles vanilla keeps as compile time constants. Ours are plain fields so they can be
    // flipped from the debugger or from code without a command
    public static long timeoutMillis = 60_000;
    public static float maxRenderDistance = 80;
    public static boolean showNodeLabels = true;
    // every label in here goes through this one scale, so it is the knob for the whole overlay
    // getting too busy. Small enough that a node's three labels and the two edges meeting at it
    // stay apart, which is what crowds first
    public static float textScale = 0.007F;
    // node boxes are sized off the mob's own width, which at a lattice node spacing of one block
    // leaves barely any air between them. Shrunk so the path reads as a line of markers rather than
    // a solid tube, and so the labels sitting on them stay legible
    public static float nodeBoxScale = 0.45F;

    // the throttle profile overlay: an arrow per node along the direction of travel, as long as the
    // speed allowed there. Off makes the path read as pure geometry again
    public static boolean showSpeedArrows = true;
    // blocks of arrow per block-per-tick of speed. A bird at full throttle does about 0.2 b/t, so
    // this puts its arrow at roughly a block and a half
    public static double speedArrowScale = 7.0;
    // arrows for what the mob is actually doing: where it is pointing versus where it is going.
    // The gap between the two is the sideslip that makes a turning bird look like a crabbing drone
    public static boolean showMobVectors = true;
    // breadcrumbs of where the mob has actually been, one per tick. The gap between this and the
    // path's own line is everything the steering layer adds on top of the plan: corner cutting,
    // overshoot, the wobble of rejoining the line after being pushed off it
    public static boolean showTrail = true;
    // what each step cost the search, drawn on the step itself rather than on a node: the terms
    // that decide a lattice path are all properties of the move, not of the cell it lands in
    public static boolean showEdgeCosts = true;

    private static final int FACING_COLOR = 0xFFFF55;
    private static final int VELOCITY_COLOR = 0x55FFFF;
    // orange, so it stays apart from the path's green-to-red speed ramp
    private static final float TRAIL_HUE = 0.08F;

    private final Map<Integer, Entry> paths = new HashMap<>();

    public void addPath(int entityId, DebugPath path, float nodeHalfWidth, MobDebugInfo mobInfo) {
        // diffed against whatever was here before, so the render side can show real progress made
        // per real second instead of just the absolute cursor position
        Entry previous = this.paths.get(entityId);
        long now = Util.getMillis();
        double cursorDelta = previous != null ? mobInfo.rulerCursor() - previous.mobInfo().rulerCursor() : 0.0;
        long deltaMillis = previous != null ? now - previous.creationTime() : 0L;
        this.paths.put(entityId, new Entry(path, nodeHalfWidth, mobInfo, now, cursorDelta, deltaMillis,
                stitchTrail(previous, mobInfo)));
    }

    /**
     * The packet only carries the samples taken since the last one, so the flown line is built up
     * here. Nothing trims it: it lives exactly as long as the entry it hangs off, so a path still
     * on screen always shows the whole flight that produced it. A mob that has been given a new
     * path arrives with a new epoch, which is what says the history so far is over and can go.
     */
    private static List<Vec3> stitchTrail(@Nullable Entry previous, MobDebugInfo mobInfo) {
        List<Vec3> trail = previous != null && previous.mobInfo().trailEpoch() == mobInfo.trailEpoch()
                ? previous.trail() : new ArrayList<>();
        trail.addAll(mobInfo.trailSamples());
        return trail;
    }

    public void clear() {
        this.paths.clear();
    }

    public void render(PoseStack poseStack, MultiBufferSource bufferSource, double camX, double camY, double camZ) {
        if (this.paths.isEmpty()) return;

        long now = Util.getMillis();
        this.paths.values().removeIf(entry -> now - entry.creationTime > timeoutMillis);
        for (Entry entry : this.paths.values()) {
            renderPath(poseStack, bufferSource, entry.path, entry.nodeHalfWidth, showNodeLabels, camX, camY, camZ);
            if (showTrail) {
                renderTrail(poseStack, bufferSource, entry.trail(), camX, camY, camZ);
            }
            renderMobInfo(poseStack, bufferSource, entry, camX, camY, camZ);
        }
    }

    public static void renderPath(PoseStack poseStack, MultiBufferSource bufferSource, DebugPath path,
                                  float nodeHalfWidth, boolean showLabels,
                                  double camX, double camY, double camZ) {
        renderPathLine(poseStack, bufferSource.getBuffer(RenderType.debugLineStrip(6)), path, camX, camY, camZ);

        BlockPos target = path.target();
        // vanilla hides the whole path when the target is out of range; each piece culls on its own here
        if (distanceToCamera(target.getX(), target.getY(), target.getZ(), camX, camY, camZ) <= maxRenderDistance) {
            // green when the search actually got there, yellow when this is only a closest approach
            renderBox(poseStack, bufferSource, new AABB(
                            target.getX() + 0.25, target.getY() + 0.25, target.getZ() + 0.25,
                            target.getX() + 0.75, target.getY() + 0.75, target.getZ() + 0.75),
                    path.reached() ? 0 : 1, 1, 0, camX, camY, camZ);
        }

        float halfWidth = nodeHalfWidth * nodeBoxScale;
        List<DebugNode> nodes = path.nodes();
        for (int i = 0; i < nodes.size(); i++) {
            DebugNode node = nodes.get(i);
            if (isTooFar(node, camX, camY, camZ)) continue;
            // stack the boxes slightly so overlapping nodes stay distinguishable
            float yOffset = 0.01F * i;
            boolean isNext = i == path.nextNodeIndex();
            renderBox(poseStack, bufferSource, new AABB(
                            node.x() + 0.5F - halfWidth, node.y() + yOffset, node.z() + 0.5F - halfWidth,
                            node.x() + 0.5F + halfWidth, node.y() + 0.12F + yOffset, node.z() + 0.5F + halfWidth),
                    isNext ? 1 : 0, 0, isNext ? 0 : 1, camX, camY, camZ);
        }

        // the pale tiles covering everything the search touched. Empty unless
        // BirdPathfindingConfig#collectDebugData is turned on, which is the knob for them
        renderNodeSet(poseStack, bufferSource, path.closedSet(), halfWidth, 1, 0.8F, 0.8F, camX, camY, camZ);
        renderNodeSet(poseStack, bufferSource, path.openSet(), halfWidth, 0.8F, 1, 1, camX, camY, camZ);

        if (showSpeedArrows) {
            renderSpeedArrows(poseStack, bufferSource, path, camX, camY, camZ);
        }

        if (showEdgeCosts) {
            renderEdgeCosts(poseStack, bufferSource, path, camX, camY, camZ);
            renderCostSummary(poseStack, bufferSource, path, camX, camY, camZ);
        }

        if (showLabels) {
            for (DebugNode node : nodes) {
                if (isTooFar(node, camX, camY, camZ)) continue;
                renderLabel(poseStack, bufferSource, String.valueOf(node.type()), node, 0.75, -1);
                renderLabel(poseStack, bufferSource, String.format(Locale.ROOT, "%.2f", node.costMalus()), node, 0.25, -1);
                // what the cell paid for being boxed in. Skipped when it paid nothing, which is the
                // normal case in open air and would otherwise put a "0.00" over every node
                if (node.clearanceCost() > 0.005F) {
                    renderLabel(poseStack, bufferSource,
                            String.format(Locale.ROOT, "hug %.2f", node.clearanceCost()),
                            node, 0.5, clearanceTextColor(node.clearanceCost()));
                }
            }
            //renderClearanceSummary(poseStack, bufferSource, path, camX, camY, camZ);
            renderThrottleSummary(poseStack, bufferSource, path, camX, camY, camZ);
        }
    }

    /**
     * The throttle profile, drawn as one arrow per node pointing the way the mob will be travelling
     * there and as long as the speed it is allowed to be doing. A path that is all long green arrows
     * is flyable flat out; arrows shrinking and reddening into a corner is the profile braking for
     * it, which is the whole point of the throttle layer and is otherwise invisible.
     * <p>
     * Nothing is drawn for a path with no profile, which is what a plain vanilla path looks like.
     */
    private static void renderSpeedArrows(PoseStack poseStack, MultiBufferSource bufferSource, DebugPath path,
                                          double camX, double camY, double camZ) {
        List<DebugNode> nodes = path.nodes();
        float maxSpeed = path.envelopeMaxSpeed();
        for (int i = 0; i < nodes.size(); i++) {
            DebugNode node = nodes.get(i);
            if (!node.hasSpeedLimit() || isTooFar(node, camX, camY, camZ)) continue;

            // the leg leaving this node, or the one arriving at it for the last node
            DebugNode from = i < nodes.size() - 1 ? node : nodes.get(Math.max(0, i - 1));
            DebugNode to = i < nodes.size() - 1 ? nodes.get(i + 1) : node;
            Vec3 direction = new Vec3(to.x() - from.x(), to.y() - from.y(), to.z() - from.z());
            if (direction.lengthSqr() < 1.0E-8) continue;

            Vec3 base = new Vec3(node.x() + 0.5 - camX, node.y() + 0.5 - camY, node.z() + 0.5 - camZ);
            double length = node.speedLimit() * speedArrowScale;
            Vec3 tip = base.add(direction.normalize().scale(length));
            float fraction = maxSpeed > 1.0E-5F ? Mth.clamp(node.speedLimit() / maxSpeed, 0, 1) : 1;
            DebugRenderHelper.renderArrow(poseStack, bufferSource, base, tip,
                    Math.min(0.2, length * 0.35), Mth.hsvToRgb(fraction * 0.33F, 0.9F, 1.0F));
        }
    }

    /**
     * What each step of the path cost the search, written on the step itself: the total first, then
     * only the terms that actually charged something. A run of bare totals is a path the geometry
     * alone decided; a "turn 5.0" in the middle of one is the search having paid for a corner it
     * could have avoided, which is the thing to look at before touching the turn knobs.
     */
    private static void renderEdgeCosts(PoseStack poseStack, MultiBufferSource bufferSource, DebugPath path,
                                        double camX, double camY, double camZ) {
        List<DebugNode> nodes = path.nodes();
        for (int i = 0; i < nodes.size() - 1; i++) {
            DebugNode node = nodes.get(i);
            if (isTooFar(node, camX, camY, camZ)) continue;
            DebugNode next = nodes.get(i + 1);
            EdgeCost cost = node.edgeCost();
            DebugRenderHelper.renderFloatingText(poseStack, bufferSource, edgeCostLabel(cost),
                    (node.x() + next.x()) / 2.0 + 0.5, (node.y() + next.y()) / 2.0 + 0.5,
                    (node.z() + next.z()) / 2.0 + 0.5, edgeCostColor(cost), textScale, true, true);
        }
    }

    /**
     * Written as the sum it is, {@code 7.0 = dist 1.4 + turn 5.0 + hug 0.6}, so there is no reading
     * where the terms are charges on top of the total instead of what makes it up. A step that only
     * paid for its own length is just the one number.
     */
    private static String edgeCostLabel(EdgeCost cost) {
        if (cost.extras() <= 0.005F && cost.malus() <= 0.005F) {
            return String.format(Locale.ROOT, "%.1f", cost.total());
        }
        StringBuilder label = new StringBuilder(String.format(Locale.ROOT, "%.1f = dist %.1f",
                cost.total(), cost.distance()));
        appendTerm(label, "turn", cost.turn());
        appendTerm(label, "vert", cost.vertical());
        appendTerm(label, "hug", cost.clearance());
        appendTerm(label, "malus", cost.malus());
        return label.toString();
    }

    private static void appendTerm(StringBuilder label, String name, float value) {
        if (value > 0.005F) {
            label.append(String.format(Locale.ROOT, " + %s %.1f", name, value));
        }
    }

    /** How much of the step was the lattice's doing rather than plain distance. */
    private static int edgeCostColor(EdgeCost cost) {
        float share = cost.total() > 1.0E-4F ? cost.extras() / cost.total() : 0;
        if (share > 0.6F) return 0xFFFF5555;
        return share > 0.3F ? 0xFFFFAA55 : 0xFFAAFFAA;
    }

    /**
     * The same split totalled over the whole route, which is what says whether a knob is worth
     * moving: a turn total that dwarfs the distance means the search is buying smoothness at a
     * price the flight cannot repay, and a total near the distance means the knobs are barely
     * biting and the path is whatever the geometry gave it.
     */
    private static void renderCostSummary(PoseStack poseStack, MultiBufferSource bufferSource, DebugPath path,
                                          double camX, double camY, double camZ) {
        BlockPos target = path.target();
        if (distanceToCamera(target.getX(), target.getY(), target.getZ(), camX, camY, camZ) > maxRenderDistance) {
            return;
        }
        EdgeCost total = EdgeCost.NONE;
        for (DebugNode node : path.nodes()) {
            total = total.plus(node.edgeCost());
        }
        DebugRenderHelper.renderFloatingText(poseStack, bufferSource, String.format(Locale.ROOT,
                        "cost %.1f = dist %.1f + turn %.1f + vert %.1f + hug %.1f + malus %.1f",
                        total.total(), total.distance(), total.turn(), total.vertical(),
                        total.clearance(), total.malus()),
                target.getX() + 0.5, target.getY() + 1.8, target.getZ() + 0.5, -1, textScale, true, true);
    }

    /**
     * What the throttle layer did to this path overall. The two numbers to watch: how many nodes are
     * actually being held below top speed (zero means the profile is not biting and the corners are
     * all flyable flat out) and how long the flight is expected to take, which is the budget a
     * flier-appropriate path timeout should be using instead of vanilla's cruise-speed guess.
     */
    private static void renderThrottleSummary(PoseStack poseStack, MultiBufferSource bufferSource, DebugPath path,
                                              double camX, double camY, double camZ) {
        BlockPos target = path.target();
        if (distanceToCamera(target.getX(), target.getY(), target.getZ(), camX, camY, camZ) > maxRenderDistance) {
            return;
        }
        float slowest = Float.MAX_VALUE;
        float fastest = 0;
        int limited = 0;
        for (DebugNode node : path.nodes()) {
            if (!node.hasSpeedLimit()) continue;
            slowest = Math.min(slowest, node.speedLimit());
            fastest = Math.max(fastest, node.speedLimit());
            if (node.speedLimit() < path.envelopeMaxSpeed() * 0.95F) {
                limited++;
            }
        }
        if (fastest <= 0) {
            return;
        }
        DebugRenderHelper.renderFloatingText(poseStack, bufferSource, String.format(Locale.ROOT,
                        "throttle %.3f-%.3f b/t of %.3f, %d/%d limited, eta %.0ft",
                        slowest, fastest, path.envelopeMaxSpeed(), limited, path.nodes().size(),
                        path.expectedFlightTicks()),
                target.getX() + 0.5, target.getY() + 1.5, target.getZ() + 0.5, -1, textScale, true, true);
    }

    /**
     * What the whole route paid for flying close to things, next to the knobs that produced it.
     * This is the number to watch when retuning: near zero means the term is not biting at all,
     * while a total that rivals the path's own length means the bird is detouring more than the
     * smoother line is worth.
     */
    private static void renderClearanceSummary(PoseStack poseStack, MultiBufferSource bufferSource, DebugPath path,
                                               double camX, double camY, double camZ) {
        BlockPos target = path.target();
        if (distanceToCamera(target.getX(), target.getY(), target.getZ(), camX, camY, camZ) > maxRenderDistance) {
            return;
        }
        float total = 0;
        int charged = 0;
        for (DebugNode node : path.nodes()) {
            total += node.clearanceCost();
            if (node.clearanceCost() > 0.005F) {
                charged++;
            }
        }
        DebugRenderHelper.renderFloatingText(poseStack, bufferSource, String.format(Locale.ROOT,
                        "hug total %.1f over %d/%d nodes (wallHugCost %.1f, h weight %.1f)",
                        total, charged, path.nodes().size(),
                        BirdPathfindingConfig.wallHugCost, BirdPathfindingConfig.heuristicWeight),
                target.getX() + 0.5, target.getY() + 1.2, target.getZ() + 0.5, -1, textScale, true, true);
    }

    /**
     * Wall hug charge as a traffic light against the configured maximum, so a glance says whether
     * the bird is merely near geometry or genuinely boxed in.
     */
    private static int clearanceTextColor(float clearanceCost) {
        float fraction = clearanceFraction(clearanceCost);
        if (fraction > 0.5F) return 0xFFFF5555;
        return fraction > 0.25F ? 0xFFFFAA55 : 0xFFFFFF55;
    }

    /**
     * Charge as a share of what a fully boxed-in cell would pay. Reads the live config so the scale
     * follows retuning; falls back to the raw value if clearance has been switched off since the
     * path was recorded.
     */
    private static float clearanceFraction(float clearanceCost) {
        float wallHugCost = BirdPathfindingConfig.wallHugCost;
        return Mth.clamp(wallHugCost > 0 ? clearanceCost / wallHugCost : clearanceCost, 0, 1);
    }

    /**
     * Flat tiles at ground level, so the searched area reads as a heat map under the path itself.
     * The tile's own colour says which set it came from; how walled in the cell is drains the green
     * and blue out of it, so the wall hug field shows up as red staining hugging the geometry. With
     * clearance off every tile keeps its plain set colour.
     */
    private static void renderNodeSet(PoseStack poseStack, MultiBufferSource bufferSource, List<DebugNode> nodes,
                                      float nodeHalfWidth, float red, float green, float blue,
                                      double camX, double camY, double camZ) {
        float halfWidth = nodeHalfWidth / 2;
        for (DebugNode node : nodes) {
            if (isTooFar(node, camX, camY, camZ)) continue;
            float openness = 1 - clearanceFraction(node.clearanceCost());
            renderBox(poseStack, bufferSource, new AABB(
                            node.x() + 0.5F - halfWidth, node.y() + 0.01F, node.z() + 0.5F - halfWidth,
                            node.x() + 0.5F + halfWidth, node.y() + 0.1, node.z() + 0.5F + halfWidth),
                    red, green * openness, blue * openness, camX, camY, camZ);
        }
    }

    /** Line strip through every node, hue ramped along the path so direction of travel is readable. */
    public static void renderPathLine(PoseStack poseStack, VertexConsumer consumer, DebugPath path,
                                      double camX, double camY, double camZ) {
        List<DebugNode> nodes = path.nodes();
        for (int i = 0; i < nodes.size(); i++) {
            DebugNode node = nodes.get(i);
            if (isTooFar(node, camX, camY, camZ)) continue;
            int color = i == 0 ? 0 : Mth.hsvToRgb((float) i / nodes.size() * 0.33F, 0.9F, 0.9F);
            consumer.addVertex(poseStack.last(),
                            (float) (node.x() - camX + 0.5), (float) (node.y() - camY + 0.5), (float) (node.z() - camZ + 0.5))
                    .setColor(color >> 16 & 255, color >> 8 & 255, color & 255, 255);
        }
    }

    private static void renderBox(PoseStack poseStack, MultiBufferSource bufferSource, AABB box,
                                  float red, float green, float blue, double camX, double camY, double camZ) {
        DebugRenderHelper.renderFilledBox(poseStack, bufferSource, box.move(-camX, -camY, -camZ), red, green, blue, 0.5F);
    }

    private static void renderLabel(PoseStack poseStack, MultiBufferSource bufferSource, String text,
                                    DebugNode node, double yOffset, int color) {
        DebugRenderHelper.renderFloatingText(poseStack, bufferSource, text,
                node.x() + 0.5, node.y() + yOffset, node.z() + 0.5, color, textScale, true, true);
    }

    /**
     * The "what is it thinking" half: navigation/steering state, drawn at the move control's
     * current wanted position since that is roughly where the mob itself is (the carrot is a block
     * or two out), rather than at a fixed offset from a node that may be far behind or ahead of it.
     */
    private static void renderMobInfo(PoseStack poseStack, MultiBufferSource bufferSource, Entry entry,
                                      double camX, double camY, double camZ) {
        MobDebugInfo info = entry.mobInfo();
        Vec3 pos = info.wantedPos();
        if (distanceToCamera(pos.x, pos.y, pos.z, camX, camY, camZ) > maxRenderDistance) return;

        if (showMobVectors) {
            renderMobVectors(poseStack, bufferSource, info, camX, camY, camZ);
        }

        // magenta normally, flips to red when the navigation itself has given up
        renderBox(poseStack, bufferSource, new AABB(pos.x - 0.1, pos.y - 0.1, pos.z - 0.1,
                        pos.x + 0.1, pos.y + 0.1, pos.z + 0.1),
                1, info.stuck() ? 0 : 0.2F, 1, camX, camY, camZ);

        double progress = info.rulerLength() > 1.0E-4 ? info.rulerCursor() / info.rulerLength() * 100.0 : 0.0;
        int textColor = info.stuck() ? 0xFFFF5555 : -1;
        poseStack.translate(0,1.4,0);
        // steering (hasWanted() && !isDone(), the exact condition BirdMoveControl branches on) is
        // what is actually happening; raw operation is kept as a footnote since MoveControl never
        // resets it back to WAIT here, same as vanilla's own SmoothSwimmingMoveControl
        String status = (info.stuck() ? "STUCK " : "") + (info.steering() ? "STEERING" : "COASTING")
                + (info.pathDone() ? " done" : "") + " (" + info.operation() + ")";
        DebugRenderHelper.renderFloatingText(poseStack, bufferSource, status,
                pos.x, pos.y + 1.0, pos.z, textColor, textScale, true, true);
        DebugRenderHelper.renderFloatingText(poseStack, bufferSource, String.format(Locale.ROOT,
                        "%.1f/%.1f (%.0f%%) node %d/%d", info.rulerCursor(), info.rulerLength(), progress,
                        info.nextNodeIndex(), info.nodeCount()),
                pos.x, pos.y + 0.75, pos.z, -1, textScale, true, true);
        // actual speed against what the profile allows underfoot and what the navigation actually
        // commanded. Over the profile's limit means the corner coming up is going to be cut wider
        // than the planner budgeted for; cmd below it is the braking or rejoin correction biting
        String throttleText = info.speedLimitCommanded() >= 0.0
                ? String.format(Locale.ROOT, "v=%.3f limit %.3f (cmd %.3f) off %.2f slip %.0fdeg",
                info.velocity().length(), info.speedLimitNow(), info.speedLimitCommanded(),
                info.offRoute(), info.sideslipDegrees())
                : String.format(Locale.ROOT, "v=%.3f off %.2f slip %.0fdeg",
                info.velocity().length(), info.offRoute(), info.sideslipDegrees());
        int throttleColor = info.speedLimitNow() >= 0.0
                && info.velocity().length() > info.speedLimitNow() * 1.1 ? 0xFFFF5555 : -1;
        DebugRenderHelper.renderFloatingText(poseStack, bufferSource, throttleText,
                pos.x, pos.y + 0.5, pos.z, throttleColor, textScale, true, true);

        // the two vanilla watchdogs that can null the path out without a goal ever asking for it.
        // Watch these climb to catch a stall as it happens instead of reasoning back from a dead path
        double budget = info.timeoutBudget();
        double timeoutRatio = budget > 1.0E-4 ? info.timeoutTimer() / budget : 0.0;
        int timeoutColor = timeoutRatio > 0.8 ? 0xFFFF5555 : timeoutRatio > 0.5 ? 0xFFFFFF55 : -1;
        DebugRenderHelper.renderFloatingText(poseStack, bufferSource, String.format(Locale.ROOT,
                        "timeout %d/%.0f (%.0f%%) stuckChk %d/100", info.timeoutTimer(), budget,
                        timeoutRatio * 100.0, info.ticksSinceStuckCheck()),
                pos.x, pos.y + 0.25, pos.z, timeoutColor, textScale, true, true);

        // the ruler cursor's real-world progress rate. This is the one that catches a carrot-chase
        // deadlock directly: if this reads ~0 while the mob is still "steering" and not yet flagged
        // by either watchdog above, the cursor has stopped advancing even though nothing gave up yet
        if (entry.deltaMillis() > 0) {
            double blocksPerSecond = entry.cursorDelta() / (entry.deltaMillis() / 1000.0);
            int rateColor = Math.abs(blocksPerSecond) < 0.05 && info.steering() ? 0xFFFF5555 : -1;
            DebugRenderHelper.renderFloatingText(poseStack, bufferSource, String.format(Locale.ROOT,
                            "cursor %+.2fb / %dms (%.2f b/s)", entry.cursorDelta(), entry.deltaMillis(),
                            blocksPerSecond),
                    pos.x, pos.y, pos.z, rateColor, textScale, true, true);
        }
    }

    /**
     * Two arrows from the mob itself: yellow for where the body is pointing, cyan for where it is
     * actually going, scaled the same way the path's speed arrows are so the two can be compared by
     * eye. Thrust is only ever applied along the yellow one, so the angle between them is the
     * sideslip, and a cyan arrow noticeably shorter than the nearest path arrow is the mob failing
     * to keep up with its own profile.
     */
    private static void renderMobVectors(PoseStack poseStack, MultiBufferSource bufferSource, MobDebugInfo info,
                                         double camX, double camY, double camZ) {
        Vec3 origin = info.mobPos().subtract(camX, camY, camZ);
        Vec3 velocity = info.velocity();
        DebugRenderHelper.renderArrow(poseStack, bufferSource, origin,
                origin.add(info.facing().scale(0.75)), 0.15, FACING_COLOR);
        if (velocity.lengthSqr() > 1.0E-8) {
            Vec3 tip = origin.add(velocity.scale(speedArrowScale));
            DebugRenderHelper.renderArrow(poseStack, bufferSource, origin, tip,
                    Math.min(0.2, velocity.length() * speedArrowScale * 0.35), VELOCITY_COLOR);
        }
    }

    /**
     * The line actually flown, one segment per tick, drawn dim to bright with age so the direction
     * of travel and the most recent stretch read without a legend. Accumulated across packets, so
     * this is the whole flight rather than a window of it.
     */
    private static void renderTrail(PoseStack poseStack, MultiBufferSource bufferSource, List<Vec3> trail,
                                    double camX, double camY, double camZ) {
        for (int i = 1; i < trail.size(); i++) {
            Vec3 from = trail.get(i - 1);
            Vec3 to = trail.get(i);
            if (distanceToCamera(from.x, from.y, from.z, camX, camY, camZ) > maxRenderDistance) continue;
            float freshness = (float) i / trail.size();
            DebugRenderHelper.renderLine(poseStack, bufferSource,
                    from.subtract(camX, camY, camZ), to.subtract(camX, camY, camZ),
                    Mth.hsvToRgb(TRAIL_HUE, 0.9F, 0.35F + 0.65F * freshness));
        }
    }

    private static boolean isTooFar(DebugNode node, double camX, double camY, double camZ) {
        return distanceToCamera(node.x(), node.y(), node.z(), camX, camY, camZ) > maxRenderDistance;
    }

    // manhattan, like vanilla: it is only a culling heuristic, no need for a square root per node
    private static float distanceToCamera(double x, double y, double z, double camX, double camY, double camZ) {
        return (float) (Math.abs(x - camX) + Math.abs(y - camY) + Math.abs(z - camZ));
    }

    private record Entry(DebugPath path, float nodeHalfWidth, MobDebugInfo mobInfo, long creationTime,
                         double cursorDelta, long deltaMillis, List<Vec3> trail) {
    }
}
