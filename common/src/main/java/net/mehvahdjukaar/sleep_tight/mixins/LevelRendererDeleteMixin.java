package net.mehvahdjukaar.sleep_tight.mixins;

import com.mojang.blaze3d.vertex.PoseStack;
import net.mehvahdjukaar.sleep_tight.client.ClientEvents;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class LevelRendererDeleteMixin {


    @Inject(method = "setPosRaw", at = @At(value = "TAIL"))
    public void test(double x, double y, double z, CallbackInfo ci) {
        ClientEvents.test((Entity) (Object)this, x,y, z);
    }

}
