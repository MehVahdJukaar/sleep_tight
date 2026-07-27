package net.mehvahdjukaar.sleep_tight.test;

import net.mehvahdjukaar.sleep_tight.SleepTightClient;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class TestMobRenderer extends MobRenderer<BirdTestMob, TestMobModel> {

    public TestMobRenderer(EntityRendererProvider.Context context) {
        super(context, new TestMobModel(context.bakeLayer(SleepTightClient.BEDBUG)), 0.375f);
    }

    @Override
    public ResourceLocation getTextureLocation(BirdTestMob entity) {
        return SleepTightClient.BEDBUG_TEXTURE;
    }
}
