package net.mehvahdjukaar.sleep_tight.test;

import net.mehvahdjukaar.sleep_tight.client.renderers.BedbugEntityRenderer;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * Bedbug geometry with a plain six legged walk cycle and a pair of placeholder wings, so the test
 * mob doesn't drag in the bedbug's burrow/splatter state. It bakes its own layer rather than the
 * bedbug's, since wings added to that one would show up on the real bedbug too.
 */
public class TestMobModel extends HierarchicalModel<BirdTestMob> {

    // where a wing sits folded, in radians below horizontal. Positive is down, the same convention
    // the legs use
    private static final float WING_FOLDED = 1.05F;
    // and how far either side of horizontal it swings once it is out
    private static final float WING_BEAT = 0.7F;

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart antenna;
    private final ModelPart rightWing;
    private final ModelPart leftWing;
    private final ModelPart[] rightLegs;
    private final ModelPart[] leftLegs;

    /**
     * How far the whole body is pitched, in degrees, set by the renderer just before this poses. The
     * head cancels it out: a bird holds its head level while the body rotates underneath, and
     * without this the head would carry the dive on top of wherever it is looking.
     */
    public float bodyPitch;

    /**
     * Where the wings are in their stroke, in whole strokes, and how far out they are held, 0 folded
     * to 1 spread. Both come off the entity, which works them out from the thrust it is putting out
     * and whether its feet are down. Set by the renderer alongside {@link #bodyPitch}.
     */
    public float flapPhase;
    public float wingSpread;

    public TestMobModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.antenna = head.getChild("antenna");
        this.rightWing = root.getChild("right_wing");
        this.leftWing = root.getChild("left_wing");
        this.rightLegs = new ModelPart[]{
                root.getChild("right_hind_leg"), root.getChild("right_middle_leg"), root.getChild("right_front_leg")};
        this.leftLegs = new ModelPart[]{
                root.getChild("left_hind_leg"), root.getChild("left_middle_leg"), root.getChild("left_front_leg")};
    }

    /** The bedbug body with a slab bolted on either side of it. Placeholder, as is the rest of it. */
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = BedbugEntityRenderer.createMesh();
        PartDefinition main = mesh.getRoot();
        float shoulder = 24 - 5.5F - 2.5F;
        main.addOrReplaceChild("right_wing", CubeListBuilder.create().texOffs(20, 0)
                        .addBox(-9.0F, -0.5F, -3.0F, 9.0F, 1.0F, 6.0F),
                PartPose.offset(-5.0F, shoulder, 0.0F));
        main.addOrReplaceChild("left_wing", CubeListBuilder.create().texOffs(20, 0)
                        .mirror().addBox(0.0F, -0.5F, -3.0F, 9.0F, 1.0F, 6.0F),
                PartPose.offset(5.0F, shoulder, 0.0F));
        return LayerDefinition.create(mesh, 64, 32);
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

        // one angle, mirrored. The stroke is scaled by how far out the wings are, so a folded pair
        // sits still against the flanks however fast the phase underneath happens to be running
        float wingRest = Mth.lerp(this.wingSpread, WING_FOLDED, 0.0F);
        float stroke = Mth.sin(this.flapPhase * Mth.TWO_PI) * WING_BEAT * this.wingSpread;
        this.leftWing.zRot = wingRest + stroke;
        this.rightWing.zRot = -this.leftWing.zRot;

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
