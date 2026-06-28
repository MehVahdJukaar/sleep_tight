package net.mehvahdjukaar.sleep_tight.fabric;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientBlockEntityEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.mehvahdjukaar.sleep_tight.client.SleepGuiOverlay;
import net.mehvahdjukaar.sleep_tight.core.ModEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.InBedChatScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;

import static net.mehvahdjukaar.sleep_tight.fabric.SleepTightFabric.BED_DATA;

public class SleepTightFabricClient {

    public static void init() {
        ScreenEvents.BEFORE_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof InBedChatScreen s) {
                ScreenEvents.beforeRender(s).register((screen1, matrices, mouseX, mouseY, tickDelta) -> {
                    SleepGuiOverlay.renderBedScreenOverlay(s, matrices, mouseX, mouseY);
                });
            }
        });

        ClientBlockEntityEvents.BLOCK_ENTITY_LOAD.register((blockEntity, clientLevel) -> {
            //initialize attachments
            if (ModEvents.shouldHaveBedData(blockEntity)) {
                blockEntity.getAttachedOrCreate(BED_DATA);
            }
        });
        var overlay = new SleepGuiOverlayImpl();
        HudRenderCallback.EVENT.register(overlay::render);
    }

    private static class SleepGuiOverlayImpl extends SleepGuiOverlay<Gui> {

        @Override
        protected void setupOverlayRenderState(Gui gui, boolean blend, boolean depthTest, ResourceLocation texture) {
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
                RenderSystem.setShaderTexture(0, texture);
            }

            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
        }

        public void render(GuiGraphics graphics, float partialTicks) {
            Minecraft mc = Minecraft.getInstance();
            render(mc.gui, graphics, partialTicks, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
        }
    }
}
