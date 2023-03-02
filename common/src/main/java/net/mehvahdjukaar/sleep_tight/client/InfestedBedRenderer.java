package net.mehvahdjukaar.sleep_tight.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector3f;
import it.unimi.dsi.fastutil.ints.Int2IntFunction;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.mehvahdjukaar.sleep_tight.SleepTight;
import net.mehvahdjukaar.sleep_tight.common.InfestedBedTile;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BrightnessCombiner;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoubleBlockCombiner;
import net.minecraft.world.level.block.entity.BedBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;

public class InfestedBedRenderer implements BlockEntityRenderer<InfestedBedTile> {
    private final ModelPart headRoot;
    private final ModelPart footRoot;

    public InfestedBedRenderer(BlockEntityRendererProvider.Context context) {
        this.headRoot = context.bakeLayer(ModelLayers.BED_HEAD);
        this.footRoot = context.bakeLayer(ModelLayers.BED_FOOT);
    }

    public void render(InfestedBedTile blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        Material material = Sheets.BED_TEXTURES[blockEntity.getColor().getId()];
        Level level = blockEntity.getLevel();
        if (level != null) {
            BlockState blockState = blockEntity.getBlockState();
            DoubleBlockCombiner.NeighborCombineResult<? extends InfestedBedTile> neighborCombineResult = DoubleBlockCombiner.combineWithNeigbour(SleepTight.INFESTED_BED_TILE.get(), BedBlock::getBlockType, BedBlock::getConnectedDirection, BedBlock.FACING, blockState, level, blockEntity.getBlockPos(), (levelAccessor, blockPos) -> false);
            int light = neighborCombineResult.apply(new BrightnessCombiner<>()).get(packedLight);
            this.renderPiece(poseStack, bufferSource, blockState.getValue(BedBlock.PART) == BedPart.HEAD ? this.headRoot : this.footRoot, blockState.getValue(BedBlock.FACING), material, light, packedOverlay, false);
        } else {
            this.renderPiece(poseStack, bufferSource, this.headRoot, Direction.SOUTH, material, packedLight, packedOverlay, false);
            this.renderPiece(poseStack, bufferSource, this.footRoot, Direction.SOUTH, material, packedLight, packedOverlay, true);
        }

    }

    /**
     * @param foot {@code true} if piece to render is the foot of the bed, {@code false} otherwise or if being rendered by a {@link net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer}
     */
    private void renderPiece(PoseStack poseStack, MultiBufferSource bufferSource, ModelPart modelPart, Direction direction, Material material, int packedLight, int packedOverlay, boolean foot) {
        poseStack.pushPose();
        poseStack.translate(0.0, 0.5625, foot ? -1.0 : 0.0);
        poseStack.mulPose(Vector3f.XP.rotationDegrees(90.0F));
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Vector3f.ZP.rotationDegrees(180.0F + direction.toYRot()));
        poseStack.translate(-0.5, -0.5, -0.5);
        VertexConsumer vertexConsumer = material.buffer(bufferSource, RenderType::entitySolid);
        modelPart.render(poseStack, vertexConsumer, packedLight, packedOverlay);
        poseStack.popPose();
    }
}
