package net.mehvahdjukaar.sleep_tight.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import net.mehvahdjukaar.moonlight.api.misc.EventCalled;
import net.mehvahdjukaar.sleep_tight.common.entities.BedEntity;
import net.mehvahdjukaar.sleep_tight.common.tiles.HammockTile;
import net.mehvahdjukaar.sleep_tight.configs.ClientConfigs;
import net.mehvahdjukaar.sleep_tight.core.SleepEffectsHelper;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;

public class ClientEvents {

    public static boolean cameraHack;

    @EventCalled
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
        if (pos == null) return;
        if (entity.getLevel().getBlockEntity(pos) instanceof HammockTile tile) {

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

            //bed entity on bed
            var dir = BedBlock.getBedOrientation(entity.level, pos);
            if (dir != null) {
                float f1 = 90 - dir.toYRot();
                poseStack.mulPose(Vector3f.YP.rotationDegrees(f1));

                poseStack.translate(1.5, 0, 0);
            }
        }

    }

    @EventCalled
    public static void rotateCameraOverHammockAxis(float partialTicks, PoseStack matrixStack, Camera camera) {
        Minecraft mc = Minecraft.getInstance();


        var e = mc.getCameraEntity();
        if (e == null || !mc.options.getCameraType().isFirstPerson()) return;
        double intensity = ClientConfigs.CAMERA_ROLL_INTENSITY.get();
        if (intensity == 0) return;

        BlockPos pos = null;
        boolean onBedEntity = false;
        if (e.getVehicle() instanceof BedEntity bed) {
            onBedEntity = true;
            pos = bed.getOnPos();
        } else if (e instanceof Player p) {
            pos = p.getSleepingPos().orElse(null);
        }
        if (pos != null && e.getLevel().getBlockEntity(pos) instanceof HammockTile tile) {
            var q = camera.rotation().copy();
            q.conj();
            matrixStack.mulPose(q);
            float roll = (float) (tile.getRoll(partialTicks) * intensity);

            float o = 6 / 16f - tile.getPivotOffset();
            matrixStack.translate(0, -o, 0);

            matrixStack.mulPose(tile.getDirection().step().rotationDegrees(roll));

            matrixStack.translate(0, o, 0);
            matrixStack.mulPose(camera.rotation());
        }

        //previously in camera setup
        if (onBedEntity) {
            //same y offset as camera in bed
            matrixStack.translate(0, -0.3, 0);
        }
    }


    public static void onSleepStarted(Entity entity, BlockState state, BlockPos pos) {
        if (entity instanceof Player player) {
            BlockPos partnerPos = SleepEffectsHelper.getPartnerPos(player, state, pos);
            if (partnerPos != null) {
                entity.level.addParticle(ParticleTypes.HEART,
                        0.5 + (pos.getX() + partnerPos.getX()) / 2f,
                        0.6 + (pos.getY() + partnerPos.getY()) / 2f,
                        0.5 + (pos.getZ() + partnerPos.getZ()) / 2f, 0, 0, 0);
            }
        }
    }

    public static void displayRidingMessage(BedEntity bed) {
        Minecraft mc = Minecraft.getInstance();
        Component component = bed.getRidingMessage(mc.options.keyJump.getTranslatedKeyMessage(),
                mc.options.keyShift.getTranslatedKeyMessage());
        mc.gui.setOverlayMessage(component, false);
        mc.getNarrator().sayNow(component);
    }
}
