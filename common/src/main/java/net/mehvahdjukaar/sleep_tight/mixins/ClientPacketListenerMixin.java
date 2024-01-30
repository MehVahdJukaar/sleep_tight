package net.mehvahdjukaar.sleep_tight.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.mehvahdjukaar.sleep_tight.common.entities.BedEntity;
import net.mehvahdjukaar.sleep_tight.configs.CommonConfigs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
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

    @WrapOperation(method = "handleSetEntityPassengersPacket", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/network/chat/Component;translatable(Ljava/lang/String;[Ljava/lang/Object;)Lnet/minecraft/network/chat/MutableComponent;"))
    public MutableComponent displayBedRidingMessage(String message, Object[] arg, Operation<MutableComponent> translatable,
                                                    @Local(ordinal = 0) Entity vehicle) {
        //hack since beds can only have one passenger, so we can cancel
        if (vehicle instanceof BedEntity bed) {
            if (!CommonConfigs.SLEEP_IMMEDIATELY.get()) {
                Options options = Minecraft.getInstance().options;
                return bed.getRidingMessage(options.keyJump.getTranslatedKeyMessage(),
                        options.keyShift.getTranslatedKeyMessage());
            }//technically not needed on forge since... idk events i think
        }
        return translatable.call(message, arg);
    }
}
