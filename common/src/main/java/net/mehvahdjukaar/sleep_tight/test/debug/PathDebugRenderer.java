package net.mehvahdjukaar.sleep_tight.test.debug;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.mehvahdjukaar.sleep_tight.test.pathfinding.BirdPathfindingConfig;
import net.minecraft.Util;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

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
    public static float textScale = 0.02F;

    private final Map<Integer, Entry> paths = new HashMap<>();

    public void addPath(int entityId, DebugPath path, float nodeHalfWidth, MobDebugInfo mobInfo) {
        // diffed against whatever was here before, so the render side can show real progress made
        // per real second instead of just the absolute cursor position
        Entry previous = this.paths.get(entityId);
        long now = Util.getMillis();
        double cursorDelta = previous != null ? mobInfo.rulerCursor() - previous.mobInfo().rulerCursor() : 0.0;
        long deltaMillis = previous != null ? now - previous.creationTime() : 0L;
        this.paths.put(entityId, new Entry(path, nodeHalfWidth, mobInfo, now, cursorDelta, deltaMillis));
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

        List<DebugNode> nodes = path.nodes();
        for (int i = 0; i < nodes.size(); i++) {
            DebugNode node = nodes.get(i);
            if (isTooFar(node, camX, camY, camZ)) continue;
            // stack the boxes slightly so overlapping nodes stay distinguishable
            float yOffset = 0.01F * i;
            boolean isNext = i == path.nextNodeIndex();
            renderBox(poseStack, bufferSource, new AABB(
                            node.x() + 0.5F - nodeHalfWidth, node.y() + yOffset, node.z() + 0.5F - nodeHalfWidth,
                            node.x() + 0.5F + nodeHalfWidth, node.y() + 0.25F + yOffset, node.z() + 0.5F + nodeHalfWidth),
                    isNext ? 1 : 0, 0, isNext ? 0 : 1, camX, camY, camZ);
        }

        // the pale tiles covering everything the search touched. Empty unless
        // BirdPathfindingConfig#collectDebugData is turned on, which is the knob for them
        renderNodeSet(poseStack, bufferSource, path.closedSet(), nodeHalfWidth, 1, 0.8F, 0.8F, camX, camY, camZ);
        renderNodeSet(poseStack, bufferSource, path.openSet(), nodeHalfWidth, 0.8F, 1, 1, camX, camY, camZ);

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
            renderClearanceSummary(poseStack, bufferSource, path, camX, camY, camZ);
        }
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
     * current wanted position since that is roughly where the mob itself is (the lookahead is
     * short), rather than at a fixed offset from a node that may be far behind or ahead of it.
     */
    private static void renderMobInfo(PoseStack poseStack, MultiBufferSource bufferSource, Entry entry,
                                      double camX, double camY, double camZ) {
        MobDebugInfo info = entry.mobInfo();
        Vec3 pos = info.wantedPos();
        if (distanceToCamera((int) pos.x, (int) pos.y, (int) pos.z, camX, camY, camZ) > maxRenderDistance) return;

        // magenta normally, flips to red when the navigation itself has given up
        renderBox(poseStack, bufferSource, new AABB(pos.x - 0.1, pos.y - 0.1, pos.z - 0.1,
                        pos.x + 0.1, pos.y + 0.1, pos.z + 0.1),
                1, info.stuck() ? 0 : 0.2F, 1, camX, camY, camZ);

        double progress = info.rulerLength() > 1.0E-4 ? info.rulerCursor() / info.rulerLength() * 100.0 : 0.0;
        int textColor = info.stuck() ? 0xFFFF5555 : -1;

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
        DebugRenderHelper.renderFloatingText(poseStack, bufferSource,
                String.format(Locale.ROOT, "v=%.2f", info.velocity().length()),
                pos.x, pos.y + 0.5, pos.z, -1, textScale, true, true);

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

    private static boolean isTooFar(DebugNode node, double camX, double camY, double camZ) {
        return distanceToCamera(node.x(), node.y(), node.z(), camX, camY, camZ) > maxRenderDistance;
    }

    // manhattan, like vanilla: it is only a culling heuristic, no need for a square root per node
    private static float distanceToCamera(int x, int y, int z, double camX, double camY, double camZ) {
        return (float) (Math.abs(x - camX) + Math.abs(y - camY) + Math.abs(z - camZ));
    }

    private record Entry(DebugPath path, float nodeHalfWidth, MobDebugInfo mobInfo, long creationTime,
                         double cursorDelta, long deltaMillis) {
    }
}
