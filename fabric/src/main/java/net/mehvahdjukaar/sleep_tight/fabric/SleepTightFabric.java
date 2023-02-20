package net.mehvahdjukaar.sleep_tight.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.mehvahdjukaar.moonlight.api.platform.PlatformHelper;
import net.mehvahdjukaar.moonlight.fabric.FabricSetupCallbacks;
import net.mehvahdjukaar.sleep_tight.ModEvents;
import net.mehvahdjukaar.sleep_tight.SleepTight;
import net.mehvahdjukaar.sleep_tight.SleepTightClient;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

public class SleepTightFabric implements ModInitializer {

    @Override
    public void onInitialize() {

        SleepTight.commonInit();

        if (PlatformHelper.getEnv().isClient()) {
            FabricSetupCallbacks.CLIENT_SETUP.add(SleepTightClient::init);
        }

        FabricSetupCallbacks.COMMON_SETUP.add(SleepTight::commonSetup);

        UseBlockCallback.EVENT.register(ModEvents::onRightClickBlock);

    }

}
