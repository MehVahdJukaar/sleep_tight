package net.mehvahdjukaar.sleep_tight.client;


import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.mehvahdjukaar.moonlight.api.util.math.MthUtils;
import net.mehvahdjukaar.sleep_tight.SleepTightClient;
import net.mehvahdjukaar.sleep_tight.SleepTightPlatformStuff;
import net.mehvahdjukaar.sleep_tight.common.blocks.DreamEssenceBlock;
import net.mehvahdjukaar.sleep_tight.common.blocks.ISleepTightBed;
import net.mehvahdjukaar.sleep_tight.common.blocks.NightBagBlock;
import net.mehvahdjukaar.sleep_tight.common.entities.BedEntity;
import net.mehvahdjukaar.sleep_tight.common.items.NightBagItem;
import net.mehvahdjukaar.sleep_tight.configs.ClientConfigs;
import net.mehvahdjukaar.sleep_tight.core.BedData;
import net.mehvahdjukaar.sleep_tight.core.PlayerSleepData;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.InBedChatScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

import java.util.ArrayList;

public abstract class SleepGuiOverlay<T extends Gui> {

    public void render(T gui, GuiGraphics graphics, float partialTicks, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
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
            if (laying || (cooldown && (
                    (hit instanceof BlockHitResult bh && mc.level.getBlockState(bh.getBlockPos())
                            .getBlock() instanceof ISleepTightBed) ||
                            player.getMainHandItem().getItem() instanceof NightBagItem
            ))) {


                var c = SleepTightPlatformStuff.getPlayerSleepData(player);
                float f = 1 - c.getInsomniaCooldown(player);
                if (f < 1) {

                    if (laying && timer) {
                        graphics.drawString(mc.font, "" + c.getInsomniaTimeLeft(player) / 20, 2, 2, 14737632);
                    }

                    if (cooldown) {

                        setupOverlayRenderState(gui, true, false, SleepTightClient.ICONS);
                        //gui.setBlitOffset(-90);

                        graphics.pose().pushPose();

                        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ONE_MINUS_DST_COLOR,
                                GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR, GlStateManager.SourceFactor.ONE,
                                GlStateManager.DestFactor.ZERO);


                        int j = height / 2 - 7 + 16;
                        int k = width / 2 - 6;

                        if (mc.options.attackIndicator().get() == AttackIndicatorStatus.CROSSHAIR &&
                                player.getAttackStrengthScale(0.0F) != 1) {
                            j += 8;
                        }

                        int l = (int) (f * 11.0F);
                        graphics.blit(SleepTightClient.ICONS, k, j, 3, 18, 11, 5, 48, 48);
                        graphics.blit(SleepTightClient.ICONS, k, j, 16 + 3f, 18, l, 5, 48, 48);


                        graphics.pose().popPose();
                    }
                }
            }
        }
    }

    protected abstract void setupOverlayRenderState(T gui, boolean blend, boolean depthTest, ResourceLocation texture);


    //static stuff

    //I have a player and a block entity serializable capability which i want to have access on client too. When is the correct time to sync them? For example player enters a world and its serverside caps are read and initialized but client one isnt.


    public static void renderBedScreenOverlay(InBedChatScreen s, GuiGraphics graphics, int mouseX, int mouseY) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;


        if (ClientConfigs.SHOW_TIME.get()) {
            graphics.drawString(mc.font, getCurrentTime(player.level()), 2, 2, 14737632);
        }

        //ModBedCapability cap = ModBedCapability.getHomeBedIfHere(player, p.get());
        int y = s.height - 39;
        int iconSize = 18;
        if (isHomeBed) {
            int x = s.width / 2 - 120;
            graphics.blit(SleepTightClient.ICONS, x, y, 0, 0, iconSize, iconSize, 48, 48);
        }
        if (hasDreamerEssence) {
            int x = s.width / 2 + 120 - iconSize;
            graphics.blit(SleepTightClient.ICONS, x, y, iconSize, 0, iconSize, iconSize, 48, 48);
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
                graphics.renderTooltip(mc.font, lines, mouseX, mouseY);
            }
        }
        if (hasDreamerEssence) {
            int x = s.width / 2 + 120 - iconSize;
            if (MthUtils.isWithinRectangle(x, y, iconSize, iconSize, mouseX, mouseY)) {

                graphics.renderTooltip(mc.font, mc.font.split(Component.translatable("gui.sleep_tight.dreamer_essence"), 200), mouseX, mouseY);
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
            BedData cap = PlayerSleepData.getHomeBedIfHere(player, pos);
            isHomeBed = cap != null;


            hasDreamerEssence = !(player.level().getBlockState(pos).getBlock() instanceof NightBagBlock) &&
                    DreamEssenceBlock.isInRange(pos, player.level());
        }

    }

    //random static global state yay
    private static boolean isHomeBed = false;
    private static boolean hasDreamerEssence = false;

}

