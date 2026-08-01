package net.mehvahdjukaar.sleep_tight.test;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.mehvahdjukaar.sleep_tight.SleepTightClient;
import net.mehvahdjukaar.sleep_tight.test.controller.BirdFlightConfig;
import net.mehvahdjukaar.sleep_tight.test.throttle.FlightEnvelope;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class TestMobRenderer extends MobRenderer<BirdTestMob, TestMobModel> {

    public TestMobRenderer(EntityRendererProvider.Context context) {
        super(context, new TestMobModel(context.bakeLayer(SleepTightClient.BEDBUG)), 0.375f);
    }

    @Override
    public ResourceLocation getTextureLocation(BirdTestMob entity) {
        return SleepTightClient.BEDBUG_TEXTURE;
    }

    /**
     * Points the whole body along the flight instead of only the head. The move control already
     * tracks pitch to the actual velocity, but vanilla feeds pitch to the head alone, so without
     * this the flown slope is invisible and a mob climbing at 45 degrees looks identical to one
     * flying level. Bank is derived here rather than synched because yaw is interpolated already.
     */
    @Override
    protected void setupRotations(BirdTestMob entity, PoseStack poseStack, float bob, float yBodyRot,
                                  float partialTick, float scale) {
        super.setupRotations(entity, poseStack, bob, yBodyRot, partialTick, scale);
        poseStack.mulPose(Axis.XP.rotationDegrees(-entity.getViewXRot(partialTick)));

        // as a fraction of the rate the bird can turn at rather than a flat degrees-per-degree, so a
        // full-rate corner banks fully at any speed. FLYING_SPEED is syncable, so the client can
        // rebuild the envelope and reach the same number the server steered by
        float yawRate = Mth.degreesDifference(entity.yRotO, entity.getYRot());
        float maxYawRate = (float) (FlightEnvelope.forMob(entity).maxYawRate() * Mth.RAD_TO_DEG);
        float bank = maxYawRate <= 1.0E-4F ? 0.0F
                : Mth.clamp(BirdFlightConfig.maxBankAngle * yawRate / maxYawRate,
                        -BirdFlightConfig.maxBankAngle, BirdFlightConfig.maxBankAngle);
        poseStack.mulPose(Axis.ZP.rotationDegrees(bank));
    }
}
