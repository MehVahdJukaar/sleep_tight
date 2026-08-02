package net.mehvahdjukaar.sleep_tight.test;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * Bedbug geometry (it bakes the bedbug layer) with a plain six legged walk cycle, so the test mob
 * doesn't drag in the bedbug's burrow/splatter state.
 */
public class TestMobModel extends HierarchicalModel<BirdTestMob> {
    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart antenna;
    private final ModelPart[] rightLegs;
    private final ModelPart[] leftLegs;

    /**
     * How far the whole body is pitched, in degrees, set by the renderer just before this poses. The
     * head cancels it out: a bird holds its head level while the body rotates underneath, and
     * without this the head would carry the dive on top of wherever it is looking.
     */
    public float bodyPitch;

    public TestMobModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.antenna = head.getChild("antenna");
        this.rightLegs = new ModelPart[]{
                root.getChild("right_hind_leg"), root.getChild("right_middle_leg"), root.getChild("right_front_leg")};
        this.leftLegs = new ModelPart[]{
                root.getChild("left_hind_leg"), root.getChild("left_middle_leg"), root.getChild("left_front_leg")};
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(BirdTestMob entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.head.xRot = (27.5F + headPitch + this.bodyPitch) * Mth.DEG_TO_RAD;
        this.head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        this.antenna.xRot = 0.4F + Mth.cos(ageInTicks * 1.5F) * 0.1F;

        float speed = 1.87F;
        for (int i = 0; i < 3; i++) {
            float phase = Mth.PI * 2 / 3f * i;
            float yaw = -Mth.cos(limbSwing * speed * 2 + phase) * 0.5F * limbSwingAmount;
            float lift = Math.abs(Mth.sin(limbSwing * speed + phase) * 0.4F) * limbSwingAmount;
            //  middle legs sit flatter, the outer ones splay forward and back
            float rest = i == 1 ? 0.68F : 0.885F;
            float splay = i == 1 ? 0.0F : (i == 0 ? 0.585F : -0.585F);

            rightLegs[i].zRot = -rest + lift;
            leftLegs[i].zRot = rest - lift;
            rightLegs[i].yRot = splay + yaw;
            leftLegs[i].yRot = -splay - yaw;
        }
    }
}