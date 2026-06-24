package net.mehvahdjukaar.sleep_tight.client;


import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.mehvahdjukaar.moonlight.api.platform.PlatHelper;
import net.mehvahdjukaar.moonlight.api.util.math.MthUtils;
import net.mehvahdjukaar.moonlight.api.util.math.colors.HSVColor;
import net.mehvahdjukaar.moonlight.api.util.math.colors.RGBColor;
import net.mehvahdjukaar.sleep_tight.STPlatStuff;
import net.mehvahdjukaar.sleep_tight.SleepTightClient;
import net.mehvahdjukaar.sleep_tight.common.blocks.DreamEssenceBlock;
import net.mehvahdjukaar.sleep_tight.common.blocks.ISleepTightBed;
import net.mehvahdjukaar.sleep_tight.common.entities.BedEntity;
import net.mehvahdjukaar.sleep_tight.common.items.NightBagItem;
import net.mehvahdjukaar.sleep_tight.configs.ClientConfigs;
import net.mehvahdjukaar.sleep_tight.core.BedData;
import net.mehvahdjukaar.sleep_tight.core.PlayerSleepData;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.gui.screens.InBedChatScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;

public class SleepGuiOverlay extends Gui implements LayeredDraw.Layer {


    public SleepGuiOverlay(Minecraft minecraft) {
        super(minecraft);
    }

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        Options options = mc.options;


        BedData bedData = SleepTightClient.getLayingBedData();
        Player player = mc.player;
        if (bedData != null) {
            PlayerSleepData playerData = STPlatStuff.getPlayerSleepData(player);
            renderBar(graphics,
                    bedData, playerData, mc,
                    player, deltaTracker.getGameTimeDeltaTicks());
            return;
        }

        HitResult hit = mc.hitResult;

        boolean cooldown = ClientConfigs.INSOMNIA_COOLDOWN.get();
        boolean timer = ClientConfigs.INSOMNIA_TIMER.get();

        if (!timer && !cooldown) return;

        renderCooldownCrossAir(graphics, options, mc, hit, player, cooldown, timer);
    }

    private void renderCooldownCrossAir(GuiGraphics graphics, Options options, Minecraft mc, HitResult hit, Player player, boolean cooldown, boolean timer) {
        if (options.getCameraType().isFirstPerson() && (mc.gameMode.getPlayerMode() != GameType.SPECTATOR ||
                this.canRenderCrosshairForSpectator(hit))) {

            boolean laying = player.getVehicle() instanceof BedEntity;
            if (laying || (cooldown && (
                    (hit instanceof BlockHitResult bh && mc.level.getBlockState(bh.getBlockPos())
                            .getBlock() instanceof ISleepTightBed) ||
                            player.getMainHandItem().getItem() instanceof NightBagItem
            ))) {


                PlayerSleepData playerData = STPlatStuff.getPlayerSleepData(player);
                float insomniaPerc = 1 - playerData.getInsomniaCooldownPercentage(player);
                if (insomniaPerc < 1) {

                    if (laying && timer) {
                        graphics.drawString(mc.font, "" + playerData.getInsomniaCooldown(player) / 20, 2, 2, 14737632);
                    }

                    if (cooldown) {
                        setupOverlayRenderState(graphics, true, false, SleepTightClient.ICONS);
                        //gui.setBlitOffset(-90);

                        graphics.pose().pushPose();

                        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ONE_MINUS_DST_COLOR,
                                GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR, GlStateManager.SourceFactor.ONE,
                                GlStateManager.DestFactor.ZERO);


                        int py = graphics.guiHeight() / 2 - 7 + 16;
                        int px = graphics.guiWidth() / 2 - 6;

                        if (mc.options.attackIndicator().get() == AttackIndicatorStatus.CROSSHAIR &&
                                player.getAttackStrengthScale(0.0F) != 1) {
                            py += 8;
                        }

                        int l = (int) (insomniaPerc * 11.0F);
                        graphics.blit(SleepTightClient.ICONS, px, py, 3, 18, 11, 5, 48, 48);
                        graphics.blit(SleepTightClient.ICONS, px, py, 16 + 3f, 18, l, 5, 48, 48);


                        graphics.pose().popPose();

                        RenderSystem.defaultBlendFunc();
                    }
                }
            }
        }
    }

    protected void setupOverlayRenderState(GuiGraphics graphics, boolean blend, boolean depthTest, ResourceLocation texture) {
    }

    //static stuff

    //I have a player and a block entity serializable capability which i want to have access on client too. When is the correct time to sync them? For example player enters a world and its serverside caps are read and initialized but client one isnt.


    public static void renderBedScreenOverlay(InBedChatScreen s, GuiGraphics graphics, int mouseX, int mouseY) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;
        BlockPos sleepingPos = player.getSleepingPos().orElse(null);
        if (sleepingPos == null) return;
        PlayerSleepData playerData = STPlatStuff.getPlayerSleepData(player);
        BedData bedData = STPlatStuff.getBedDataIfPresent(player.level(), sleepingPos);
        if (bedData == null) return;
        boolean hasDreamerEssence = DreamEssenceBlock.isInRange(sleepingPos, player.level());
        boolean isMaxFamiliar = playerData.isBedFamiliarityMaxed(bedData);

        if (ClientConfigs.SHOW_TIME.get().shouldShow(player)) {
            graphics.drawString(mc.font, getCurrentTime(player.level()), 2, 2, 14737632);
        }

        //ModBedCapability cap = ModBedCapability.getHomeBedIfHere(player, p.get());
        int y = s.height - 39;
        int iconSize = 18;
        int bx = s.width / 2 - 120;
        int bh = isMaxFamiliar ? 0 : 28;
        graphics.blit(SleepTightClient.ICONS, bx, y, 0, bh, iconSize, iconSize, 48, 48);

        if (hasDreamerEssence) {
            int x = s.width / 2 + 120 - iconSize;
            graphics.blit(SleepTightClient.ICONS, x, y, iconSize, 0, iconSize, iconSize, 48, 48);
        }

        if (MthUtils.isWithinRectangle(bx, y, iconSize, iconSize, mouseX, mouseY)) {
            double nightmare = playerData.getNightmareChance(player, sleepingPos);
            int bedLevel = bedData.getBedLevel(player);
            MutableComponent title = isMaxFamiliar ?
                    Component.translatable("gui.sleep_tight.home_bed") :
                    Component.translatable("gui.sleep_tight.bed");
            var lines = new ArrayList<>(mc.font.split(title, 200));
            if (!isMaxFamiliar) {
                String percent = String.format("%.1f", playerData.getBedFamiliarity(bedData) * 100) + "%";
                lines.addAll(mc.font.split(Component.translatable("gui.sleep_tight.familiarity", percent), 200));
            }
            if(PlatHelper.isDev()) lines.addAll(mc.font.split(Component.literal( "LastId Bits: " + bedData.getId()), 400));
            lines.addAll(mc.font.split(Component.translatable("gui.sleep_tight.bed_level", bedLevel), 200));
            lines.addAll(mc.font.split(Component.translatable("gui.sleep_tight.nightmare", nightmare), 200));
            graphics.renderTooltip(mc.font, lines, mouseX, mouseY);
        }
        if (hasDreamerEssence) {
            int dx = s.width / 2 + 120 - iconSize;
            if (MthUtils.isWithinRectangle(dx, y, iconSize, iconSize, mouseX, mouseY)) {

                graphics.renderTooltip(mc.font, mc.font.split(Component.translatable("gui.sleep_tight.dreamer_essence"), 200), mouseX, mouseY);
            }
        }
    }


    private static final ResourceLocation BACKGROUND = ResourceLocation.withDefaultNamespace("boss_bar/white_background");
    private static final ResourceLocation PROGRESS = ResourceLocation.withDefaultNamespace("boss_bar/white_progress");
    private static final ResourceLocation OVERLAY_BACKGROUND = ResourceLocation.withDefaultNamespace("boss_bar/notched_6_background");
    private static final ResourceLocation OVERLAY_PROGRESS = ResourceLocation.withDefaultNamespace("boss_bar/notched_6_progress");


    private static void renderBar(GuiGraphics graphics,
                                  BedData bedData, PlayerSleepData playerData,
                                  Minecraft mc, Player player,
                                  float partialTicks) {
        int screenHeight = graphics.guiHeight();
        int screenWidth = graphics.guiWidth();
        int xpBarLeft = screenWidth / 2 - 91;

        float familiarity = playerData.getBedFamiliarity(bedData);
        boolean hasDreamerEssence = DreamEssenceBlock.isInRange(player.blockPosition(), player.level());
        double nightmareChance = playerData.getNightmareChance(player, player.blockPosition());
        int barColor = hasDreamerEssence ? 0xc93095 : 0xDCAC07;//  0xDCB402

        HSVColor color = new RGBColor(barColor).asHSV();
        if (!hasDreamerEssence) {
            float desaturation = (float) (1 - (nightmareChance * 0.5));
            color = color.withSaturation(color.saturation() * desaturation)
                    .withValue(color.value() * desaturation);
        }
        var rgb = color.asRGB();

        RenderSystem.setShaderColor(rgb.red(), rgb.green(), rgb.blue(), 1.0F);
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableBlend();

        int k = (int) (familiarity * 183.0F);
        int xpBarTop = screenHeight - 32 + 3;
        graphics.blitSprite(BACKGROUND, xpBarLeft, xpBarTop, 183, 5);


        graphics.blitSprite(PROGRESS, 183, 5, 0, 0, xpBarLeft, xpBarTop, k,5);
        graphics.blitSprite(OVERLAY_PROGRESS, xpBarLeft, xpBarTop, 182, 5);
        RenderSystem.disableBlend();

        int power = bedData.getBedLevel(player);

        boolean bedFamiliarityMaxed = playerData.isBedFamiliarityMaxed(bedData);
        int textCol = hasDreamerEssence ?
                (bedFamiliarityMaxed ? 0xBC46FF : 0x602680) :
                (bedFamiliarityMaxed ? 0x00E1FF : 0x186475);

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        String bedLevelStr = String.valueOf(power);
        int cx = (screenWidth - mc.font.width(bedLevelStr)) / 2;
        int cy = screenHeight - 31 - 4;
        graphics.drawString(mc.font, bedLevelStr, cx + 1, cy, 0, false);
        graphics.drawString(mc.font, bedLevelStr, cx - 1, cy, 0, false);
        graphics.drawString(mc.font, bedLevelStr, cx, cy + 1, 0, false);
        graphics.drawString(mc.font, bedLevelStr, cx, cy - 1, 0, false);
        graphics.drawString(mc.font, bedLevelStr, cx, cy, textCol, false);
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


}

