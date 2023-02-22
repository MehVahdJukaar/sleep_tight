package net.mehvahdjukaar.sleep_tight.forge;


import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.mehvahdjukaar.sleep_tight.SleepTightClient;
import net.mehvahdjukaar.sleep_tight.common.DreamerEssenceTargetEntity;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.InBedChatScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public class SleepGuiOverlay extends Gui implements IGuiOverlay {

    public SleepGuiOverlay(Minecraft minecraft) {
        super(minecraft, minecraft.getItemRenderer());
    }

    @Override
    public void render(ForgeGui gui, PoseStack poseStack, float partialTicks, int width, int height) {

        if (!gui.getMinecraft().options.hideGui) {
            Options options = this.minecraft.options;
            if (options.getCameraType().isFirstPerson() && (this.minecraft.gameMode.getPlayerMode() != GameType.SPECTATOR || this.canRenderCrosshairForSpectator(this.minecraft.hitResult))) {
                gui.setupOverlayRenderState(true, false, SleepTightClient.ICONS);
                gui.setBlitOffset(-90);
                renderCrosshair(poseStack);
            }
        }
    }


    public void renderCrosshair(PoseStack poseStack) {
        poseStack.pushPose();
        this.screenWidth = this.minecraft.getWindow().getGuiScaledWidth();
        this.screenHeight = this.minecraft.getWindow().getGuiScaledHeight();


        poseStack.translate(0, 10, 0);

        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ONE_MINUS_DST_COLOR, GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        //this.blit(poseStack, (this.screenWidth - 15) / 2, (this.screenHeight - 15) / 2, 0, 0, 15, 15);
        if (this.minecraft.options.attackIndicator().get() == AttackIndicatorStatus.CROSSHAIR) {
            float f = this.minecraft.player.getAttackStrengthScale(0.0F);
            boolean flag = false;
            if (this.minecraft.crosshairPickEntity instanceof LivingEntity && f >= 1.0F) {
                flag = this.minecraft.player.getCurrentItemAttackStrengthDelay() > 5.0F;
                flag &= this.minecraft.crosshairPickEntity.isAlive();
                flag &= this.minecraft.player.canHit(this.minecraft.crosshairPickEntity, 0.0);
            }

            int j = this.screenHeight / 2 - 7 + 16;
            int k = this.screenWidth / 2 - 8;

            if (flag) {
                this.blit(poseStack, k, j, 68, 94, 16, 16);
            } else {
                int l = (int) (f * 17.0F);
                blit(poseStack, k, j, 0, 18, 16, 4, 36, 48);
                blit(poseStack, k, j, 16, 18, l, 4, 36, 48);
            }

        }

        poseStack.popPose();

    }


    //static stuff

    //I have a player and a block entity serializable capability which i want to have access on client too. When is the correct time to sync them? For example player enters a world and its serverside caps are read and initialized but client one isnt.


    public static void renderBedScreenOverlay(InBedChatScreen s, PoseStack poseStack) {
        Player player = Minecraft.getInstance().player;
        //ModBedCapability cap = ModBedCapability.getHomeBedIfHere(player, p.get());
        RenderSystem.setShaderTexture(0, SleepTightClient.ICONS);
        if (isHomeBed) {
            Gui.blit(poseStack, s.width / 2 - 120, s.height - 39, 0, 0, 18, 18, 48, 48);
        }
        if (hasDreamerEssence) {
            Gui.blit(poseStack, s.width / 2 + 120 - 18, s.height - 39, 18, 0, 18, 18, 48, 48);
        }
    }

    public static void setupOverlay(InBedChatScreen screen) {
        isHomeBed = false;
        hasDreamerEssence = false;
        Player player = Minecraft.getInstance().player;
        var p = player.getSleepingPos();
        if (p.isPresent()) {
            BlockPos pos = p.get();
            ModBedCapability cap = ModBedCapability.getHomeBedIfHere(player, pos);
            isHomeBed = cap != null;

            hasDreamerEssence = !player.getLevel().getEntitiesOfClass(DreamerEssenceTargetEntity.class,
                    new AABB(pos).inflate(5)).isEmpty();
        }

    }

    //random static global state yay
    private static boolean isHomeBed = false;
    private static boolean hasDreamerEssence = false;
}

