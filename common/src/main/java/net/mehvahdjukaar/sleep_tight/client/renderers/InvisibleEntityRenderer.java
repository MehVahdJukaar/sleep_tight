package net.mehvahdjukaar.sleep_tight.client.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;

public class InvisibleEntityRenderer extends EntityRenderer<Entity> {

	public InvisibleEntityRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@NotNull
	@Override
	public ResourceLocation getTextureLocation(@NotNull Entity entity) {
		return null;
	}

	@Override
	public boolean shouldRender(@NotNull Entity livingEntityIn, @NotNull Frustum camera, double camX, double camY, double camZ) {
		return true;
	}

	@Override
	public void render(Entity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
	}
}
