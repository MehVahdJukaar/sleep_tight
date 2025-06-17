package net.mehvahdjukaar.sleep_tight.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import net.mehvahdjukaar.sleep_tight.STPlatStuff;
import net.mehvahdjukaar.sleep_tight.common.network.ClientBoundSyncBedCapMessage;
import net.mehvahdjukaar.sleep_tight.common.network.NetworkHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ChunkHolder.class)
public class ChunkHolderMixin {

    @Inject(method = "broadcastBlockEntity", at = @At("TAIL"))
    private void syncBedAtt(List<ServerPlayer> list, Level arg, BlockPos pos, CallbackInfo ci, @Local BlockEntity te) {
        if (te != null) {
            var cap = STPlatStuff.getBedDataFromThis(te);
            if (cap != null) {
                list.forEach(p -> NetworkHandler.CHANNEL.sendToClientPlayer(p, new ClientBoundSyncBedCapMessage(pos, cap)));
            }
        }
    }
}
