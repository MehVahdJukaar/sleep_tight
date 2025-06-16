package net.mehvahdjukaar.sleep_tight.integration.fabric;

import net.mehvahdjukaar.moonlight.api.client.gui.MediaButton;
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
                Component.literal("§9Sleep Tight Configs"),
                ResourceLocation.withDefaultNamespace("textures/block/blue_wool.png"),
                parent, ClientConfigs.SPEC, CommonConfigs.SPEC);
    }

    @Override
    protected void addExtraButtons() {

        int y = this.height - 27;
        int centerX = this.width / 2;

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, (button) -> this.minecraft.setScreen(this.parent))
                .bounds(centerX - 45, y, 90, 20).build());

        var patreon = MediaButton.patreon(this, centerX - 45 - 22, y,
                "https://www.patreon.com/user?u=53696377");

        var kofi = MediaButton.koFi(this, centerX - 45 - 22 * 2, y,
                "https://ko-fi.com/mehvahdjukaar");

        var curseforge = MediaButton.curseForge(this, centerX - 45 - 22 * 3, y,
                "https://www.curseforge.com/minecraft/mc-mods/speep-tight");

        var github = MediaButton.github(this, centerX - 45 - 22 * 4, y,
                "https://github.com/MehVahdJukaar/sleep_tight");


        var discord = MediaButton.discord(this, centerX + 45 + 2, y,
                "https://discord.com/invite/qdKRTDf8Cv");

        var youtube = MediaButton.youtube(this, centerX + 45 + 2 + 22, y,
                "https://www.youtube.com/watch?v=LSPNAtAEn28&t=1s");

        var twitter = MediaButton.twitter(this, centerX + 45 + 2 + 22 * 2, y,
                "https://twitter.com/Supplementariez?s=09");

        var akliz = MediaButton.akliz(this, centerX + 45 + 2 + 22 * 3, y,
                "https://www.akliz.net/supplementaries");


        this.addRenderableWidget(kofi);
        this.addRenderableWidget(akliz);
        this.addRenderableWidget(patreon);
        this.addRenderableWidget(curseforge);
        this.addRenderableWidget(discord);
        this.addRenderableWidget(youtube);
        this.addRenderableWidget(github);
        this.addRenderableWidget(twitter);
    }

}
