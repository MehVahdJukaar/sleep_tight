package net.mehvahdjukaar.sleep_tight.forge;

import net.mehvahdjukaar.moonlight.api.platform.PlatformHelper;
import net.mehvahdjukaar.sleep_tight.ModEvents;
import net.mehvahdjukaar.sleep_tight.SleepTight;
import net.mehvahdjukaar.sleep_tight.SleepTightClient;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerSetSpawnEvent;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.event.entity.player.SleepingTimeCheckEvent;
import net.minecraftforge.event.level.SleepFinishedTimeEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * Author: MehVahdJukaar
 */
@Mod(SleepTight.MOD_ID)
public class SleepTightForge {

    public SleepTightForge() {
        SleepTight.commonInit();
        if (PlatformHelper.getEnv().isClient()) {
            SleepTightClient.init();
            MinecraftForge.EVENT_BUS.register(SleepTightForgeClient.class);
        }
        MinecraftForge.EVENT_BUS.register(this);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(SleepTightForge::setup);
    }

    public static void setup(final FMLCommonSetupEvent event) {
        event.enqueueWork(SleepTight::commonSetup);
    }

    @SubscribeEvent
    public void onSleepTimeCheck(SleepingTimeCheckEvent event) {
        var p = event.getSleepingLocation();
        if (p.isPresent()) {
            switch (ModEvents.onCheckSleepTime(event.getEntity().getLevel(), p.get())) {
                case FAIL -> event.setResult(Event.Result.DENY);
                case CONSUME, SUCCESS -> event.setResult(Event.Result.ALLOW);
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
            long newTime = ModEvents.getWakeTime(serverLevel, oldTime);

            if (oldTime != newTime) {
                evt.setTimeAddition(newTime);
            }
        }
    }

    @SubscribeEvent
    public void onPlayerWakeUp(PlayerWakeUpEvent evt) {
        ModEvents.onWokenUp(evt.getEntity());
    }

    @SubscribeEvent
    public void onSpawnSet(PlayerSetSpawnEvent evt) {
        if (evt.getSpawnLevel() == evt.getEntity().level.dimension()) {
            if (ModEvents.shouldCancelSetSpawn(evt.getEntity(), evt.getNewSpawn())) {
                evt.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!event.isCanceled()) {
            var ret = ModEvents.onRightClickBlock(event.getEntity(), event.getLevel(), event.getHand(), event.getHitVec());
            if (ret != InteractionResult.PASS) {
                event.setCanceled(true);
                event.setCancellationResult(ret);
            }
        }
    }

}

