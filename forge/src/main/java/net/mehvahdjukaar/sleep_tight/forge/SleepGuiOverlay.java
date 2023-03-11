package net.mehvahdjukaar.sleep_tight.forge;


import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.mehvahdjukaar.moonlight.api.util.math.MthUtils;
import net.mehvahdjukaar.sleep_tight.SleepTightClient;
import net.mehvahdjukaar.sleep_tight.SleepTightPlatformStuff;
import net.mehvahdjukaar.sleep_tight.common.entities.BedEntity;
import net.mehvahdjukaar.sleep_tight.common.blocks.ISleepTightBed;
import net.mehvahdjukaar.sleep_tight.common.blocks.NightBagBlock;
import net.mehvahdjukaar.sleep_tight.configs.ClientConfigs;
import net.mehvahdjukaar.sleep_tight.core.BedData;
import net.mehvahdjukaar.sleep_tight.core.PlayerSleepData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.InBedChatScreen;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

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

        boolean cooldown = ClientConfigs.INSOMNIA_COOLDOWN.get();
        boolean timer = ClientConfigs.INSOMNIA_TIMER.get();

        if (!timer && !cooldown) return;


        if (options.getCameraType().isFirstPerson() && (mc.gameMode.getPlayerMode() != GameType.SPECTATOR ||
                gui.canRenderCrosshairForSpectator(hit))) {
            Player player = mc.player;

            boolean laying = player.getVehicle() instanceof BedEntity;
            if (laying || (cooldown && hit instanceof BlockHitResult bh &&
                    mc.level.getBlockState(bh.getBlockPos()).getBlock() instanceof ISleepTightBed)) {


                var c = SleepTightPlatformStuff.getPlayerSleepData(player);
                float f = 1 - c.getInsomniaCooldown(player);
                if (f < 1) {

                    if (laying && timer) {
                        mc.font.draw(poseStack, "" + c.getInsomniaTimeLeft(player)/20, 2.0F, 2, 14737632);
                    }

                    if(cooldown) {

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
                        GuiComponent.blit(poseStack, k, j, 16 + 3, 18, l, 5, 48, 48);


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


        if (ClientConfigs.SHOW_TIME.get()) {
            mc.font.draw(poseStack, getCurrentTime(player.level), 2.0F, 2, 14737632);
        }

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
                var data = SleepTightPlatformStuff.getPlayerSleepData(player);
                double nightmare = data.getNightmareChance(player, player.getSleepingPos().orElse(BlockPos.ZERO));
                int bedLevel = data.getHomeBedLevel();
                var lines = new ArrayList<>(mc.font.split(Component.translatable("gui.sleep_tight.home_bed"), 200));
                lines.addAll(mc.font.split(Component.translatable("gui.sleep_tight.bed_level", bedLevel), 200));
                lines.addAll(mc.font.split(Component.translatable("gui.sleep_tight.nightmare", nightmare), 200));
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

    private static Component getCurrentTime(Level level) {
        int time = ((int) (level.getDayTime() + 6000) % 24000);
        int m = (int) (((time % 1000f) / 1000f) * 60);
        int h = time / 1000;
        String a = "";
        if (!ClientConfigs.TIME_FORMAT_24H.get()) {
            a = time < 12000 ? " AM" : " PM";
            h = h % 12;
            if (h == 0) h = 12;
        }
        return Component.literal(h + ":" + ((m < 10) ? "0" : "") + m + a);

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

