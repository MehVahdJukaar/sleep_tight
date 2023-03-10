package net.mehvahdjukaar.sleep_tight.mixins;

import net.mehvahdjukaar.sleep_tight.common.blocks.ISleepTightBed;
import net.mehvahdjukaar.sleep_tight.configs.CommonConfigs;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BedBlock.class)
public abstract class BedBlockMixin extends Block implements ISleepTightBed {


    protected BedBlockMixin(Properties properties) {
        super(properties);
    }


    @ModifyArg(method = "use", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;explode(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;Lnet/minecraft/world/level/ExplosionDamageCalculator;DDDFZLnet/minecraft/world/level/Explosion$BlockInteraction;)Lnet/minecraft/world/level/Explosion;"
    ))
    public float explodeSmall(@Nullable Entity exploder, @Nullable DamageSource damageSource, @Nullable ExplosionDamageCalculator context,
                              double x, double y, double z, float size, boolean causesFire, Explosion.BlockInteraction mode) {
        if (CommonConfigs.EXPLOSION_BEHAVIOR.get() == CommonConfigs.ExplosionBehavior.TINY_EXPLOSION) return 0;
        return size;
    }

    @Redirect(method = "use", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/BedBlock;canSetSpawn(Lnet/minecraft/world/level/Level;)Z"))
    private boolean allowsSleepingInDimension(Level level) {
        if (!CommonConfigs.EXPLOSION_BEHAVIOR.get().canExplode()) {
            return true;
        }
        return BedBlock.canSetSpawn(level);
    }
}