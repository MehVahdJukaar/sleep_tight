package net.mehvahdjukaar.sleep_tight;

import net.mehvahdjukaar.moonlight.api.platform.ClientPlatformHelper;
import net.mehvahdjukaar.sleep_tight.client.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
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
            .map(dyeColor -> new Material(BED_SHEET, SleepTight.res("entity/hammocks/" + dyeColor.getName())))
            .toArray(Material[]::new);

    public static void init() {
        ClientPlatformHelper.addModelLayerRegistration(SleepTightClient::registerLayers);
        ClientPlatformHelper.addEntityRenderersRegistration(SleepTightClient::registerEntityRenderers);
        ClientPlatformHelper.addBlockEntityRenderersRegistration(SleepTightClient::registerBlockEntityRenderers);
        ClientPlatformHelper.addAtlasTextureCallback(BED_SHEET, SleepTightClient::addTextures);
        ClientPlatformHelper.addParticleRegistration(SleepTightClient::registerParticles);
    }



    public static void setup() {
    }

    private static ModelLayerLocation loc(String name) {
        return new ModelLayerLocation(SleepTight.res(name), name);
    }

    private static void addTextures(ClientPlatformHelper.AtlasTextureEvent event) {
        Arrays.stream(HAMMOCK_TEXTURES).forEach(e -> event.addSprite(e.texture()));
    }

    private static void registerParticles(ClientPlatformHelper.ParticleEvent event) {
        event.register(SleepTight.DREAM_PARTICLE.get(), DreamParticle.Factory::new);
        event.register(SleepTight.BEDBUG_PARTICLE.get(), BedbugParticle.Factory::new);
    }

    private static void registerLayers(ClientPlatformHelper.ModelLayerEvent event) {
        event.register(HAMMOCK, HammockBlockTileRenderer::createLayer);
        event.register(BEDBUG, BedbugEntityRenderer::createLayer);
    }

    private static void registerEntityRenderers(ClientPlatformHelper.EntityRendererEvent event) {
        event.register(SleepTight.BED_ENTITY.get(), InvisibleEntityRenderer::new);
        event.register(SleepTight.DREAMER_ESSENCE_ENTITY.get(), InvisibleEntityRenderer::new);
        event.register(SleepTight.BEDBUG_ENTITY.get(), BedbugEntityRenderer::new);
    }

    private static void registerBlockEntityRenderers(ClientPlatformHelper.BlockEntityRendererEvent event) {
        event.register(SleepTight.HAMMOCK_TILE.get(), HammockBlockTileRenderer::new);
        event.register(SleepTight.INFESTED_BED_TILE.get(), InfestedBedRenderer::new);
    }


    public static Player getPlayer() {
        return Minecraft.getInstance().player;
    }
}
