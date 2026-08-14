package net.mehvahdjukaar.sleep_tight.mixins.fabric;

import net.mehvahdjukaar.sleep_tight.core.BedbugSpawner;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.PhantomSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//on neoforge we just register our spawner with an event. fabric has nothing like that and the level's
//customSpawners list is immutable, so we ride along with the phantom spawner instead, same as supplementaries
//does with the wandering trader one
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
