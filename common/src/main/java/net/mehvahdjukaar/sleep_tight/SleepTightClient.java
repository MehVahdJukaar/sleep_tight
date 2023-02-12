package net.mehvahdjukaar.sleep_tight;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import net.mehvahdjukaar.moonlight.api.platform.ClientPlatformHelper;
import net.mehvahdjukaar.sleep_tight.client.HammockBlockTileRenderer;
import net.mehvahdjukaar.sleep_tight.common.HammockBlockEntity;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.DyeColor;

import java.util.Arrays;
import java.util.Comparator;

public class SleepTightClient {

    public static final ModelLayerLocation NIGHT_BAG = loc("night_bag");
    public static final ModelLayerLocation HAMMOCK = loc("hammock");
    public static final ResourceLocation BED_SHEET = new ResourceLocation("textures/atlas/beds.png");

    public static final Material[] HAMMOCK_TEXTURES = Arrays.stream(DyeColor.values())
            .sorted(Comparator.comparingInt(DyeColor::getId))
            .map(dyeColor -> new Material(BED_SHEET, SleepTight.res("entity/hammocks/" + dyeColor.getName())))
            .toArray(Material[]::new);

    public static void init() {
        ClientPlatformHelper.addModelLayerRegistration(SleepTightClient::registerLayers);
        ClientPlatformHelper.addEntityRenderersRegistration(SleepTightClient::registerEntityRenderers);
        ClientPlatformHelper.addBlockEntityRenderersRegistration(SleepTightClient::registerBlockEntityRenderers);
        ClientPlatformHelper.addAtlasTextureCallback(BED_SHEET, SleepTightClient::addTextures);
    }


    public static void setup() {
    }

    private static ModelLayerLocation loc(String name) {
        return new ModelLayerLocation(SleepTight.res(name), name);
    }

    private static void addTextures(ClientPlatformHelper.AtlasTextureEvent event) {
        Arrays.stream(HAMMOCK_TEXTURES).forEach(e -> event.addSprite(e.texture()));
    }

    private static void registerLayers(ClientPlatformHelper.ModelLayerEvent event) {
        event.register(HAMMOCK, HammockBlockTileRenderer::createLayer);
    }

    private static void registerEntityRenderers(ClientPlatformHelper.EntityRendererEvent event) {
    }

    private static void registerBlockEntityRenderers(ClientPlatformHelper.BlockEntityRendererEvent event) {
        event.register(SleepTight.HAMMOCK_TILE.get(), HammockBlockTileRenderer::new);
    }

    public static <T extends LivingEntity> void renderStuff(T entity, PoseStack poseStack, float p, MultiBufferSource bufferSource) {
        var e = entity.getLevel().getBlockEntity(entity.getSleepingPos().get());
        if (e instanceof HammockBlockEntity tile) {
            float o = tile.getPivotOffset();
            Vector3f v = tile.getAxis() == Direction.Axis.Z ? Vector3f.ZN : Vector3f.XN;
            poseStack.translate(0, o, 0);
            poseStack.mulPose(v.rotationDegrees(-15 * Mth.sin(tile.getYaw(p))));
            var pBuffer = bufferSource.getBuffer(RenderType.lines());
            Matrix4f matrix4f = poseStack.last().pose();
            Matrix3f matrix3f = poseStack.last().normal();
            pBuffer.vertex(matrix4f, 0.0F, 0, -1.0F)
                    .color(0, 255, 255, 255)
                    .normal(matrix3f, 0, 1, 0).endVertex();
            pBuffer.vertex(matrix4f, 0, 0, 2)
                    .color(0, 255, 255, 255)
                    .normal(matrix3f, 0, 1, 0).endVertex();

            poseStack.translate(0, -o, 0);

        }
    }
}
