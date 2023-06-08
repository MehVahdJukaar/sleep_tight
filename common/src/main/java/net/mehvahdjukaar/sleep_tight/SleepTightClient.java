package net.mehvahdjukaar.sleep_tight;

import net.mehvahdjukaar.moonlight.api.client.model.NestedModelLoader;
import net.mehvahdjukaar.moonlight.api.misc.EventCalled;
import net.mehvahdjukaar.moonlight.api.platform.ClientHelper;
import net.mehvahdjukaar.sleep_tight.client.InfestedBedBakedModel;
import net.mehvahdjukaar.sleep_tight.client.particles.BedbugParticle;
import net.mehvahdjukaar.sleep_tight.client.particles.DreamParticle;
import net.mehvahdjukaar.sleep_tight.client.renderers.BedbugEntityRenderer;
import net.mehvahdjukaar.sleep_tight.client.renderers.HammockBlockTileRenderer;
import net.mehvahdjukaar.sleep_tight.client.renderers.InfestedBedRenderer;
import net.mehvahdjukaar.sleep_tight.client.renderers.InvisibleEntityRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.blockentity.BedRenderer;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;

import java.util.Arrays;
import java.util.Comparator;

public class SleepTightClient {

    public static final ModelLayerLocation NIGHT_BAG = loc("night_bag");
    public static final ModelLayerLocation HAMMOCK = loc("hammock");
    public static final ModelLayerLocation BEDBUG = loc("bedbug");
    public static final ResourceLocation BED_SHEET = new ResourceLocation("textures/atlas/beds.png");
    public static final ResourceLocation ICONS = SleepTight.res("textures/gui/sleep_icons.png");
    public static final ResourceLocation BEDBUG_TEXTURE = SleepTight.res("textures/entity/bedbug.png");

    public static final Material[] HAMMOCK_TEXTURES = Arrays.stream(DyeColor.values())
            .sorted(Comparator.comparingInt(DyeColor::getId))
            .map(dyeColor -> new Material(BED_SHEET, SleepTight.res("entity/bed/hammock_" + dyeColor.getName())))
            .toArray(Material[]::new);

    public static void init() {
        ClientHelper.addModelLayerRegistration(SleepTightClient::registerLayers);
        ClientHelper.addEntityRenderersRegistration(SleepTightClient::registerEntityRenderers);
        ClientHelper.addBlockEntityRenderersRegistration(SleepTightClient::registerBlockEntityRenderers);
        ClientHelper.addParticleRegistration(SleepTightClient::registerParticles);
        ClientHelper.addModelLoaderRegistration(SleepTightClient::registerModelLoaders);
    }



    public static void setup() {
    }

    private static ModelLayerLocation loc(String name) {
        return new ModelLayerLocation(SleepTight.res(name), name);
    }

    private static void registerParticles(ClientHelper.ParticleEvent event) {
        event.register(SleepTight.DREAM_PARTICLE.get(), DreamParticle.Factory::new);
        event.register(SleepTight.BEDBUG_PARTICLE.get(), BedbugParticle.Factory::new);
    }

    private static void registerLayers(ClientHelper.ModelLayerEvent event) {
        event.register(HAMMOCK, HammockBlockTileRenderer::createLayer);
        event.register(BEDBUG, BedbugEntityRenderer::createLayer);
    }

    private static void registerEntityRenderers(ClientHelper.EntityRendererEvent event) {
        event.register(SleepTight.BED_ENTITY.get(), InvisibleEntityRenderer::new);
        event.register(SleepTight.DREAMER_ESSENCE_ENTITY.get(), InvisibleEntityRenderer::new);
        event.register(SleepTight.BEDBUG_ENTITY.get(), BedbugEntityRenderer::new);
    }

    private static void registerBlockEntityRenderers(ClientHelper.BlockEntityRendererEvent event) {
        event.register(SleepTight.HAMMOCK_TILE.get(), HammockBlockTileRenderer::new);
        event.register(SleepTight.INFESTED_BED_TILE.get(), InfestedBedRenderer::new);
    }

    @EventCalled
    private static void registerModelLoaders(ClientHelper.ModelLoaderEvent event) {
        event.register(SleepTight.res("infested_bed"), new InfestedBedBakedModel.Loader());
    }

    public static Player getPlayer() {
        return Minecraft.getInstance().player;
    }
}
