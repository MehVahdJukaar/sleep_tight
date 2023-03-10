package net.mehvahdjukaar.sleep_tight.mixins;

import com.google.common.collect.Lists;
import net.mehvahdjukaar.sleep_tight.common.entities.DreamerEssenceTargetEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.Comparator;
import java.util.List;

@Mixin(targets = "net.minecraft.world.entity.monster.Phantom$PhantomAttackPlayerTargetGoal")
public abstract class PhantomMixin {

    @ModifyArg(method = "canUse", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;getNearbyPlayers(Lnet/minecraft/world/entity/ai/targeting/TargetingConditions;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;")
    )
    public AABB checkForDreamerEssence(TargetingConditions predicate, LivingEntity phantom, AABB area) {
        List<DreamerEssenceTargetEntity> list = phantom.level.getEntitiesOfClass(DreamerEssenceTargetEntity.class, area,
                e -> ((Mob) phantom).getSensing().hasLineOfSight(e));
        if (!list.isEmpty()) {
            list.sort(Comparator.comparingDouble(Entity::getY));
            list = Lists.reverse(list);

            for (DreamerEssenceTargetEntity entity : list) {
                if (phantom.canAttack(entity, TargetingConditions.DEFAULT)) {
                    ((Phantom) phantom).setTarget(entity);
                    return phantom.getBoundingBox();
                }
            }
        }
        return area;
    }

}
