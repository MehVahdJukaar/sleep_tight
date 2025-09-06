package net.mehvahdjukaar.sleep_tight.client;

import com.google.gson.JsonParser;
import net.mehvahdjukaar.moonlight.api.events.AfterLanguageLoadEvent;
import net.mehvahdjukaar.moonlight.api.platform.PlatHelper;
import net.mehvahdjukaar.moonlight.api.resources.ResType;
import net.mehvahdjukaar.moonlight.api.resources.pack.*;
import net.mehvahdjukaar.sleep_tight.SleepTight;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class ModClientDynamicResources extends DynamicClientResourceProvider {

    public ModClientDynamicResources() {
        super(SleepTight.res("generated_pack"), PackGenerationStrategy.REGEN_ON_EVERY_RELOAD);
    }

    @Override
    protected Collection<String> gatherSupportedNamespaces() {
        return List.of("minecraft");
    }

    @Override
    public void regenerateDynamicAssets(Consumer<ResourceGenTask> executor) {

        executor.accept((manager, sink) -> {
            ResourceLocation res = ResourceLocation.withDefaultNamespace("white_bed");


            var o = manager.getResource(ResType.BLOCKSTATES.getPath(res));

            if (o.isPresent() && !Objects.equals(o.get().sourcePackId(), "vanilla")) return;


            //this replaces bed models to set their particles
            if (!PlatHelper.isModLoaded("enhancedblockentities") &&
                    !PlatHelper.isModLoaded("betterbeds")) {

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
                    sink.addJson(ResourceLocation.withDefaultNamespace(
                            c.getName() + "_bed"), json, ResType.BLOCKSTATES);
                }
            }
        });
    }

    @Override
    protected void addDynamicTranslations(AfterLanguageLoadEvent afterLanguageLoadEvent) {

    }
}
