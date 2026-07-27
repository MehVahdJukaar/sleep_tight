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
