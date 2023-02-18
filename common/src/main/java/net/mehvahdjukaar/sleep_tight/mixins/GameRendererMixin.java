package net.mehvahdjukaar.sleep_tight.mixins;

import com.mojang.blaze3d.vertex.PoseStack;
import net.mehvahdjukaar.sleep_tight.SleepTightClient;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Shadow public abstract Camera getMainCamera();

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;renderItemInHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/Camera;F)V",
    shift = At.Shift.BEFORE), require = 1)
    public void bedCameraHackOn(float partialTicks, long finishTimeNano, PoseStack matrixStack, CallbackInfo ci) {
        SleepTightClient.cameraHack = true;
    }

    @Inject(method = "renderLevel", at = @At(value = "TAIL"))
    public void bedCameraHackOff(float partialTicks, long finishTimeNano, PoseStack matrixStack, CallbackInfo ci) {
        SleepTightClient.cameraHack = false;
    }

    @Inject(method = "renderLevel", at = @At(value = "HEAD"))
    public void mainBedCameraHack(float partialTicks, long finishTimeNano, PoseStack matrixStack, CallbackInfo ci) {
        SleepTightClient.rotateCameraOverHammockAxis(partialTicks, matrixStack, this.getMainCamera());
    }
}
