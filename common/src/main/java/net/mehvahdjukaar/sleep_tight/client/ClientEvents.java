package net.mehvahdjukaar.sleep_tight.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import net.mehvahdjukaar.sleep_tight.common.BedEntity;
import net.mehvahdjukaar.sleep_tight.common.HammockBlockEntity;
import net.mehvahdjukaar.sleep_tight.network.NetworkHandler;
import net.mehvahdjukaar.sleep_tight.network.ServerBoundCommitSleepMessage;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

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
            o += 0.125;
            Vector3f v = tile.getDirection().step();
            poseStack.translate(0, o, 0);
            poseStack.mulPose(v.rotationDegrees(roll));
            poseStack.translate(0, -o, 0);

            var mc = Minecraft.getInstance();

            if (bedEntity != null) {
                float f1 = 90 - tile.getDirection().toYRot();
                poseStack.mulPose(Vector3f.YP.rotationDegrees(f1));

                poseStack.translate(1.6125 - 3 / 32f, 0.125, 0);
            }
            //fixes random offset for local player in third person
            else if (entity == mc.player) {
                poseStack.translate(0, 0.125, 0);
            }
        } else if (bedEntity != null) {

            //bed

            var mc = Minecraft.getInstance();


            var dir = entity.getBedOrientation();
            float f1 = 90 + dir.toYRot();
            poseStack.mulPose(Vector3f.YP.rotationDegrees(f1));

            poseStack.translate(1.5, 0, 0);

            //fixes random offset for local player in third person
            if (entity == mc.player) {
                // poseStack.translate(0, 2 / 16f, 0);
            }
        } else {
            var mc = Minecraft.getInstance();

            if (entity == mc.player) {
              //  poseStack.translate(0, 0.125, 0);
            }
        }

    }


    public static void rotateCameraOverHammockAxis(float partialTicks, PoseStack matrixStack, Camera camera) {
        Minecraft mc = Minecraft.getInstance();
        var e = mc.getCameraEntity();
        if (e == null || !mc.options.getCameraType().isFirstPerson()) return;
        BlockPos pos = null;
        if (e.getVehicle() instanceof BedEntity bed) {
            pos = bed.getOnPos();
        } else if (e instanceof Player p) {
            pos = p.getSleepingPos().orElse(null);
        }
        if (pos != null && e.getLevel().getBlockEntity(pos) instanceof HammockBlockEntity tile) {
            var q = camera.rotation().copy();
            q.conj();
            matrixStack.mulPose(q);
            var yaw = tile.getRoll(partialTicks);

            float o = 6 / 16f - tile.getPivotOffset();
            matrixStack.translate(0, -o, 0);

            matrixStack.mulPose(tile.getDirection().step().rotationDegrees(yaw));

            matrixStack.translate(0, o, 0);
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

    public static void test(Entity entity, double x, double y, double z) {
        if (entity instanceof LocalPlayer) {
            if (y != -59.31248) {
                int aa = 1;
                // entity.setPosRaw(x, -60.5, z);
            }
        }
    }
}
