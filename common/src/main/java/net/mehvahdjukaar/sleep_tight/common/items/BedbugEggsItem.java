package net.mehvahdjukaar.sleep_tight.common.items;

import net.mehvahdjukaar.sleep_tight.STPlatStuff;
import net.mehvahdjukaar.sleep_tight.SleepTight;
import net.mehvahdjukaar.sleep_tight.common.entities.BedbugEntity;
import net.mehvahdjukaar.sleep_tight.core.BedData;
import net.mehvahdjukaar.sleep_tight.core.ModEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class BedbugEggsItem extends Item {
    public BedbugEggsItem(Properties properties) {
        super(properties);
    }

    public InteractionResult useOnBed(Player player, InteractionHand hand, ItemStack stack, BlockState state,
                                      BlockPos pos, BlockHitResult hit) {
        Level level = player.level();
        if (infestBed(level, pos, null)) {
            player.awardStat(Stats.ITEM_USED.get(this));
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            level.playSound(player, pos, SoundEvents.TURTLE_EGG_CRACK, SoundSource.PLAYERS, 0.6f, 1.7f);
            level.playSound(player, pos, SoundEvents.SILVERFISH_STEP, SoundSource.PLAYERS, 1, 1f);

            if (level.isClientSide) {
                Vec3 h = hit.getLocation();
                for (int i = 0; i < 8; ++i) {
                    level.addParticle(new ItemParticleOption(ParticleTypes.ITEM, this.getDefaultInstance()),
                            h.x, h.y + 0.1, h.z,
                            (level.random.nextFloat() - 0.5) * 0.08,
                            (level.random.nextFloat() - 0.5) * 0.08,
                            (level.random.nextFloat() - 0.5) * 0.08);
                }
            }

            return InteractionResult.SUCCESS;
            //particles
        }
        return InteractionResult.PASS;
    }

    public static boolean infestBed(Level level, BlockPos pos, @Nullable BedbugEntity entity) {
        BedData data = STPlatStuff.getBedDataIfPresent(level, pos);
        if (data != null && !data.isInfested()) {
            CompoundTag mobTag;
            if (entity != null) {
                mobTag = prepareMobTagForContainer(entity, 0.5);
            } else {
                mobTag = new CompoundTag();
                mobTag.putString("id", SleepTight.BEDBUG_ENTITY.getId().toString());
            }
            data.setBedBug(mobTag);
            ModEvents.syncBedDataToClients(level.getBlockEntity(pos));
            return true;
        }

        return false;
    }


    private static CompoundTag prepareMobTagForContainer(Entity entity, double yOffset) {
        //set post relative to center block cage
        double px = 0.5;
        double py = yOffset + 0.0001;
        double pz = 0.5;
        entity.setPos(px, py, pz);
        entity.xOld = px;
        entity.yOld = py;
        entity.zOld = pz;

        if (entity.isPassenger()) {
            entity.getVehicle().ejectPassengers();
        }

        //prepares entity
        if (entity instanceof LivingEntity le) {
            le.yHeadRotO = 0;
            le.yHeadRot = 0;
            le.walkAnimation.setSpeed(0);
            le.hurtDuration = 0;
            le.hurtTime = 0;
            le.attackAnim = 0;
        }
        entity.setYRot(0);
        entity.yRotO = 0;
        entity.xRotO = 0;
        entity.setXRot(0);
        entity.clearFire();
        entity.invulnerableTime = 0;

        CompoundTag mobTag = new CompoundTag();
        entity.save(mobTag);
        if (mobTag.isEmpty()) {
            return null;
        }
        mobTag.remove("Passengers");
        mobTag.remove("Leash");
        mobTag.remove("UUID");//TODO: UUID
        return mobTag;
    }

}
