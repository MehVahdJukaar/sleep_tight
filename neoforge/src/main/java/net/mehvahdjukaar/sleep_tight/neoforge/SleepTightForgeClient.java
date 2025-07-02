package net.mehvahdjukaar.sleep_tight.neoforge;

import net.mehvahdjukaar.sleep_tight.SleepTight;
import net.mehvahdjukaar.sleep_tight.SleepTightClient;
import net.mehvahdjukaar.sleep_tight.client.SleepGuiOverlay;
import net.mehvahdjukaar.sleep_tight.common.entities.BedEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.InBedChatScreen;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

public class SleepTightForgeClient {

    public static void init(IEventBus bus) {
        NeoForge.EVENT_BUS.register(SleepTightForgeClient.class);
        bus.addListener(SleepTightForgeClient::onAddGuiLayers);
    }

    @SubscribeEvent
    public static void onRenderGuiOverlayPre(RenderGuiLayerEvent.Pre event) {
        var overlay = event.getName();
        if (overlay == VanillaGuiLayers.EXPERIENCE_BAR) {
            if (SleepTightClient.getLayingBedData() != null) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onEntityTIck(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof LivingEntity le)
            SleepTightClient.onEntityTick(le);
    }

    @SubscribeEvent
    public static void onRenderScreen(ScreenEvent.Render.Post event) {
        if (event.getScreen() instanceof InBedChatScreen s) {
            SleepGuiOverlay.renderBedScreenOverlay(s, event.getGuiGraphics(), event.getMouseX(), event.getMouseY());
        }
    }

    @SubscribeEvent
    public static void renderPlayer(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        Minecraft mc = Minecraft.getInstance();
        if (player == mc.player &&
                mc.options.getCameraType().isFirstPerson() &&
                player.getVehicle() instanceof BedEntity) {
            event.getRenderer().getModel().head.visible = false;
        }
    }

    public static void onAddGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CROSSHAIR, SleepTight.res("sleep_indicator"),
                new SleepGuiOverlay(Minecraft.getInstance()));
    }


}
