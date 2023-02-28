package net.mehvahdjukaar.sleep_tight.forge;


import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.mehvahdjukaar.moonlight.api.util.math.MthUtils;
import net.mehvahdjukaar.sleep_tight.SleepTightClient;
import net.mehvahdjukaar.sleep_tight.SleepTightPlatformStuff;
import net.mehvahdjukaar.sleep_tight.core.BedData;
import net.mehvahdjukaar.sleep_tight.common.NightBagBlock;
import net.mehvahdjukaar.sleep_tight.core.PlayerSleepData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.InBedChatScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerXpEvent;

import java.util.ArrayList;

public class SleepGuiOverlay implements IGuiOverlay {

    public SleepGuiOverlay() {
    }

    @Override
    public void render(ForgeGui gui, PoseStack poseStack, float partialTicks, int width, int height) {

        Minecraft mc = gui.getMinecraft();
        Options options = mc.options;

        if (options.hideGui) return;
        var hit = mc.hitResult;

        if (options.getCameraType().isFirstPerson() && (mc.gameMode.getPlayerMode() != GameType.SPECTATOR ||
                gui.canRenderCrosshairForSpectator(hit))) {
            if (hit instanceof BlockHitResult blockHitResult) {
                BlockPos pos = blockHitResult.getBlockPos();
                BlockState blockState = mc.level.getBlockState(pos);
                if (blockState.is(BlockTags.BEDS)) {

                    var c = SleepTightPlatformStuff.getPlayerSleepData(mc.player);
                    float f = 1 - c.getInsomniaCooldown(mc.player);
                    if (f < 1) {

                        gui.setupOverlayRenderState(true, false, SleepTightClient.ICONS);
                        gui.setBlitOffset(-90);

                        poseStack.pushPose();

                        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ONE_MINUS_DST_COLOR,
                                GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR, GlStateManager.SourceFactor.ONE,
                                GlStateManager.DestFactor.ZERO);


                        int j = height / 2 - 7 + 16;
                        int k = width / 2 - 6;


                        int l = (int) (f * 11.0F);
                        GuiComponent.blit(poseStack, k, j, 3, 18, 11, 5, 48, 48);
                        GuiComponent.blit(poseStack, k, j, 16+3, 18, l, 5, 48, 48);


                        poseStack.popPose();
                    }
                }
            }
        }
    }


    //static stuff

    //I have a player and a block entity serializable capability which i want to have access on client too. When is the correct time to sync them? For example player enters a world and its serverside caps are read and initialized but client one isnt.


    public static void renderBedScreenOverlay(InBedChatScreen s, PoseStack poseStack, int mouseX, int mouseY) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        //ModBedCapability cap = ModBedCapability.getHomeBedIfHere(player, p.get());
        RenderSystem.setShaderTexture(0, SleepTightClient.ICONS);
        int y = s.height - 39;
        int iconSize = 18;
        if (isHomeBed) {
            int x = s.width / 2 - 120;
            Gui.blit(poseStack, x, y, 0, 0, iconSize, iconSize, 48, 48);
        }
        if (hasDreamerEssence) {
            int x = s.width / 2 + 120 - iconSize;
            Gui.blit(poseStack, x, y, iconSize, 0, iconSize, iconSize, 48, 48);
        }

        if (isHomeBed) {
            int x = s.width / 2 - 120;
            if (MthUtils.isWithinRectangle(x, y, iconSize, iconSize, mouseX, mouseY)) {
                int a = SleepTightPlatformStuff.getPlayerSleepData(player).getConsecutiveNightsSlept();
                var b = SleepTightPlatformStuff.getPlayerSleepData(player).getInsomniaCooldown(player);
                var nc = SleepTightPlatformStuff.getPlayerSleepData(player).getNightmareChance(player, player.getSleepingPos().orElse(BlockPos.ZERO));
                int nightSlept = SleepTightPlatformStuff.getPlayerSleepData(player).getNightsSleptInHomeBed();
                var lines = new ArrayList<>(mc.font.split(Component.translatable("gui.sleep_tight.home_bed"), 200));
                lines.addAll(mc.font.split(Component.translatable("gui.sleep_tight.time_slept", nightSlept), 200));
                lines.addAll(mc.font.split(Component.literal("consecutive nights" + a), 200));
                lines.addAll(mc.font.split(Component.literal("sleep cooldown" + b), 200));
                lines.addAll(mc.font.split(Component.literal("nightmare chance" + nc), 200));
                s.renderTooltip(poseStack, lines, mouseX, mouseY);
            }
        }
        if (hasDreamerEssence) {
            int x = s.width / 2 + 120 - iconSize;
            if (MthUtils.isWithinRectangle(x, y, iconSize, iconSize, mouseX, mouseY)) {

                s.renderTooltip(poseStack, mc.font.split(Component.translatable("gui.sleep_tight.dreamer_essence"), 200), mouseX, mouseY);
            }
        }
    }

    public static void setupOverlay(InBedChatScreen screen) {
        isHomeBed = false;
        hasDreamerEssence = false;
        Player player = Minecraft.getInstance().player;
        var p = player.getSleepingPos();
        if (p.isPresent()) {
            BlockPos pos = p.get();
            BedData cap = ForgePlayerSleepCapability.getHomeBedIfHere(player, pos);
            isHomeBed = cap != null;


            hasDreamerEssence = !(player.getLevel().getBlockState(pos).getBlock() instanceof NightBagBlock) &&
                    PlayerSleepData.isDreamerEssenceInRange(pos, player.level);
        }

    }

    //random static global state yay
    private static boolean isHomeBed = false;
    private static boolean hasDreamerEssence = false;
}

