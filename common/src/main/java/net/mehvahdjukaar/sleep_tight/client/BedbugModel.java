package net.mehvahdjukaar.sleep_tight.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.mehvahdjukaar.sleep_tight.common.BedbugEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.SpiderModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

public class BedbugModel<T extends LivingEntity> extends SpiderModel<T> {
    private final ModelPart head;
    private final ModelPart rightHindLeg;
    private final ModelPart leftHindLeg;
    private final ModelPart rightMiddleHindLeg;
    private final ModelPart leftMiddleHindLeg;
    private final ModelPart rightMiddleFrontLeg;
    private final ModelPart leftMiddleFrontLeg;
    private final ModelPart rightFrontLeg;
    private final ModelPart leftFrontLeg;
    private final ModelPart body;
    private final ModelPart antenna;

    public BedbugModel(ModelPart modelPart) {
        super(modelPart);
        this.head = modelPart.getChild("head");
        this.body = modelPart.getChild("body0");
        this.antenna = head.getChild("antenna");
        this.rightHindLeg = modelPart.getChild("right_hind_leg");
        this.leftHindLeg = modelPart.getChild("left_hind_leg");
        this.rightMiddleHindLeg = modelPart.getChild("right_middle_hind_leg");
        this.leftMiddleHindLeg = modelPart.getChild("left_middle_hind_leg");
        this.rightMiddleFrontLeg = modelPart.getChild("right_middle_front_leg");
        this.leftMiddleFrontLeg = modelPart.getChild("left_middle_front_leg");
        this.rightFrontLeg = modelPart.getChild("right_front_leg");
        this.leftFrontLeg = modelPart.getChild("left_front_leg");
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        super.renderToBuffer(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

        this.leftHindLeg.visible = false;
        this.rightHindLeg.visible = false;

        float fortyFive = 0.7853982F; //45
        float mi = 0.58119464F; //33.3

        float fortyFive2 = 0.7853982F; //45
        float h = 0.3926991F; //22.5



        this.rightHindLeg.zRot = -fortyFive;
        this.leftHindLeg.zRot = fortyFive;

        this.rightMiddleHindLeg.zRot = -fortyFive;
        this.leftMiddleHindLeg.zRot = fortyFive;
        this.rightMiddleFrontLeg.zRot = -mi;
        this.leftMiddleFrontLeg.zRot = mi;
        this.rightFrontLeg.zRot = -fortyFive;
        this.leftFrontLeg.zRot = fortyFive;
        float g = -0.0F;

        this.rightHindLeg.yRot = fortyFive2;
        this.leftHindLeg.yRot = -fortyFive2;

        this.rightMiddleHindLeg.yRot = fortyFive2;
        this.leftMiddleHindLeg.yRot = -fortyFive2;
        this.rightMiddleFrontLeg.yRot = -0;
        this.leftMiddleFrontLeg.yRot = 0;
        this.rightFrontLeg.yRot = -fortyFive2;
        this.leftFrontLeg.yRot = fortyFive2;

        float s = 0.6662F;

        float i = -(Mth.cos(limbSwing * s * 2.0F + 0.0F) * 0.4F) * limbSwingAmount;
        float j = -(Mth.cos(limbSwing * s * 2.0F + 3.1415927F) * 0.4F) * limbSwingAmount;
        float k = -(Mth.cos(limbSwing * s * 2.0F + 1.5707964F) * 0.4F) * limbSwingAmount;
        float l = -(Mth.cos(limbSwing * s * 2.0F + 4.712389F) * 0.4F) * limbSwingAmount;
        float m = Math.abs(Mth.sin(limbSwing * s + 0.0F) * 0.4F) * limbSwingAmount;
        float n = Math.abs(Mth.sin(limbSwing * s + 3.1415927F) * 0.4F) * limbSwingAmount;
        float o = Math.abs(Mth.sin(limbSwing * s + 1.5707964F) * 0.4F) * limbSwingAmount;
        float p = Math.abs(Mth.sin(limbSwing * s + 4.712389F) * 0.4F) * limbSwingAmount;
        ModelPart leg = this.rightHindLeg;
        leg.yRot += i;
        leg = this.leftHindLeg;
        leg.yRot += -i;
        leg = this.rightMiddleHindLeg;
        leg.yRot += j;
        leg = this.leftMiddleHindLeg;
        leg.yRot += -j;
        leg = this.rightMiddleFrontLeg;
        leg.yRot += k;
        leg = this.leftMiddleFrontLeg;
        leg.yRot += -k;
        leg = this.rightFrontLeg;
        leg.yRot += l;
        leg = this.leftFrontLeg;
        leg.yRot += -l;
        leg = this.rightHindLeg;
        leg.zRot += m;
        leg = this.leftHindLeg;
        leg.zRot += -m;
        leg = this.rightMiddleHindLeg;
        leg.zRot += n;
        leg = this.leftMiddleHindLeg;
        leg.zRot += -n;
        leg = this.rightMiddleFrontLeg;
        leg.zRot += o;
        leg = this.leftMiddleFrontLeg;
        leg.zRot += -o;
        leg = this.rightFrontLeg;
        leg.zRot += p;
        leg = this.leftFrontLeg;
        leg.zRot += -p;
    }
}
