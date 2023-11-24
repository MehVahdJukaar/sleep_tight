package net.mehvahdjukaar.sleep_tight.mixins;

import net.mehvahdjukaar.sleep_tight.SleepTightClient;
import net.mehvahdjukaar.sleep_tight.configs.ClientConfigs;
import net.minecraft.client.renderer.entity.VillagerRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VillagerRenderer.class)
public class VillagerRendererMixin {

    @Inject(method = "getTextureLocation(Lnet/minecraft/world/entity/npc/Villager;)Lnet/minecraft/resources/ResourceLocation;",
    at = @At("HEAD"), cancellable = true)
    public void getTextureLocation(Villager entity, CallbackInfoReturnable<ResourceLocation> cir) {
        if(entity.isSleeping() && ClientConfigs.VILLAGER_SLEEP.get())cir.setReturnValue(SleepTightClient.SLEEPING_VILLAGER_TEXTURE);
    }
}
