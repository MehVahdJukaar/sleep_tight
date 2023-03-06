package net.mehvahdjukaar.sleep_tight.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.mehvahdjukaar.sleep_tight.SleepTightClient;
import net.mehvahdjukaar.sleep_tight.common.BedbugEntity;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RabbitRenderer;
import net.minecraft.resources.ResourceLocation;

public class BedbugEntityRenderer<T extends BedbugEntity> extends MobRenderer<T, BedbugModel<T>> {

    public BedbugEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new BedbugModel<>(context.bakeLayer(SleepTightClient.BEDBUG)), 7/16f);
    }

    @Override
    protected float getFlipDegrees(T livingEntity) {
        return 180.0F;
    }

    /**
     * Returns the location of an entity's texture.
     */
    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return SleepTightClient.BEDBUG_TEXTURE;
    }

    @Override
    public void render(T entity, float entityYaw, float partialTicks, PoseStack matrixStack, MultiBufferSource buffer, int packedLight) {
        matrixStack.pushPose();
        matrixStack.scale(0.75F, 0.75F, 0.75F);
        super.render(entity, entityYaw, partialTicks, matrixStack, buffer, packedLight);

        matrixStack.popPose();
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition main = mesh.getRoot();
        int h = 24-5;
        var head = main.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 8)
                .addBox(-2.5F, 0.0F, -3F, 5.0F, 2.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, h-1.5f, -9.0F, (float) Math.toRadians(27.5),0,0));

        head.addOrReplaceChild("antenna", CubeListBuilder.create().texOffs(41, 0)
                        .addBox(-4.5F, 0.0F, 0F, 9.0F, 2.0F, 0.0F),
                PartPose.offsetAndRotation(0.0F, -2, -2.5f, (float) 0,0,0));

        main.addOrReplaceChild("body0", CubeListBuilder.create().texOffs(0, 0
        ).addBox(-3.5F, -2, -1.5F, 7.0F, 4.0F, 3.0F),
                PartPose.offset(0.0F, h-0.5f, -7.5F));
        main.addOrReplaceChild("body1", CubeListBuilder.create().texOffs(0, 15)
                .addBox(-5.0F, -2.5F, -6.0F, 10, 5, 12.0F),
                PartPose.offset(0.0F, h, 0.0F));

        CubeListBuilder rightLeg = CubeListBuilder.create().texOffs(20, 0)
                .addBox(-7.0F, -1.0F, -1.0F, 8, 2.0F, 2.0F);
        CubeListBuilder leftLeg = CubeListBuilder.create().texOffs(20, 0)
                .mirror().addBox(-1.0F, -1.0F, -1.0F, 8, 2.0F, 2.0F);

        float w = 5f;
        float z1 = 2;
        float z2 = 2;
        float z3 = 0;
        float z4 = -2;
        main.addOrReplaceChild("right_hind_leg", rightLeg, PartPose.offset(-w, h, z1));
        main.addOrReplaceChild("left_hind_leg", leftLeg, PartPose.offset(w, h, z1));
        main.addOrReplaceChild("right_middle_hind_leg", rightLeg, PartPose.offset(-w, h, z2));
        main.addOrReplaceChild("left_middle_hind_leg", leftLeg, PartPose.offset(w, h, z2));
        main.addOrReplaceChild("right_middle_front_leg", rightLeg, PartPose.offset(-w, h, z3));
        main.addOrReplaceChild("left_middle_front_leg", leftLeg, PartPose.offset(w, h, z3));
        main.addOrReplaceChild("right_front_leg", rightLeg, PartPose.offset(-w, h, z4));
        main.addOrReplaceChild("left_front_leg", leftLeg, PartPose.offset(w, h, z4));
        return LayerDefinition.create(mesh, 64, 32);
    }
}
