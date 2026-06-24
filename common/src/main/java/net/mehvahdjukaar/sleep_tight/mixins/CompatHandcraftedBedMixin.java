package net.mehvahdjukaar.sleep_tight.mixins;

import earth.terrarium.handcrafted.common.blocks.FancyBedBlock;
import net.mehvahdjukaar.sleep_tight.common.tiles.CompatBedTile;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(FancyBedBlock.class)
public class CompatHandcraftedBedMixin implements EntityBlock {

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos arg, BlockState state) {
        return new CompatBedTile(arg, state);
    }
}
