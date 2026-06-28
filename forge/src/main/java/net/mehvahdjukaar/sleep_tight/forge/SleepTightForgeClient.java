package net.mehvahdjukaar.sleep_tight.forge;

import net.mehvahdjukaar.sleep_tight.SleepTight;
import net.mehvahdjukaar.sleep_tight.SleepTightClient;
import net.mehvahdjukaar.sleep_tight.client.SleepGuiOverlay;
import net.mehvahdjukaar.sleep_tight.common.entities.BedEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.InBedChatScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class SleepTightForgeClient {

    public static void init(IEventBus bus) {
        MinecraftForge.EVENT_BUS.register(SleepTightForgeClient.class);
        bus.addListener(SleepTightForgeClient::onAddGuiLayers);
    }

    @SubscribeEvent
    public static void onRenderGuiOverlayPre(RenderGuiOverlayEvent.Pre event) {
        var overlay = event.getOverlay();
        if (overlay == VanillaGuiOverlay.EXPERIENCE_BAR.type()) {
            if (SleepTightClient.getLayingBedData() != null) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onEntityTIck(LivingEvent.LivingTickEvent event) {
        SleepTightClient.onEntityTick(event.getEntity());
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

    public static void onAddGuiLayers(RegisterGuiOverlaysEvent event) {
        event.registerAbove(VanillaGuiOverlay.CROSSHAIR.id(), "sleep_indicator", new SleepGuiOverlayImpl());
    }


    public static class SleepGuiOverlayImpl extends SleepGuiOverlay<ForgeGui> implements IGuiOverlay {

        public SleepGuiOverlayImpl() {
        }

        @Override
        protected void setupOverlayRenderState(ForgeGui gui, boolean blend, boolean depthTest, ResourceLocation icons) {
            gui.setupOverlayRenderState(blend, depthTest);
        }
    }


}
