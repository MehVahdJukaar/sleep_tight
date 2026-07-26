package net.mehvahdjukaar.sleep_tight.test;

import net.mehvahdjukaar.moonlight.api.platform.ClientHelper;

public class TestClientStuff {

    public static void init() {
        ClientHelper.addEntityRenderersRegistration(TestClientStuff::registerEntityRenderers);
    }

    private static void registerEntityRenderers(ClientHelper.EntityRendererEvent event) {
        event.register(TestStuff.TEST_MOB.get(), TestMobRenderer::new);
    }
}
