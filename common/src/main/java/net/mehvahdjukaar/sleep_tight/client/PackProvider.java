package net.mehvahdjukaar.sleep_tight.client;

import com.google.gson.JsonParser;
import net.mehvahdjukaar.moonlight.api.platform.PlatformHelper;
import net.mehvahdjukaar.moonlight.api.resources.ResType;
import net.mehvahdjukaar.moonlight.api.resources.pack.DynClientResourcesProvider;
import net.mehvahdjukaar.moonlight.api.resources.pack.DynamicTexturePack;
import net.mehvahdjukaar.sleep_tight.SleepTight;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.DyeColor;
import org.apache.logging.log4j.Logger;

import java.util.Objects;

public class PackProvider extends DynClientResourcesProvider {

    public static final PackProvider INSTANCE = new PackProvider();

    public PackProvider() {
        super(new DynamicTexturePack(SleepTight.res("generated_pack"), Pack.Position.BOTTOM, true, true));
        this.dynamicPack.generateDebugResources = false;
        this.dynamicPack.addNamespaces("minecraft");
    }

    @Override
    public Logger getLogger() {
        return SleepTight.LOGGER;
    }

    @Override
    public boolean dependsOnLoadedPacks() {
        return true;
    }

    @Override
    public void regenerateDynamicAssets(ResourceManager manager) {

        ResourceLocation res = new ResourceLocation("white_bed");


        var o = manager.getResource(ResType.BLOCKSTATES.getPath(res));

        if (o.isPresent() && !Objects.equals(o.get().sourcePackId(), "Default")) return;


        if (!PlatformHelper.isModLoaded("enhancedblockentities") &&
                !PlatformHelper.isModLoaded("betterbeds")) {

            String str = """
                    {
                      "variants": {
                        "": {
                          "model": "sleep_tight:block/#_bed"
                        }
                      }
                    }""";
            for (var c : DyeColor.values()) {
                var json = JsonParser.parseString(str.replace("#", c.getName()));

                dynamicPack.addJson(new ResourceLocation(c.getName() + "_bed"), json, ResType.BLOCKSTATES);
            }
        }
    }

    @Override
    public void generateStaticAssetsOnStartup(ResourceManager manager) {

    }


}
