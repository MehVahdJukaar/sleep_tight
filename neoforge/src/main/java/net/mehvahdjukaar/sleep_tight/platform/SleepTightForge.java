package net.mehvahdjukaar.sleep_tight.platform;

import net.mehvahdjukaar.moonlight.api.platform.PlatHelper;
import net.mehvahdjukaar.moonlight.api.platform.RegHelper;
import net.mehvahdjukaar.sleep_tight.STPlatStuff;
import net.mehvahdjukaar.sleep_tight.SleepTight;
import net.mehvahdjukaar.sleep_tight.SleepTightClient;
import net.mehvahdjukaar.sleep_tight.common.InvigoratedEffect;
import net.mehvahdjukaar.sleep_tight.core.BedbugSpawner;
import net.mehvahdjukaar.sleep_tight.core.ModEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.portal.DimensionTransition;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.*;
import net.neoforged.neoforge.event.level.*;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Author: MehVahdJukaar
 */
@Mod(SleepTight.MOD_ID)
public class SleepTightForge {

    public SleepTightForge(IEventBus bus) {
        RegHelper.startRegisteringFor(bus);
        SleepTight.commonInit();

        if (PlatHelper.getPhysicalSide().isClient()) {
            SleepTightClient.init();
            SleepTightForgeClient.init(bus);
        }

        NeoForge.EVENT_BUS.register(this);
        bus.addListener(SleepTightForge::setup);
    }

    public static void setup(final FMLCommonSetupEvent event) {
        event.enqueueWork(SleepTight::commonSetup);
    }


    @SubscribeEvent
    public void onModifyCustomSpawners(ModifyCustomSpawnersEvent event) {
        event.addCustomSpawner(new BedbugSpawner());
    }

    @SubscribeEvent
    public void onPlayerRespawnPositionCheck(PlayerRespawnPositionEvent event) {
        DimensionTransition transition = event.getDimensionTransition();
        if (event.getEntity() instanceof ServerPlayer sp && ModEvents.shouldCancelRespawnHere(event.getEntity(), transition)) {
            //respawn out of bed
            event.setDimensionTransition(
                    DimensionTransition.missingRespawnBlock(sp.server.overworld(),
                            sp, transition.postDimensionTransition()));
        }
    }

    @SubscribeEvent
    public void onSleepConditionCheck(CanPlayerSleepEvent event) {
        if (!ModEvents.checkExtraSleepConditions(event.getEntity(), event.getPos())) {
            event.setProblem(Player.BedSleepingProblem.OTHER_PROBLEM);
            return;
        }
        switch (ModEvents.onCheckSleepTime(event.getEntity().level(), event.getPos())) {
            case FAIL -> {
                if (event.getVanillaProblem() == null)
                    event.setProblem(Player.BedSleepingProblem.NOT_POSSIBLE_NOW);
            }
            case CONSUME, SUCCESS -> {
                if (event.getVanillaProblem() == Player.BedSleepingProblem.NOT_POSSIBLE_NOW)
                    event.setProblem(null);
            }
        }
    }

    @SubscribeEvent
    public void canContinueSleeping(CanContinueSleepingEvent event) {
        var p = event.getEntity().getSleepingPos();
        if (p.isPresent()) {
            switch (ModEvents.onCheckSleepTime(event.getEntity().level(), p.get())) {
                case FAIL -> event.setContinueSleeping(false);
                case CONSUME, SUCCESS -> event.setContinueSleeping(true);
            }
        }
    }

    @SubscribeEvent
    public void onPlayerSetSpawn(PlayerSetSpawnEvent evt) {
        if (!ModEvents.canSetSpawn(evt.getEntity(), evt.getNewSpawn())) {
            evt.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onSleepFinished(SleepFinishedTimeEvent evt) {
        if (evt.getLevel() instanceof ServerLevel serverLevel) {
            long oldTime = evt.getNewTime();
            long newTime = ModEvents.getWakeUpTimeWhenSlept(serverLevel, oldTime);

            if (oldTime != newTime) {
                evt.setTimeAddition(newTime);
            }
        }
    }

    @SubscribeEvent
    public void onPlayerWakeUp(PlayerWakeUpEvent evt) {
        ModEvents.onWokenUp(evt.getEntity(), evt.updateLevel());
    }

    @SubscribeEvent
    public void onSpawnSet(PlayerSetSpawnEvent evt) {
        if (evt.getSpawnLevel() == evt.getEntity().level().dimension()) {
            BlockPos newSpawn = evt.getNewSpawn();
            if (newSpawn != null && ModEvents.shouldCancelSetSpawn(evt.getEntity(), newSpawn)) {
                evt.setCanceled(true);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!event.isCanceled()) {
            InteractionResult ret = ModEvents.onRightClickBlock(event.getEntity(), event.getLevel(), event.getHand(), event.getHitVec());
            if (ret != null) {
                event.setCanceled(true);
                event.setCancellationResult(ret);
            }
        }
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ModEvents.onPlayerRespawned(player);
        }
    }

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player instanceof ServerPlayer sp) {
            var sleepData = STPlatStuff.getPlayerSleepData(player);
            sleepData.tick(sp);
        }
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        ModEvents.onEntityKilled(event.getEntity(), event.getSource().getEntity());
    }

    @SubscribeEvent
    public void onBlockBreak(BlockDropsEvent event) {
        if (event.getLevel() instanceof ServerLevel sl)
            ModEvents.spawnAfterBreakBed(event.getState(), sl, event.getPos(), null);
        int i = event.getDroppedExperience();
        if (i > 0) {
            int j = InvigoratedEffect.forgeGetExtraXpForBlockBroken(i, event.getBreaker());
            if (j != 0) event.setDroppedExperience(i + j);
        }
    }

}

