package net.mehvahdjukaar.sleep_tight.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.mehvahdjukaar.moonlight.api.platform.PlatHelper;
import net.mehvahdjukaar.sleep_tight.STPlatStuff;
import net.mehvahdjukaar.sleep_tight.SleepTight;
import net.mehvahdjukaar.sleep_tight.SleepTightClient;
import net.mehvahdjukaar.sleep_tight.common.blocks.HammockBlock;
import net.mehvahdjukaar.sleep_tight.core.ModEvents;
import net.mehvahdjukaar.sleep_tight.core.PlayerSleepData;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class SleepTightFabric implements ModInitializer {

    //TODO:
    public static AttachmentType<PlayerSleepData> PLAYER_SLEEP_DATA;

    @Override
    public void onInitialize() {

        SleepTight.commonInit();
        DumbTaskScheduler.init();

        if (PlatHelper.getPhysicalSide().isClient()) {
            SleepTightClient.init();
            SleepTightFabricClient.init();
        }

        //yes not ideal at all. if done in after we might not have the block entity
        PlayerBlockBreakEvents.BEFORE.register((level, player, blockPos, blockState, blockEntity) -> {
            if (level instanceof ServerLevel sl)
                ModEvents.spawnAfterBreakBed(blockState, sl, blockPos, blockEntity);
            return true;
        });
        ServerBlockEntityEvents.BLOCK_ENTITY_LOAD.register((blockEntity, serverLevel) -> {
            //initialize attachments
            if (ModEvents.shouldHaveBedData(blockEntity)) {
                //Thanks fabric
                //https://github.com/FabricMC/fabric/issues/4718
                //Thanks fabric. Without this it just deadlocks the game LMAO. GG
                //both attachments and cap api seems failed systems to me, without synching and even issues like these, I should just use mixins next time

                int ticTime = serverLevel.getServer().getTickCount() + 1;
                DumbTaskScheduler.schedule(new TickTask(ticTime, () -> {
                    //if(true)return;
                    SleepTight.BED_DATA.getOrCreate(blockEntity);
                }));
                //blockEntity.getAttachedOrCreate(BED_DATA);
            }
        });
        UseBlockCallback.EVENT.register((player, level, interactionHand, blockHitResult) -> {
            var ret = ModEvents.onRightClickBlock(player, level, interactionHand, blockHitResult);
            return ret == null ? InteractionResult.PASS : ret;
        });

        EntitySleepEvents.ALLOW_SETTING_SPAWN.register(ModEvents::canSetSpawn);
        EntitySleepEvents.STOP_SLEEPING.register((a, b) -> {
            if (a instanceof Player p) ModEvents.onWokenUp(p, true);
        });

        EntitySleepEvents.ALLOW_SLEEP_TIME.register(((player, sleepingPos, vanillaResult) ->
                ModEvents.onCheckSleepTime(player.level(), sleepingPos)));

        EntitySleepEvents.ALLOW_SLEEPING.register((player, pos) -> {
            if (!ModEvents.checkExtraSleepConditions(player, pos)) {
                return Player.BedSleepingProblem.OTHER_PROBLEM;
            }
            return null;
        });

        EntitySleepEvents.ALLOW_BED.register((entity, sleepingPos, state, vanillaResult) -> {
            if (state.getBlock() instanceof HammockBlock) {
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        });

        EntitySleepEvents.MODIFY_SLEEPING_DIRECTION.register((entity, sleepingPos, sleepingDirection) -> {
            Level level = entity.level();
            BlockState state = level.getBlockState(sleepingPos);
            if (state.getBlock() instanceof HammockBlock hb) {
                return hb.getBedDirection(state, level, sleepingPos);
            }
            return sleepingDirection;
        });

        ServerPlayConnectionEvents.JOIN.register((l, s, m) -> ModEvents.onPlayerLoggedIn(l.player));

        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
            //if (!alive) {
            var oldData = STPlatStuff.getPlayerSleepData(oldPlayer);
            var newData = STPlatStuff.getPlayerSleepData(newPlayer);
            newData.copyFrom(oldData);
            //just server. we must sync
            newData.syncToClient(newPlayer);
            //}
        });

        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            ModEvents.onPlayerRespawned(newPlayer);
        });

        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((world, entity, killedEntity) -> {
            ModEvents.onEntityKilled(killedEntity, entity);
        });


    }


}
