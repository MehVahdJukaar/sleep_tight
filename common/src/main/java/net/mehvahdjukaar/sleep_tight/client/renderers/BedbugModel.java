package net.mehvahdjukaar.sleep_tight.client.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.mehvahdjukaar.sleep_tight.common.entities.BedbugEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

public class BedbugModel<T extends BedbugEntity> extends HierarchicalModel<T> {
    private final ModelPart head;
    private final ModelPart rightHindLeg;
    private final ModelPart leftHindLeg;
    private final ModelPart rightMiddleLeg;
    private final ModelPart leftMiddleLeg;
    private final ModelPart rightFrontLeg;
    private final ModelPart leftFrontLeg;
    private final ModelPart body;
    private final ModelPart antenna;
    private final ModelPart root;

    public BedbugModel(ModelPart modelPart) {
        this.root = modelPart;
        this.head = modelPart.getChild("head");
        this.body = modelPart.getChild("body0");
        this.antenna = head.getChild("antenna");
        this.rightHindLeg = modelPart.getChild("right_hind_leg");
        this.leftHindLeg = modelPart.getChild("left_hind_leg");
        this.rightMiddleLeg = modelPart.getChild("right_middle_leg");
        this.leftMiddleLeg = modelPart.getChild("left_middle_leg");
        this.rightFrontLeg = modelPart.getChild("right_front_leg");
        this.leftFrontLeg = modelPart.getChild("left_front_leg");
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

        this.head.xRot = (27.5f + headPitch) * 0.017453292F;

        float fortyFive = 0.7853982F + 0.1f; //45
        float thirtyThree = 0.58119464F + 0.1f; //33.3

        float fortyFive2 = 0.7853982F - 0.2f; //45
        float h = 0.3926991F; //22.5


        this.rightHindLeg.zRot = -fortyFive;
        this.leftHindLeg.zRot = fortyFive;
        this.rightMiddleLeg.zRot = -thirtyThree;
        this.leftMiddleLeg.zRot = thirtyThree;
        this.rightFrontLeg.zRot = -fortyFive;
        this.leftFrontLeg.zRot = fortyFive;

        this.rightHindLeg.yRot = fortyFive2;
        this.leftHindLeg.yRot = -fortyFive2;
        this.rightMiddleLeg.yRot = -0;
        this.leftMiddleLeg.yRot = 0;
        this.rightFrontLeg.yRot = -fortyFive2;
        this.leftFrontLeg.yRot = fortyFive2;

        int b = entity.burrowingTicks;

        float speed = 0.6662F + 1.2f;
        float ampl = 0.4f + 0.1f;
        float ampl2 = 0.4f;

        //  limbSwingAmount = 0.5f;
        // limbSwing = entity.tickCount/20f;

        float a1 = (Mth.cos(ageInTicks * 1.5f)) * (0.3f + limbSwingAmount) * 0.3f;

        this.antenna.xRot = 0.4f + a1;


        float c0 = -(Mth.cos(limbSwing * speed * 2.0F + 0) * ampl) * limbSwingAmount;
        float c1 = -(Mth.cos(limbSwing * speed * 2.0F + Mth.PI * 4 / 3f) * ampl) * limbSwingAmount;
        float c2 = -(Mth.cos(limbSwing * speed * 2.0F + Mth.PI * 2 / 3f) * ampl) * limbSwingAmount;

        float s0 = Math.abs(Mth.sin(limbSwing * speed + 0) * ampl2) * limbSwingAmount;
        float s1 = Math.abs(Mth.sin(limbSwing * speed + Mth.PI * 4 / 3f) * ampl2) * limbSwingAmount;
        float s2 = Math.abs(Mth.sin(limbSwing * speed + Mth.PI * 2 / 3f) * ampl2) * limbSwingAmount;


        ModelPart leg = this.rightHindLeg;
        leg.yRot += c0;
        leg = this.leftHindLeg;
        leg.yRot += -c0;
        leg = this.rightMiddleLeg;
        leg.yRot += c1;
        leg = this.leftMiddleLeg;
        leg.yRot += -c1;
        leg = this.rightFrontLeg;
        leg.yRot += c2;
        leg = this.leftFrontLeg;
        leg.yRot += -c2;


        leg = this.rightHindLeg;
        leg.zRot += s0;
        leg = this.leftHindLeg;
        leg.zRot += -s0;
        leg = this.rightMiddleLeg;
        leg.zRot += s1;
        leg = this.leftMiddleLeg;
        leg.zRot += -s1;
        leg = this.rightFrontLeg;
        leg.zRot += s2;
        leg = this.leftFrontLeg;
        leg.zRot += -s2;
    }
}
