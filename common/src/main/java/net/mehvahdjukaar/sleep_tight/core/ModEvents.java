package net.mehvahdjukaar.sleep_tight.core;

import net.mehvahdjukaar.moonlight.api.misc.EventCalled;
import net.mehvahdjukaar.sleep_tight.SleepTightPlatformStuff;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class ModEvents {

    @EventCalled
    public static long getWakeUpTimeWhenSlept(ServerLevel level, long newTimeDayTime) {
        WakeReason wakeReason = WakeReason.DEFAULT;

        List<ServerPlayer> sleepingPlayers = level.players().stream().filter(Player::isSleepingLongEnough).toList();

        //hammocks wake time
        if (level.isDay()) {

            for (Player player : sleepingPlayers) {
                if (level.getBlockState(player.getSleepingPos().get()).getBlock() instanceof HammockBlock) {
                    wakeReason = WakeReason.SLEPT_IN_HAMMOCK;
                    break;
                }
            }
        }
        Set<Player> encounterSpawnedFor = new HashSet<>();

        if (wakeReason == WakeReason.DEFAULT) {

            //encounter
            for (var player : sleepingPlayers) {
                if (player.gameMode.isSurvival()) {
                    if (WakeUpEncounterHelper.tryPerformEncounter(player, level, player.getSleepingPos().get())) {
                        wakeReason = WakeReason.ENCOUNTER;
                        encounterSpawnedFor.add(player);
                    }
                }
            }
        }

        if (wakeReason == WakeReason.DEFAULT) {

            //nightmares
            double chances = 0;
            int players = 0;
            for (var player : level.players()) {
                players++;
                PlayerSleepData c = SleepTightPlatformStuff.getPlayerSleepData(player);
                chances += c.getNightmareChance(player);
            }
            double nightmareChance = players == 0 ? 0 : chances / players;

            if (level.random.nextFloat() < nightmareChance) wakeReason = WakeReason.NIGHTMARE;
        }

        long sleepDayTime = level.getDayTime();
        long newWakeTime = wakeReason.modifyWakeUpTime(newTimeDayTime, sleepDayTime);

        for (var player : sleepingPlayers) {
            switch (wakeReason) {
                case DEFAULT -> {
                    long dayTimeDelta = ((newWakeTime + 24000) - sleepDayTime) % 24000;
                    onPlayerSleepFinished(player, dayTimeDelta);
                }
                case SLEPT_IN_HAMMOCK -> onRestedInHammock(player);
                case ENCOUNTER -> onEncounter(player, encounterSpawnedFor.contains(player));
                case NIGHTMARE -> onNightmare(player);
            }
        }

        return newWakeTime;
    }


    private enum WakeReason {
        DEFAULT, SLEPT_IN_HAMMOCK, ENCOUNTER, NIGHTMARE;

        long modifyWakeUpTime(long newTime, long dayTime) {
            if (this == ENCOUNTER || this == NIGHTMARE) {
                double mult = this == ENCOUNTER ? CommonConfigs.ENCOUNTER_SLEEP_TIME_MULTIPLIER.get() :
                        CommonConfigs.NIGHTMARE_SLEEP_TIME_MULTIPLIER.get();

                return (dayTime + (long) ((newTime - dayTime) * mult));

            } else if (this == SLEPT_IN_HAMMOCK) {
                long i = dayTime + 24000L;
                return (i - i % 24000L) - 12001L;
            }
            return newTime;
        }
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
    public static InteractionResult onRightClickBlock(Player player, Level level, InteractionHand hand, BlockHitResult hitResult) {
        if (!player.isSpectator()) { //is this check even needed?
            BlockPos pos = hitResult.getBlockPos();
            var state = level.getBlockState(pos);
            Block b = state.getBlock();
            if (state.getBlock() instanceof BedBlock) {

                boolean extraConditions = checkExtraSleepConditions(player, pos);
                if (!extraConditions) return InteractionResult.sidedSuccess(level.isClientSide);

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


    private static void onEncounter(ServerPlayer player, boolean mobSpawned) {
        if (mobSpawned) {
            var c = SleepTightPlatformStuff.getPlayerSleepData(player);
            c.addInsomnia(player, CommonConfigs.ENCOUNTER_INSOMNIA_DURATION.get());
            c.syncToClient(player);
        } else {
            player.displayClientMessage(Component.translatable("message.sleep_tight.encounter"), true);
        }
    }

    private static void onNightmare(ServerPlayer player) {
        var c = SleepTightPlatformStuff.getPlayerSleepData(player);
        c.addInsomnia(player, CommonConfigs.NIGHTMARE_INSOMNIA_DURATION.get());
        c.syncToClient(player);
        player.displayClientMessage(Component.translatable("message.sleep_tight.nightmare"), true);
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 20 * 3, 0, false, false, false,
                null, Optional.of(new MobEffectInstance.FactorData(20, 10, 1, 1, 20 * 3, 1, true))));
    }

    private static void onRestedInHammock(ServerPlayer player) {
        PlayerSleepData playerCap = SleepTightPlatformStuff.getPlayerSleepData(player);
        playerCap.addInsomnia(player, CommonConfigs.HAMMOCK_COOLDOWN.get());
        playerCap.syncToClient(player);
    }

    //server sided
    public static void onPlayerSleepFinished(ServerPlayer player, long dayTimeDelta) {
        var p = player.getSleepingPos();
        if (p.isPresent()) {
            BlockPos pos = p.get();
            PlayerSleepData playerCap = SleepTightPlatformStuff.getPlayerSleepData(player);
            BlockEntity blockEntity = player.level.getBlockEntity(pos);
            if (blockEntity instanceof IVanillaBed tile) {
                playerCap.onNightSleptInto(tile.getBedData(), player);
            }
            playerCap.addInsomnia(player, CommonConfigs.BED_COOLDOWN.get());
            playerCap.syncToClient(player);

            SleepEffectsHelper.applyEffectsOnWakeUp(playerCap, player, dayTimeDelta, blockEntity);
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
                bed.onLeftBed(state, pos, player);
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
    public static boolean checkExtraSleepConditions(Player player, @Nullable BlockPos bedPos) {
        if (SleepTightPlatformStuff.getPlayerSleepData(player).getInsomniaCooldown(player) > 0) {
            if (player.level.isClientSide) {
                player.displayClientMessage(Component.translatable("message.sleep_tight.insomnia"), true);
            }
            return false;
        }
        if (!SleepEffectsHelper.checkExtraRequirements(player, bedPos)) return false;
        return true;
    }

    @EventCalled
    public static InteractionResult onCheckSleepTime(Level level, BlockPos pos) {
        if (level.getBlockState(pos).getBlock() instanceof HammockBlock) {
            long t = level.getDayTime() % 24000L;
            if (t > 500L && t < 11500L) {
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.FAIL;
        }
        return InteractionResult.SUCCESS;
        // return InteractionResult.PASS;
    }
}
