package net.mehvahdjukaar.sleep_tight.forge;

import net.mehvahdjukaar.sleep_tight.SleepTight;
import net.mehvahdjukaar.sleep_tight.client.SleepGuiOverlay;
import net.mehvahdjukaar.sleep_tight.common.entities.BedEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.InBedChatScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterGuiOverlaysEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.gui.overlay.ExtendedGui;
import net.neoforged.neoforge.client.gui.overlay.IGuiOverlay;
import net.neoforged.neoforge.client.gui.overlay.VanillaGuiOverlay;
import net.neoforged.neoforge.common.NeoForge;

public class SleepTightForgeClient {

    public static void init(IEventBus bus) {
        NeoForge.EVENT_BUS.register(SleepTightForgeClient.class);
        bus.addListener(SleepTightForgeClient::onAddGuiLayers);
    }

    @SubscribeEvent
    public static void onRenderScreen(ScreenEvent.Render.Post event) {
        if (event.getScreen() instanceof InBedChatScreen s) {
            SleepGuiOverlay.renderBedScreenOverlay(s, event.getGuiGraphics(), event.getMouseX(), event.getMouseY());
        }
    }

    @SubscribeEvent
    public static void onInitScreen(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof InBedChatScreen s) {
            SleepGuiOverlay.setupOverlay(s);
        }
    }

    @SubscribeEvent
    public static void renderPlayer(RenderPlayerEvent.Pre event){
        Player player = event.getEntity();
        Minecraft mc = Minecraft.getInstance();
        if(player == mc.player &&
                mc.options.getCameraType().isFirstPerson() &&
                player.getVehicle() instanceof BedEntity ){
            event.getRenderer().getModel().head.visible = false;
        }
    }

    public static void onAddGuiLayers(RegisterGuiOverlaysEvent event) {
        event.registerAbove(VanillaGuiOverlay.CROSSHAIR.id(),
                SleepTight.res("sleep_indicator"), new SleepGuiOverlayImpl());
    }



    public static class SleepGuiOverlayImpl extends SleepGuiOverlay<ExtendedGui> implements IGuiOverlay {

        public SleepGuiOverlayImpl() {
        }

        @Override
        protected void setupOverlayRenderState(ExtendedGui gui, boolean blend, boolean depthTest, ResourceLocation icons) {
            gui.setupOverlayRenderState(blend, depthTest);
        }
    }


}
