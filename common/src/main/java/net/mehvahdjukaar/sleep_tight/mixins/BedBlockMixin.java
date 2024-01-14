package net.mehvahdjukaar.sleep_tight.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.mehvahdjukaar.sleep_tight.common.blocks.ISleepTightBed;
import net.mehvahdjukaar.sleep_tight.configs.CommonConfigs;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
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
            target = "Lnet/minecraft/world/level/Level;explode(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;Lnet/minecraft/world/level/ExplosionDamageCalculator;Lnet/minecraft/world/phys/Vec3;FZLnet/minecraft/world/level/Level$ExplosionInteraction;)Lnet/minecraft/world/level/Explosion;"
    ))
    public float explodeSmall(@Nullable Entity exploder, @Nullable DamageSource damageSource, @Nullable ExplosionDamageCalculator context,
                              Vec3 v, float size, boolean causesFire, Level.ExplosionInteraction mode) {
        if (CommonConfigs.EXPLOSION_BEHAVIOR.get() == CommonConfigs.ExplosionBehavior.TINY_EXPLOSION) return 0;
        return size;
    }

    @WrapOperation(method = "use", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/BedBlock;canSetSpawn(Lnet/minecraft/world/level/Level;)Z"))
    private boolean allowsSleepingInDimension(Level level, Operation<Boolean> original) {
        if (!CommonConfigs.EXPLOSION_BEHAVIOR.get().canExplode()) {
            return true;
        }
        return original.call(level);
    }
}