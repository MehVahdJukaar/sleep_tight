package net.mehvahdjukaar.sleep_tight.integration.fabric;

import net.mehvahdjukaar.moonlight.api.client.gui.LinkButton;
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
                Component.literal("\u00A79Sleep Tight Configs"), new ResourceLocation("textures/block/blue_wool.png"),
                parent, ClientConfigs.SPEC, CommonConfigs.SPEC);
    }

    @Override
    protected void addExtraButtons() {

        int y = this.height - 27;
        int centerX = this.width / 2;

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, (button) -> this.minecraft.setScreen(this.parent))
                .bounds(centerX - 45, y, 90, 20).build());

        var patreon = LinkButton.create(this, centerX - 45 - 22, y, 3, 1,
                "https://www.patreon.com/user?u=53696377", "Support me on Patreon :D");

        var kofi = LinkButton.create(this, centerX - 45 - 22 * 2, y, 2, 2,
                "https://ko-fi.com/mehvahdjukaar", "Donate a Coffee");

        var curseforge = LinkButton.create(this, centerX - 45 - 22 * 3, y, 1, 2,
                "https://www.curseforge.com/minecraft/mc-mods/speep-tight", "CurseForge Page");

        var github = LinkButton.create(this, centerX - 45 - 22 * 4, y, 0, 2,
                "https://github.com/MehVahdJukaar/Supplementaries/wiki", "Mod Wiki");


        var discord = LinkButton.create(this, centerX + 45 + 2, y, 1, 1,
                "https://discord.com/invite/qdKRTDf8Cv", "Mod Discord");

        var youtube = LinkButton.create(this, centerX + 45 + 2 + 22, y, 0, 1,
                "https://www.youtube.com/watch?v=LSPNAtAEn28&t=1s", "Youtube Channel");

        var twitter = LinkButton.create(this, centerX + 45 + 2 + 22 * 2, y, 2, 1,
                "https://twitter.com/Supplementariez?s=09", "Twitter Page");

        var akliz = LinkButton.create(this, centerX + 45 + 2 + 22 * 3, y, 3, 2,
                "https://www.akliz.net/supplementaries", "Need a server? Get one with Akliz");


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
