package net.mehvahdjukaar.sleep_tight.forge;

import net.mehvahdjukaar.moonlight.api.platform.PlatHelper;
import net.mehvahdjukaar.sleep_tight.SleepTight;
import net.mehvahdjukaar.sleep_tight.SleepTightClient;
import net.mehvahdjukaar.sleep_tight.SleepTightPlatformStuff;
import net.mehvahdjukaar.sleep_tight.common.InvigoratedEffect;
import net.mehvahdjukaar.sleep_tight.core.ModEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.SleepFinishedTimeEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
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

        if (PlatHelper.getPhysicalSide().isClient()) {
            SleepTightClient.init();
            SleepTightForgeClient.init();
        }

        MinecraftForge.EVENT_BUS.register(this);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(SleepTightForge::setup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(SleepTightForge::registerCaps);
    }

    public static void setup(final FMLCommonSetupEvent event) {
        event.enqueueWork(SleepTight::commonSetup);
    }


    public static void registerCaps(RegisterCapabilitiesEvent event) {
        event.register(ForgePlayerSleepCapability.class);
    }

    @SubscribeEvent
    public void attachPlayerCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(SleepTight.res("player_data"), new ForgePlayerSleepCapability());
        }
    }

    @SubscribeEvent
    public void onSleepConditionCheck(PlayerSleepInBedEvent event) {
        if (!ModEvents.checkExtraSleepConditions(event.getEntity(), event.getPos())) {
            event.setResult(Player.BedSleepingProblem.OTHER_PROBLEM);
        }
    }

    @SubscribeEvent
    public void onSleepTimeCheck(SleepingTimeCheckEvent event) {
        var p = event.getSleepingLocation();
        if (p.isPresent()) {
            switch (ModEvents.onCheckSleepTime(event.getEntity().level(), p.get())) {
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
            var ret = ModEvents.onRightClickBlock(event.getEntity(), event.getLevel(), event.getHand(), event.getHitVec());
            if (ret != InteractionResult.PASS) {
                event.setCanceled(true);
                event.setCancellationResult(ret);
            }
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ModEvents.onPlayerLoggedIn(player);
        }
    }

    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            Player old = event.getOriginal();
            old.reviveCaps();
            var oldData = SleepTightPlatformStuff.getPlayerSleepData(old);
            var newData = SleepTightPlatformStuff.getPlayerSleepData(event.getEntity());
            newData.copyFrom(oldData);
            old.invalidateCaps();
        }
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        ModEvents.onEntityKilled(event.getEntity(), event.getSource().getEntity());
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        int i = event.getExpToDrop();
        if (i > 0) {
            int j = InvigoratedEffect.onBlockBreak(i, event.getPlayer());
            if (j != 0) event.setExpToDrop(i + j);
        }
    }

}

