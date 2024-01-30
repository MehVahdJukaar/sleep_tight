package net.mehvahdjukaar.sleep_tight.integration.fabric;

import net.mehvahdjukaar.moonlight.api.client.gui.UrlButton;
import net.mehvahdjukaar.moonlight.api.platform.configs.fabric.FabricConfigListScreen;
import net.mehvahdjukaar.sleep_tight.SleepTight;
import net.mehvahdjukaar.sleep_tight.configs.ClientConfigs;
import net.mehvahdjukaar.sleep_tight.configs.CommonConfigs;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class ModConfigSelectScreen extends FabricConfigListScreen {

    public ModConfigSelectScreen(Screen parent) {
        super(SleepTight.MOD_ID, SleepTight.NIGHT_BAG.get().asItem().getDefaultInstance(),
                Component.literal("§9Sleep Tight Configs"), new ResourceLocation("textures/block/blue_wool.png"),
                parent, ClientConfigs.SPEC, CommonConfigs.SPEC);
    }

    @Override
    protected void addExtraButtons() {

        int y = this.height - 27;
        int centerX = this.width / 2;

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, (button) -> this.minecraft.setScreen(this.parent))
                .bounds(centerX - 45, y, 90, 20).build());

        UrlButton.addMyMediaButtons(this, centerX, y, "sleep-tight", "sleep_tight");

    }

}
