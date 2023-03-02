package net.mehvahdjukaar.sleep_tight.mixins;

import net.mehvahdjukaar.sleep_tight.configs.CommonConfigs;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(BedBlock.class)
public abstract class BedBlockMixin extends Block {


    protected BedBlockMixin(Properties properties) {
        super(properties);
    }


    @ModifyArg(method = "use", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;explode(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;Lnet/minecraft/world/level/ExplosionDamageCalculator;DDDFZLnet/minecraft/world/level/Explosion$BlockInteraction;)Lnet/minecraft/world/level/Explosion;"
    ))
    public float explodeSmall(@Nullable Entity exploder, @Nullable DamageSource damageSource, @Nullable ExplosionDamageCalculator context,
                              double x, double y, double z, float size, boolean causesFire, Explosion.BlockInteraction mode) {
        if (CommonConfigs.DISABLE_BIG_EXPLOSION.get()) return 0;
        return size;
    }
}