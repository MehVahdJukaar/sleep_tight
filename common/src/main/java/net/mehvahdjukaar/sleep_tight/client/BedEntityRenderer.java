package net.mehvahdjukaar.sleep_tight.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.mehvahdjukaar.sleep_tight.common.BedEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;

public class BedEntityRenderer extends EntityRenderer<BedEntity> {

	public BedEntityRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Nonnull
	@Override
	public ResourceLocation getTextureLocation(@Nonnull BedEntity entity) {
		return null;
	}

	@Override
	public boolean shouldRender(@Nonnull BedEntity livingEntityIn, @Nonnull Frustum camera, double camX, double camY, double camZ) {
		return false;
	}

	@Override
	public void render(BedEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
	}
}
