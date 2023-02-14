package net.mehvahdjukaar.sleep_tight.forge;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.mehvahdjukaar.sleep_tight.common.BedEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Iterator;

import static net.minecraft.client.renderer.entity.LivingEntityRenderer.isEntityUpsideDown;

public class SleepTightForgeClient {

    @SubscribeEvent
    public static void onCameraSetup(ViewportEvent.ComputeCameraAngles event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && player.isSleeping()) {
            float partialTick = (float) event.getPartialTick();
            event.setPitch(player.getViewXRot(partialTick));
            event.setYaw(player.getViewYRot(partialTick));
        }
    }

    @SubscribeEvent
    public static void onRenderLiving(RenderLivingEvent.Pre event) {
        LivingEntity entity = event.getEntity();
        /*
        if (entity.getVehicle() instanceof BedEntity bed) {
            float partialTicks = event.getPartialTick();
            var renderer = event.getRenderer();
            var model = renderer.getModel();
            var buffer = event.getMultiBufferSource();
            PoseStack poseStack  =event.getPoseStack();
            var packedLight = event.getPackedLight();

            poseStack.pushPose();
            model.attackTime = renderer.getAttackAnim(entity, partialTicks);
            model.riding = false;
            model.young = entity.isBaby();
            float f = Mth.rotLerp(partialTicks, entity.yBodyRotO, entity.yBodyRot);
            float f1 = Mth.rotLerp(partialTicks, entity.yHeadRotO, entity.yHeadRot);
            float f2 = f1 - f;
            float f7;

            float f6 = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());
            if (isEntityUpsideDown(entity)) {
                f6 *= -1.0F;
                f2 *= -1.0F;
            }

            float f8;
            if (entity.hasPose(Pose.SLEEPING)) {
                Direction direction = entity.getBedOrientation();
                if (direction != null) {
                    f8 = entity.getEyeHeight(Pose.STANDING) - 0.1F;
                    poseStack.translate( ( (-direction.getStepX()) * f8), 0.0, ( (-direction.getStepZ()) * f8));
                }
            }

            f7 = renderer.getBob(entity, partialTicks);
            renderer.setupRotations(entity, poseStack, f7, f, partialTicks);
            poseStack.scale(-1.0F, -1.0F, 1.0F);
            renderer.scale(entity, poseStack, partialTicks);
            poseStack.translate(0.0, -1.5010000467300415, 0.0);
            f8 = 0.0F;
            float f5 = 0.0F;
            if (entity.isAlive()) {
                f8 = Mth.lerp(partialTicks, entity.animationSpeedOld, entity.animationSpeed);
                f5 = entity.animationPosition - entity.animationSpeed * (1.0F - partialTicks);
                if (entity.isBaby()) {
                    f5 *= 3.0F;
                }

                if (f8 > 1.0F) {
                    f8 = 1.0F;
                }
            }

            model.prepareMobModel(entity, f5, f8, partialTicks);
            model.setupAnim(entity, f5, f8, f7, f2, f6);
            Minecraft minecraft = Minecraft.getInstance();
            boolean flag = renderer.isBodyVisible(entity);
            boolean flag1 = !flag && !entity.isInvisibleTo(minecraft.player);
            boolean flag2 = minecraft.shouldEntityAppearGlowing(entity);
            RenderType rendertype = renderer.getRenderType(entity, flag, flag1, flag2);
            if (rendertype != null) {
                VertexConsumer vertexconsumer = buffer.getBuffer(rendertype);
                int i = getOverlayCoords(entity, renderer.getWhiteOverlayProgress(entity, partialTicks));
                model.renderToBuffer(poseStack, vertexconsumer, packedLight, i, 1.0F, 1.0F, 1.0F, flag1 ? 0.15F : 1.0F);
            }


            if (!entity.isSpectator()) {

                for (Object o : renderer.layers) {
                    RenderLayer<T, M> renderlayer = (RenderLayer) o;
                    renderlayer.render(poseStack, buffer, packedLight, entity, f5, f8, partialTicks, f7, f2, f6);
                }
            }

            poseStack.popPose();
            //no nametag
            super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
            MinecraftForge.EVENT_BUS.post(new RenderLivingEvent.Post(entity, this, partialTicks, poseStack, buffer, packedLight));
        }*/
    }
}
