package net.mehvahdjukaar.sleep_tight.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.mehvahdjukaar.sleep_tight.client.ClientEvents;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Shadow
    public abstract Camera getMainCamera();

    @Inject(method = "renderLevel", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GameRenderer;renderItemInHand(Lnet/minecraft/client/Camera;FLorg/joml/Matrix4f;)V",
            shift = At.Shift.BEFORE))
    public void sleep_tight$bedCameraHackOn(DeltaTracker deltaTracker, CallbackInfo ci) {
        ClientEvents.cameraHack = true;
    }

    @Inject(method = "renderLevel", at = @At(value = "TAIL"))
    public void sleep_tight$bedCameraHackOff(DeltaTracker deltaTracker, CallbackInfo ci) {
        ClientEvents.cameraHack = false;
    }

    @ModifyExpressionValue(method = "renderLevel", at = @At(value = "NEW",
            target = "()Lcom/mojang/blaze3d/vertex/PoseStack;"))
    public PoseStack sleep_tight$mainBedCameraHack(PoseStack matrixStack, @Local(argsOnly = true) DeltaTracker deltaTracker) {
        ClientEvents.rotateCameraOverHammockAxis(deltaTracker.getGameTimeDeltaPartialTick(true),
                matrixStack, this.getMainCamera());
        return matrixStack;
    }
}
