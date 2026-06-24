package net.mehvahdjukaar.sleep_tight.mixins;

import net.mehvahdjukaar.sleep_tight.core.ModEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.structures.WoodlandMansionPieces;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// woodland mansions used to ship overridden room templates that placed a (now removed) infested bed block.
// instead we let vanilla place its normal beds and infest a random subset here, so it also works with modded beds.
@Mixin(TemplateStructurePiece.class)
public abstract class TemplateStructurePieceMixin {

    /*
    @Inject(method = "postProcess", at = @At("TAIL"))
    private void sleep_tight$infestMansionBeds(WorldGenLevel level, StructureManager structureManager,
                                               ChunkGenerator generator, RandomSource random, BoundingBox box,
                                               ChunkPos chunkPos, BlockPos pos, CallbackInfo ci) {
        if (!((Object) this instanceof WoodlandMansionPieces.WoodlandMansionPiece)) return;
        ModEvents.infestStructureBeds(level, chunkPos, ((StructurePiece) (Object) this).getBoundingBox(), random);
    }*/
    //Mansions dont even have beds...
}
