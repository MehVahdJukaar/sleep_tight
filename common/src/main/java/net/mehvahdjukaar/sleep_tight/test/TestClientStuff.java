package net.mehvahdjukaar.sleep_tight.test;

import net.mehvahdjukaar.moonlight.api.platform.ClientHelper;
import net.mehvahdjukaar.sleep_tight.SleepTight;
import net.minecraft.client.model.geom.ModelLayerLocation;

public class TestClientStuff {

    // its own layer rather than the bedbug's, which it used to share. The wings are built on top of
    // that mesh, and putting them in the shared definition would grow the real bedbug too
    public static final ModelLayerLocation TEST_BIRD =
            new ModelLayerLocation(SleepTight.res("test_bird"), "main");

    public static void init() {
        ClientHelper.addModelLayerRegistration(TestClientStuff::registerLayers);
        ClientHelper.addEntityRenderersRegistration(TestClientStuff::registerEntityRenderers);
    }

    private static void registerLayers(ClientHelper.ModelLayerEvent event) {
        event.register(TEST_BIRD, TestMobModel::createBodyLayer);
    }

    private static void registerEntityRenderers(ClientHelper.EntityRendererEvent event) {
        event.register(TestStuff.TEST_MOB.get(), TestMobRenderer::new);
    }
}
