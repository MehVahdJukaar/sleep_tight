package net.mehvahdjukaar.sleep_tight.forge;

import net.mehvahdjukaar.sleep_tight.common.BedEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.InBedChatScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public class SleepTightForgeClient {

    public static void init() {
        MinecraftForge.EVENT_BUS.register(SleepTightForgeClient.class);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(SleepTightForgeClient::onAddGuiLayers);
    }

    @SubscribeEvent
    public static void onCameraSetup(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player != null && player.getVehicle() instanceof BedEntity && mc.options.getCameraType().isFirstPerson()) {
            //same y offset as camera in bed
            event.getCamera().move(0, 0.3, 0);
        }
    }

    @SubscribeEvent
    public static void onRenderScreen(ScreenEvent.Render.Post event) {
        if (event.getScreen() instanceof InBedChatScreen s) {
            SleepGuiOverlay.renderBedScreenOverlay(s, event.getPoseStack(), event.getMouseX(), event.getMouseY());
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
        event.registerAbove(VanillaGuiOverlay.CROSSHAIR.id(), "sleep_indicator", new SleepGuiOverlay());
    }



}
