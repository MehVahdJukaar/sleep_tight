package net.mehvahdjukaar.sleep_tight.mixins.forge;

import net.mehvahdjukaar.sleep_tight.core.BedbugSpawner;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.CustomSpawner;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

// Forge 1.20.1 has no ModifyCustomSpawnersEvent (NeoForge), so we append the BedbugSpawner to the
// (otherwise immutable) custom spawner list straight from the ServerLevel constructor.
@Mixin(ServerLevel.class)
public class ServerLevelMixin {

    @Shadow
    @Final
    @Mutable
    private List<CustomSpawner> customSpawners;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void sleep_tight$addBedbugSpawner(CallbackInfo ci) {
        List<CustomSpawner> spawners = new ArrayList<>(this.customSpawners);
        spawners.add(new BedbugSpawner());
        this.customSpawners = spawners;
    }
}
