package net.mehvahdjukaar.sleep_tight.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.mehvahdjukaar.moonlight.api.platform.PlatHelper;
import net.mehvahdjukaar.moonlight.fabric.MLFabricSetupCallbacks;
import net.mehvahdjukaar.sleep_tight.SleepTight;
import net.mehvahdjukaar.sleep_tight.SleepTightClient;
import net.mehvahdjukaar.sleep_tight.SleepTightPlatformStuff;
import net.mehvahdjukaar.sleep_tight.common.blocks.HammockBlock;
import net.mehvahdjukaar.sleep_tight.core.ModEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class SleepTightFabric implements ModInitializer {

    @Override
    public void onInitialize() {

        SleepTight.commonInit();

        if (PlatHelper.getPhysicalSide().isClient()) {
            SleepTightClient.init();
            SleepTightFabricClient.init();
        }

        UseBlockCallback.EVENT.register(ModEvents::onRightClickBlock);

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
            if (!alive) {
                var oldData = SleepTightPlatformStuff.getPlayerSleepData(oldPlayer);
                var newData = SleepTightPlatformStuff.getPlayerSleepData(newPlayer);
                newData.copyFrom(oldData);
            }
        });

        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((world, entity, killedEntity) -> {
            ModEvents.onEntityKilled(killedEntity, entity);
        });


    }

}
