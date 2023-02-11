package net.mehvahdjukaar.sleep_tight.mixins;

import net.mehvahdjukaar.sleep_tight.common.HammockBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(BedBlock.class)
public abstract class BedBlockMixin extends Block {


    protected BedBlockMixin(Properties properties) {
        super(properties);
    }

    @Inject(method = "getBedOrientation", at = @At("RETURN"), cancellable = true)
    private static void getHammockOrientation(BlockGetter level, BlockPos pos,CallbackInfoReturnable<Direction> cir) {
        if (cir.getReturnValue() == null) {
            BlockState blockState = level.getBlockState(pos);
            if(blockState.getBlock() instanceof HammockBlock){
                cir.setReturnValue(blockState.getValue(HammockBlock.FACING));
            }
        }
    }


}
