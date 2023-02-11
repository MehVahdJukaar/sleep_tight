package net.mehvahdjukaar.sleep_tight.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.mehvahdjukaar.moonlight.api.platform.PlatformHelper;
import net.mehvahdjukaar.moonlight.fabric.FabricSetupCallbacks;
import net.mehvahdjukaar.sleep_tight.SleepTight;
import net.mehvahdjukaar.sleep_tight.SleepTightClient;

public class GoatedFabric implements ModInitializer {

    @Override
    public void onInitialize() {

        SleepTight.commonInit();

        if (PlatformHelper.getEnv().isClient()) {
            FabricSetupCallbacks.CLIENT_SETUP.add(SleepTightClient::init);
            FabricSetupCallbacks.COMMON_SETUP.add(GoatHelmetArmorRenderer::register);
        }

        FabricSetupCallbacks.COMMON_SETUP.add(SleepTight::commonSetup);

        ServerTickEvents.START_SERVER_TICK.register(FabricRamBreakingHandler::tick);

    }


}
