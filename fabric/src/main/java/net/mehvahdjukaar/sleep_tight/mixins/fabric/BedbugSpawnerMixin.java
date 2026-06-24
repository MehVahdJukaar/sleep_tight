package net.mehvahdjukaar.sleep_tight.mixins.fabric;

import net.mehvahdjukaar.sleep_tight.core.BedbugSpawner;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.PhantomSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * NeoForge registers our {@link BedbugSpawner} through {@code ModifyCustomSpawnersEvent}.
 * Fabric has no such hook and {@link ServerLevel}'s {@code customSpawners} is an
 * {@link com.google.common.collect.ImmutableList} ({@code MinecraftServer#createLevels}), so we
 * can't add to it. Instead we piggyback on the vanilla {@link PhantomSpawner} (which lives in the
 * overworld's spawner list), exactly like Supplementaries does with {@code WanderingTraderSpawner}.
 */
@Mixin(PhantomSpawner.class)
public class BedbugSpawnerMixin {

    @Unique
    private final BedbugSpawner sleep_tight$bedbugSpawner = new BedbugSpawner();

    @Inject(method = "tick", at = @At("HEAD"))
    private void sleep_tight$tickBedbugs(ServerLevel level, boolean spawnHostiles, boolean spawnPassives,
                                         CallbackInfoReturnable<Integer> cir) {
        sleep_tight$bedbugSpawner.tick(level, spawnHostiles, spawnPassives);
    }
}
