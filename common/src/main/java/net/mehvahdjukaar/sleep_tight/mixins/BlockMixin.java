package net.mehvahdjukaar.sleep_tight.mixins;

import net.mehvahdjukaar.sleep_tight.SleepTight;
import net.mehvahdjukaar.sleep_tight.common.InvigoratedEffect;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Block.class)
public abstract class BlockMixin {

    @Inject(method = "dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;spawnAfterBreak(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;Z)V",
                    shift = At.Shift.BEFORE))
    private static void setXpHack(BlockState state, Level level, BlockPos pos, BlockEntity blockEntity, Entity entity, ItemStack tool, CallbackInfo ci) {
        if (entity instanceof LivingEntity le) {
            var eff = le.getEffect(SleepTight.INVIGORATED.get());
            if (eff != null) InvigoratedEffect.BLOCK_XP_LEVEL.set(eff.getAmplifier());
        }
    }

    @Inject(method = "dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;spawnAfterBreak(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;Z)V",
                    shift = At.Shift.AFTER))
    private static void unsetXpHack(BlockState state, Level level, BlockPos pos, BlockEntity blockEntity, Entity entity, ItemStack tool, CallbackInfo ci) {
        InvigoratedEffect.BLOCK_XP_LEVEL.remove();
    }

    @Inject(method = "dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/storage/loot/LootContext$Builder;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;spawnAfterBreak(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;Z)V",
                    shift = At.Shift.BEFORE))
    private static void setXpHackLoot(BlockState state, LootContext.Builder context, CallbackInfo ci) {
        Entity entity = context.getOptionalParameter(LootContextParams.THIS_ENTITY);
        if (entity instanceof LivingEntity le) {
            var eff = le.getEffect(SleepTight.INVIGORATED.get());
            if (eff != null) InvigoratedEffect.BLOCK_XP_LEVEL.set(eff.getAmplifier());
        }
    }

    @Inject(method = "dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/storage/loot/LootContext$Builder;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;spawnAfterBreak(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;Z)V",
                    shift = At.Shift.AFTER))
    private static void unsetXpHackLoot(BlockState state, LootContext.Builder lootContextBuilder, CallbackInfo ci) {
        InvigoratedEffect.BLOCK_XP_LEVEL.remove();

    }

    @Inject(method = "popExperience", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ExperienceOrb;award(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/phys/Vec3;I)V",
            shift = At.Shift.AFTER))
    protected void addInvigoratedXP(ServerLevel level, BlockPos pos, int amount, CallbackInfo ci) {
        InvigoratedEffect.onBlcokXpDropped(level, pos, amount);
    }
}