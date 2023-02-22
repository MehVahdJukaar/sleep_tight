package net.mehvahdjukaar.sleep_tight;

import net.mehvahdjukaar.moonlight.api.misc.EventCalled;
import net.mehvahdjukaar.sleep_tight.common.*;
import net.mehvahdjukaar.sleep_tight.configs.CommonConfigs;
import net.mehvahdjukaar.sleep_tight.network.ClientBoundSyncPlayerSleepCapMessage;
import net.mehvahdjukaar.sleep_tight.network.NetworkHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class ModEvents {

    @EventCalled
    public static long getTimeFromSleepFinished(ServerLevel level, long newTime) {
        //hammocks wake time
        if (level.isDay()) {
            boolean success = false;

            for (Player player : level.players()) {
                var p = player.getSleepingPos();
                if (p.isPresent() && player.isSleepingLongEnough() &&
                        level.getBlockState(p.get()).getBlock() instanceof HammockBlock) {
                    success = true;
                    break;
                }
            }
            if (success) {
                long i = level.getDayTime() + 24000L;
                return (i - i % 24000L) - 12001L;
            }
        }
        //nightmares
        double chances = 0;
        int players = 0;
        for (var player : level.players()) {
            if (player.isSleepingLongEnough()) {
                players++;
                PlayerSleepCapability c = SleepTightPlatformStuff.getPlayerSleepCap(player);
                chances += c.getNightmareChance(player);
            }
        }
        double chance = players == 0 ? 0 : chances / players;
        boolean nightmare = level.random.nextFloat() < chance;

        for (var player : level.players()) {
            if (player.isSleepingLongEnough()) {
               if(nightmare) onNightmare(player);
               else onPlayerSleepFinished(player);
            }
        }

        if (nightmare) {
            long i = level.getDayTime();
            return (i + (long) ((newTime-i) * CommonConfigs.NIGHTMARE_SLEEP_TIME_MULTIPLIER.get()));
        }


        return newTime;
    }


    @EventCalled
    public static boolean canSetSpawn(Player player, @Nullable BlockPos pos) {
        if (pos != null) {
            Level level = player.getLevel();
            if (!level.isClientSide) {
                return !(level.getBlockState(pos).getBlock() instanceof NightBagBlock);
            }
        }
        return true;
    }

    @EventCalled
    public static InteractionResult onCheckSleepTime(Level level, BlockPos pos) {
        long t = level.getDayTime() % 24000L;
        if (level.getBlockState(pos).getBlock() instanceof HammockBlock) {
            if (t > 500L && t < 11500L) {
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.CONSUME;
    }

    @EventCalled
    public static InteractionResult onRightClickBlock(Player player, Level level, InteractionHand hand, BlockHitResult hitResult) {
        if (!player.isSpectator()) { //is this check even needed?
            BlockPos pos = hitResult.getBlockPos();
            var state = level.getBlockState(pos);
            Block b = state.getBlock();
            if (state.getBlock() instanceof BedBlock) {
                var c = SleepTightPlatformStuff.getPlayerSleepCap(player);
                if(c.getInsomniaCooldown(player.level)>0){
                    player.displayClientMessage(Component.translatable("message.sleep_tight.insomnia"),true);
                    return InteractionResult.sidedSuccess(level.isClientSide);
                }

                if (state.getValue(BedBlock.PART) != BedPart.HEAD) {
                    pos = pos.relative(state.getValue(BedBlock.FACING));
                    state = level.getBlockState(pos);
                    if (!state.is(b)) {
                        return InteractionResult.PASS;
                    }
                }

                BedEntity.layDown(state, level, pos, player);
                //always success to prevent use action
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    @EventCalled
    public static Vec3 getSleepingPosition(BlockState state, BlockPos pos) {
        if (state.getBlock() instanceof IModBed hammockBlock) {
            return hammockBlock.getSleepingPosition(state, pos);
        } else if (state.is(BlockTags.BEDS) && CommonConfigs.FIX_BED_POSITION.get()) {
            //vanilla places player 2 pixels above bed. Player then falls down
            return new Vec3(pos.getX() + 0.5, pos.getY() + 9 / 16f, pos.getZ() + 0.5);
        }
        return null;
    }


    private static void onNightmare(ServerPlayer player) {
        var c = SleepTightPlatformStuff.getPlayerSleepCap(player);
        c.addNightmare(player.level);
        c.syncToClient(player);
        player.displayClientMessage(Component.translatable("message.sleep_tight.nightmare"),true);
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS,20*3, 0, false, false, false,
              null, Optional.of( new MobEffectInstance.FactorData(20, 10,1,1,20*3,1,true))));
    }

    //server sided
    @EventCalled
    public static void onPlayerSleepFinished(ServerPlayer player) {
        var p = player.getSleepingPos();
        if (p.isPresent()) {
            BlockPos pos = p.get();
            if (player.level.getBlockEntity(pos) instanceof ISleepTightBed tile) {
                BedCapability bedCap = tile.getBedCap();
                PlayerSleepCapability playerCap = SleepTightPlatformStuff.getPlayerSleepCap(player);
                playerCap.assignHomeBed(bedCap.getId());
                playerCap.increaseNightSlept(player.level);
                bedCap.increaseTimeSleptOn(player);
                playerCap.syncToClient(player);
            }
        }
    }

    //called regardless of sleep finished or not. both sides
    @EventCalled
    public static void onWokenUp(Player player, boolean hasWokenUpImmediately) {
        var p = player.getSleepingPos();
        if (p.isPresent()) {
            BlockPos pos = p.get();
            BlockState state = player.level.getBlockState(pos);
            if (state.getBlock() instanceof IModBed bed) {
                bed.onWokenUp(state, pos, player);
            }
        }
    }

    //true if spawn should be cancelled
    @EventCalled
    public static boolean shouldCancelSetSpawn(Player entity, BlockPos newSpawn) {
        if (entity.getLevel().getBlockState(newSpawn).getBlock() instanceof IModBed bed) {
            return !bed.canSetSpawn();
        }
        return false;
    }

    @EventCalled
    public static void onPlayerLoggedIn(ServerPlayer player) {
        NetworkHandler.CHANNEL.sendToClientPlayer(player,
                new ClientBoundSyncPlayerSleepCapMessage(player));
    }

@EventCalled
    public static boolean onCheckSleepCondition(Player player) {
        if(SleepTightPlatformStuff.getPlayerSleepCap(player).getInsomniaCooldown(player.level)>0){

            player.displayClientMessage(Component.translatable("message.sleep_tight.insomina"),true);
            return false;
        }
        return true;
    }
}
