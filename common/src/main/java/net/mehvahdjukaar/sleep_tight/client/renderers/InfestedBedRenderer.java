package net.mehvahdjukaar.sleep_tight.client.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.mehvahdjukaar.sleep_tight.common.tiles.InfestedBedTile;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BedPart;

public class InfestedBedRenderer implements BlockEntityRenderer<InfestedBedTile> {
    private final BlockEntityRenderDispatcher dispatcher;

    public InfestedBedRenderer(BlockEntityRendererProvider.Context context) {
        this.dispatcher = context.getBlockEntityRenderDispatcher();

    }

    @Override
    public void render(InfestedBedTile blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockEntity inner = blockEntity.getInner();
        if (inner != null) {
            renderInner(inner, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
        }
    }

    public <B extends BlockEntity> void renderInner(B tile, float pPartialTicks, PoseStack poseStack, MultiBufferSource buffer, int pCombinedLight, int pCombinedOverlay) {
        BlockEntityRenderer<B> renderer = dispatcher.getRenderer(tile);
        if (renderer != null) {
            if (tile.getBlockState().getValue(BedBlock.PART) == BedPart.FOOT)
                return;//not ideal. just here because level is null
            poseStack.translate(0.5, 0.5, 0.5);
            poseStack.mulPose(Axis.YP.rotationDegrees(-tile.getBlockState().getValue(BedBlock.FACING).toYRot()));
            poseStack.translate(-0.5, -0.5, -0.5);
            renderer.render(tile, pPartialTicks, poseStack, buffer, pCombinedLight, pCombinedOverlay);
        }
    }
}
