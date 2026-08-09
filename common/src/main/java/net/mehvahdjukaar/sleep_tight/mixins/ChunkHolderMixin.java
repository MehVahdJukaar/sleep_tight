package net.mehvahdjukaar.sleep_tight.mixins;

import net.mehvahdjukaar.sleep_tight.STPlatStuff;
import net.mehvahdjukaar.sleep_tight.common.network.ClientBoundSyncBedCapMessage;
import net.mehvahdjukaar.sleep_tight.common.network.ModNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//Neither capabilities nor fabric attachments travel with the chunk packet, so a client that just loaded a
//chunk has no bed data and infested beds stop showing their particles. Piggyback on the block entity data
//that IS sent and push our own packet a tick later, once the client actually has the chunk
@Mixin(targets = "net/minecraft/network/protocol/game/ClientboundLevelChunkPacketData$BlockEntityInfo")
public abstract class ChunkHolderMixin {

    @Inject(method = "create(Lnet/minecraft/world/level/block/entity/BlockEntity;)Lnet/minecraft/network/protocol/game/ClientboundLevelChunkPacketData$BlockEntityInfo;",
            at = @At("HEAD"))
    private static void sleep_tight$syncBedDataOnChunkSend(BlockEntity te, CallbackInfoReturnable<?> cir) {
        if (te != null && te.getLevel() instanceof ServerLevel serverLevel) {
            MinecraftServer server = serverLevel.getServer();
            BlockPos pos = te.getBlockPos();

            server.tell(new TickTask(server.getTickCount(), () -> {
                var cap = STPlatStuff.getBedDataFromThis(te);
                if (cap != null) {
                    ServerChunkCache chunkSource = serverLevel.getChunkSource();
                    chunkSource.chunkMap.getPlayers(new ChunkPos(pos), false).forEach(p ->
                            ModNetworking.CHANNEL.sendToClientPlayer(p, new ClientBoundSyncBedCapMessage(pos, cap)));
                }
            }));
        }
    }
}
