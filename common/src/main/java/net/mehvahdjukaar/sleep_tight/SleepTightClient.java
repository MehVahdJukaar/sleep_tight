package net.mehvahdjukaar.sleep_tight;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import net.mehvahdjukaar.moonlight.api.platform.ClientPlatformHelper;
import net.mehvahdjukaar.sleep_tight.client.BedEntityRenderer;
import net.mehvahdjukaar.sleep_tight.client.HammockBlockTileRenderer;
import net.mehvahdjukaar.sleep_tight.common.BedEntity;
import net.mehvahdjukaar.sleep_tight.common.HammockBlockEntity;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
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
        event.register(SleepTight.BED_ENTITY.get(), BedEntityRenderer::new);
    }

    private static void registerBlockEntityRenderers(ClientPlatformHelper.BlockEntityRendererEvent event) {
        event.register(SleepTight.HAMMOCK_TILE.get(), HammockBlockTileRenderer::new);
    }

    public static <T extends LivingEntity> void rotatePlayerInBed(T entity, PoseStack poseStack, float partialTicks,
                                                                  MultiBufferSource bufferSource) {
        BlockPos pos = null;
        var p = entity.getSleepingPos();
        if (p.isPresent()) {
            pos = p.get();
        } else if (entity.getVehicle() instanceof BedEntity be) {
            pos = be.getOnPos();
        }
        if (pos != null && entity.getLevel().getBlockEntity(pos) instanceof HammockBlockEntity tile) {


            float o = tile.getPivotOffset();
            Vector3f v = tile.getDirection().step();
            poseStack.translate(0, o, 0);
            poseStack.mulPose(v.rotationDegrees(15 * Mth.sin(tile.getYaw(partialTicks))));
            poseStack.translate(0, -o, 0);

            if(entity.getVehicle() != null){
                float f = Mth.rotLerp(partialTicks, entity.yBodyRotO, entity.yBodyRot);

                float f1 =90- tile.getDirection().toYRot();
                poseStack.mulPose(Vector3f.YP.rotationDegrees(f1));
                poseStack.translate(1.5,0.25,0);



            }
        }
    }

    public static void cameraSetup() {

    }
}
