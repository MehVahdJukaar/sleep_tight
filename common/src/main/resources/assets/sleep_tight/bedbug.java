// Made with Blockbench 4.5.2
// Exported for Minecraft version 1.17 - 1.18 with Mojang mappings
// Paste this class into your mod and generate all required imports


public class custom_model<T extends Entity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation("modid", "custom_model"), "main");
	private final ModelPart head;
	private final ModelPart body;
	private final ModelPart leg01;
	private final ModelPart leg02;
	private final ModelPart leg03;
	private final ModelPart leg04;
	private final ModelPart leg05;
	private final ModelPart leg06;

	public custom_model(ModelPart root) {
		this.head = root.getChild("head");
		this.body = root.getChild("body");
		this.leg01 = root.getChild("leg01");
		this.leg02 = root.getChild("leg02");
		this.leg03 = root.getChild("leg03");
		this.leg04 = root.getChild("leg04");
		this.leg05 = root.getChild("leg05");
		this.leg06 = root.getChild("leg06");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition head = partdefinition.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(-12.0F, 21.0F, 0.5F));

		PartDefinition antena02_r1 = head.addOrReplaceChild("antena02_r1", CubeListBuilder.create().texOffs(0, 4).mirror().addBox(0.389F, -0.9281F, 0.5F, 0.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)).mirror(false)
		.texOffs(0, 0).mirror().addBox(0.389F, -0.9281F, -5.5F, 0.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, -3.0F, 0.0F, 0.0F, 0.0F, -1.1345F));

		PartDefinition head_r1 = head.addOrReplaceChild("head_r1", CubeListBuilder.create().texOffs(48, 11).addBox(-2.0F, -2.0F, -2.5F, 3.0F, 2.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.0F, -2.0F, -1.0F, 0.0F, 0.0F, -0.3927F));

		PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-6.0F, -8.0F, -6.0F, 12.0F, 5.0F, 11.0F, new CubeDeformation(0.0F))
		.texOffs(44, 0).addBox(-9.0F, -8.0F, -4.0F, 3.0F, 4.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition leg01 = partdefinition.addOrReplaceChild("leg01", CubeListBuilder.create(), PartPose.offset(-1.0F, 24.0F, 5.0F));

		PartDefinition leg01_r1 = leg01.addOrReplaceChild("leg01_r1", CubeListBuilder.create().texOffs(22, 22).addBox(-4.0F, -3.0F, 0.0F, 2.0F, 2.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -3.0F, 0.0F, -0.6545F, -0.7854F, 0.0F));

		PartDefinition leg02 = partdefinition.addOrReplaceChild("leg02", CubeListBuilder.create(), PartPose.offset(-1.0F, 24.0F, 5.0F));

		PartDefinition leg02_r1 = leg02.addOrReplaceChild("leg02_r1", CubeListBuilder.create().texOffs(22, 22).addBox(-1.0F, -2.0F, -2.0F, 2.0F, 2.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -3.0F, 0.0F, -0.7418F, 0.0F, 0.0F));

		PartDefinition leg03 = partdefinition.addOrReplaceChild("leg03", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition leg03_r1 = leg03.addOrReplaceChild("leg03_r1", CubeListBuilder.create().texOffs(22, 22).addBox(4.0F, -2.0F, -1.0F, 2.0F, 2.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.0F, -3.0F, 5.0F, -0.8527F, 0.2236F, -0.2099F));

		PartDefinition leg04 = partdefinition.addOrReplaceChild("leg04", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition leg04_r1 = leg04.addOrReplaceChild("leg04_r1", CubeListBuilder.create().texOffs(0, 22).addBox(-4.0F, -3.0F, -8.0F, 2.0F, 2.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.0F, -3.0F, -6.0F, 0.6545F, 0.7854F, 0.0F));

		PartDefinition leg05 = partdefinition.addOrReplaceChild("leg05", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition leg05_r1 = leg05.addOrReplaceChild("leg05_r1", CubeListBuilder.create().texOffs(0, 22).addBox(-1.0F, -2.0F, -6.0F, 2.0F, 2.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.0F, -3.0F, -6.0F, 0.7418F, 0.0F, 0.0F));

		PartDefinition leg06 = partdefinition.addOrReplaceChild("leg06", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition leg06_r1 = leg06.addOrReplaceChild("leg06_r1", CubeListBuilder.create().texOffs(0, 22).addBox(4.0F, -2.0F, -7.0F, 2.0F, 2.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.0F, -3.0F, -6.0F, 0.8527F, -0.2236F, -0.2099F));

		return LayerDefinition.create(meshdefinition, 64, 32);
	}

	@Override
	public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		head.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		body.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		leg01.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		leg02.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		leg03.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		leg04.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		leg05.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		leg06.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}
}