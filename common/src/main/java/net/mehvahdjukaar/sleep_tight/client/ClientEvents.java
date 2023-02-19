package net.mehvahdjukaar.sleep_tight.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import net.mehvahdjukaar.sleep_tight.common.BedEntity;
import net.mehvahdjukaar.sleep_tight.common.HammockBlockEntity;
import net.mehvahdjukaar.sleep_tight.network.NetworkHandler;
import net.mehvahdjukaar.sleep_tight.network.ServerBoundCommitSleepMessage;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;

public class ClientEvents {

    public static boolean cameraHack;

    public static <T extends LivingEntity> void rotatePlayerInBed(T entity, PoseStack poseStack, float partialTicks,
                                                                  MultiBufferSource bufferSource) {
        BlockPos pos = null;
        var p = entity.getSleepingPos();
        BedEntity bedEntity = null;
        if (p.isPresent()) {
            pos = p.get();
        } else if (entity.getVehicle() instanceof BedEntity be) {
            pos = be.getOnPos();
            bedEntity = be;
        }
        if (pos != null && entity.getLevel().getBlockEntity(pos) instanceof HammockBlockEntity tile) {

            float roll = tile.getRoll(partialTicks);

            float o = tile.getPivotOffset(); //hammock pivot
            Vector3f v = tile.getDirection().step();
            poseStack.translate(0, o, 0);
            poseStack.mulPose(v.rotationDegrees(roll));
            poseStack.translate(0, -o, 0);

            var mc = Minecraft.getInstance();

            if (bedEntity != null) {
                float f1 = 90 - tile.getDirection().toYRot();
                poseStack.mulPose(Vector3f.YP.rotationDegrees(f1));

                poseStack.translate(1.6125, 0, 0);
            }
            //fixes random offset for local player in third person
            else if (entity == mc.player) {
                poseStack.translate(0, 0.125, 0);
            }
        }
    }



    public static void rotateCameraOverHammockAxis(float partialTicks, PoseStack matrixStack, Camera camera) {
        var e = Minecraft.getInstance().getCameraEntity();
        if (e != null && e.getVehicle() instanceof BedEntity bed && bed.getBedTile() instanceof HammockBlockEntity tile) {
            var q = camera.rotation().copy();
            q.conj();
            matrixStack.mulPose(q);
            var yaw = tile.getRoll(partialTicks);

            float a = 6 / 16f - tile.getPivotOffset();
            matrixStack.translate(0, -a, 0);

            matrixStack.mulPose(tile.getDirection().step().rotationDegrees(yaw));

            matrixStack.translate(0, a, 0);
            matrixStack.mulPose(camera.rotation());
        }
    }

    public static void playerSleepCommit(BedEntity bedEntity) {
        var player = Minecraft.getInstance().player;
        if (player != null) {
            bedEntity.startSleepingOn(player);
        }
        NetworkHandler.CHANNEL.sendToServer(new ServerBoundCommitSleepMessage());
    }

}
