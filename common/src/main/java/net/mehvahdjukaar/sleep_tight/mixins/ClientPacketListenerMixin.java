package net.mehvahdjukaar.sleep_tight.mixins;

import net.mehvahdjukaar.sleep_tight.common.BedEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {

    @Shadow @Final private Minecraft minecraft;

    @Inject(method = "handleSetEntityPassengersPacket", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/Component;translatable(Ljava/lang/String;[Ljava/lang/Object;)Lnet/minecraft/network/chat/MutableComponent;",
    shift = At.Shift.BEFORE), locals = LocalCapture.CAPTURE_FAILHARD, cancellable = true)
    public void mountBed(ClientboundSetPassengersPacket packet, CallbackInfo ci, Entity vehicle, boolean bl, int[] var4, int var5, int var6, int i, Entity entity2){
        //hack since beds can only have 1 passenger, so we can cancel
        if(vehicle instanceof BedEntity bed){
            Component component = bed.getRidingMessage(this.minecraft.options.keyJump.getTranslatedKeyMessage());
            this.minecraft.gui.setOverlayMessage(component, false);
            this.minecraft.getNarrator().sayNow(component);
            ci.cancel();
        }
    }
}
