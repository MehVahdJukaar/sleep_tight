package net.mehvahdjukaar.sleep_tight.fabric;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.mehvahdjukaar.sleep_tight.SleepTightClient;
import net.mehvahdjukaar.sleep_tight.client.SleepGuiOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.InBedChatScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;

public class SleepTightFabricClient {

    public static void init() {
        ScreenEvents.BEFORE_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof InBedChatScreen s) {
                SleepGuiOverlay.setupOverlay(s);
                ScreenEvents.beforeRender(s).register((screen1, matrices, mouseX, mouseY, tickDelta) -> {
                    SleepGuiOverlay.renderBedScreenOverlay(s, matrices, mouseX, mouseY);
                });
            }
        });

        var overlay = new SleepGuiOverlayImpl(Minecraft.getInstance());
        HudRenderCallback.EVENT.register(overlay::render);
    }

    private static class SleepGuiOverlayImpl extends SleepGuiOverlay {

        public SleepGuiOverlayImpl(Minecraft minecraft) {
            super(minecraft);
        }

        @Override
        protected void setupOverlayRenderState(GuiGraphics graphics, boolean blend, boolean depthTest, ResourceLocation texture) {
            if (blend) {
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
            } else {
                RenderSystem.disableBlend();
            }

            if (depthTest) {
                RenderSystem.enableDepthTest();
            } else {
                RenderSystem.disableDepthTest();
            }

            if (texture != null) {
                //RenderSystem.enableTexture();
                RenderSystem.setShaderTexture(0, texture);
            } else {
                //RenderSystem.disableTexture();
            }

            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
        }
    }
}
