package net.mehvahdjukaar.sleep_tight.test.debug;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The drawing primitives vanilla's DebugRenderer exposes as statics, cloned so our debug renderers
 * don't reach into a client class other mods routinely mixin into.
 */
public class DebugRenderHelper {

    /** Box coordinates are camera relative, same as vanilla. */
    public static void renderFilledBox(PoseStack poseStack, MultiBufferSource bufferSource, AABB box,
                                       float red, float green, float blue, float alpha) {
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.debugFilledBox());
        LevelRenderer.addChainedFilledBoxVertices(poseStack, consumer,
                box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, red, green, blue, alpha);
    }

    /**
     * A single camera-relative segment. Separate from the path's line strip on purpose: a strip
     * joins everything handed to it, so anything made of disconnected pieces (arrows) needs this.
     */
    public static void renderLine(PoseStack poseStack, MultiBufferSource bufferSource,
                                  Vec3 from, Vec3 to, int color) {
        Vec3 along = to.subtract(from);
        if (along.lengthSqr() < 1.0E-8) return;
        Vec3 normal = along.normalize();
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lines());
        PoseStack.Pose pose = poseStack.last();
        int red = color >> 16 & 255;
        int green = color >> 8 & 255;
        int blue = color & 255;
        consumer.addVertex(pose, (float) from.x, (float) from.y, (float) from.z)
                .setColor(red, green, blue, 255)
                .setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z);
        consumer.addVertex(pose, (float) to.x, (float) to.y, (float) to.z)
                .setColor(red, green, blue, 255)
                .setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z);
    }

    /**
     * Shaft plus a four-barbed head, camera relative. Four barbs rather than two so the direction
     * stays readable from any angle, which a flat arrowhead in 3D does not.
     */
    public static void renderArrow(PoseStack poseStack, MultiBufferSource bufferSource,
                                   Vec3 from, Vec3 to, double headSize, int color) {
        Vec3 along = to.subtract(from);
        if (along.lengthSqr() < 1.0E-8) return;
        renderLine(poseStack, bufferSource, from, to, color);

        Vec3 direction = along.normalize();
        // any axis not parallel to the shaft works as a seed for the perpendicular basis
        Vec3 seed = Math.abs(direction.y) > 0.9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 side = direction.cross(seed).normalize();
        Vec3 up = direction.cross(side).normalize();
        Vec3 base = to.subtract(direction.scale(headSize));
        double spread = headSize * 0.5;
        renderLine(poseStack, bufferSource, to, base.add(side.scale(spread)), color);
        renderLine(poseStack, bufferSource, to, base.subtract(side.scale(spread)), color);
        renderLine(poseStack, bufferSource, to, base.add(up.scale(spread)), color);
        renderLine(poseStack, bufferSource, to, base.subtract(up.scale(spread)), color);
    }

    /** Unlike the boxes, text takes world coordinates: it has to billboard against the camera. */
    public static void renderFloatingText(PoseStack poseStack, MultiBufferSource bufferSource, String text,
                                          double x, double y, double z, int color, float scale,
                                          boolean centered, boolean seeThrough) {
        Minecraft mc = Minecraft.getInstance();
        Camera camera = mc.gameRenderer.getMainCamera();
        if (!camera.isInitialized() || mc.getEntityRenderDispatcher().options == null) return;

        Font font = mc.font;
        Vec3 cameraPos = camera.getPosition();
        poseStack.pushPose();
        poseStack.translate((float) (x - cameraPos.x), (float) (y - cameraPos.y) + 0.07F, (float) (z - cameraPos.z));
        poseStack.mulPose(camera.rotation());
        poseStack.scale(scale, -scale, scale);
        float xOffset = centered ? -font.width(text) / 2.0F : 0.0F;
        font.drawInBatch(text, xOffset, 0.0F, color, false, poseStack.last().pose(), bufferSource,
                seeThrough ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
        poseStack.popPose();
    }
}
