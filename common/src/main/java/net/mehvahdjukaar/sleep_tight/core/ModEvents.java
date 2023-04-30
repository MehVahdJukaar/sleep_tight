package net.mehvahdjukaar.sleep_tight.core;

import net.mehvahdjukaar.moonlight.api.misc.EventCalled;
import net.mehvahdjukaar.sleep_tight.SleepTight;
import net.mehvahdjukaar.sleep_tight.SleepTightPlatformStuff;
import net.mehvahdjukaar.sleep_tight.client.ClientEvents;
import net.mehvahdjukaar.sleep_tight.common.InvigoratedEffect;
import net.mehvahdjukaar.sleep_tight.common.blocks.IModBed;
import net.mehvahdjukaar.sleep_tight.common.blocks.ISleepTightBed;
import net.mehvahdjukaar.sleep_tight.common.blocks.InfestedBedBlock;
import net.mehvahdjukaar.sleep_tight.common.blocks.NightBagBlock;
import net.mehvahdjukaar.sleep_tight.common.entities.BedEntity;
import net.mehvahdjukaar.sleep_tight.common.items.BedbugEggsItem;
import net.mehvahdjukaar.sleep_tight.common.network.ClientBoundNightmarePacket;
import net.mehvahdjukaar.sleep_tight.common.network.ClientBoundSyncPlayerSleepCapMessage;
import net.mehvahdjukaar.sleep_tight.common.network.NetworkHandler;
import net.mehvahdjukaar.sleep_tight.common.tiles.IExtraBedDataProvider;
import net.mehvahdjukaar.sleep_tight.configs.CommonConfigs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.PlayerRespawnLogic;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.AABB;
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
        WakeReason wakeReason = WakeReason.SLEPT_SUCCESSFULLY;

        List<ServerPlayer> sleepingPlayers = level.players().stream()
                .filter(Player::isSleepingLongEnough)
                .toList();

        //find first valid. Assumes they are all the same
        ISleepTightBed firstValid = (ISleepTightBed) Blocks.RED_BED;
        for (Player player : sleepingPlayers) {
            if (level.getBlockState(player.getSleepingPos().get()).getBlock() instanceof ISleepTightBed st) {
                firstValid = st;
                break;
            }
        }

        Set<Player> encounterSpawnedFor = new HashSet<>();

        //encounter
        for (var player : sleepingPlayers) {
            if (player.gameMode.isSurvival()) {
                if (WakeUpEncounterHelper.tryPerformEncounter(player, level, player.getSleepingPos().get())) {
                    wakeReason = WakeReason.ENCOUNTER;
                    encounterSpawnedFor.add(player);
                }
            }
        }

        if (wakeReason == WakeReason.SLEPT_SUCCESSFULLY) {
            //nightmares
            double chances = 0;
            int players = 0;
            for (var player : sleepingPlayers) {
                players++;
                PlayerSleepData c = SleepTightPlatformStuff.getPlayerSleepData(player);
                chances += c.getNightmareChance(player, player.getSleepingPos().get());
            }
            double nightmareChance = players == 0 ? 0 : chances / players;

            if (level.random.nextFloat() < nightmareChance) wakeReason = WakeReason.NIGHTMARE;
        }

        long sleepDayTime = level.getDayTime();
        long newWakeTime = firstValid.st_modifyWakeUpTime(wakeReason, newTimeDayTime, sleepDayTime);
        long dayTimeDelta = ((newWakeTime + 24000) - sleepDayTime) % 24000;

        for (var player : sleepingPlayers) {
            switch (wakeReason) {
                case SLEPT_SUCCESSFULLY -> onPlayerSleepFinished(player, dayTimeDelta);
                case ENCOUNTER -> onEncounter(player, encounterSpawnedFor.contains(player));
                case NIGHTMARE -> onNightmare(player);
            }
        }

        return newWakeTime;
    }

    @EventCalled
    public static boolean canSetSpawn(Player player, @Nullable BlockPos pos) {
        if (pos != null) {
            if (!BedBlock.canSetSpawn(player.level) && !CommonConfigs.EXPLOSION_BEHAVIOR.get().canRespawn()) {
                return false;
            }
            Level level = player.getLevel();
            if (!level.isClientSide) {
                return !(level.getBlockState(pos).getBlock() instanceof NightBagBlock);
            }
        }
        return true;
    }

    public static boolean isValidBed(BlockState state) {
        Block block = state.getBlock();
        return block instanceof BedBlock && block != SleepTight.INFESTED_BED;
    }

    @EventCalled
    public static InteractionResult onRightClickBlock(Player player, Level level, InteractionHand hand, BlockHitResult hitResult) {
        if (!player.isSpectator()) { //is this check even needed?
            BlockPos pos = hitResult.getBlockPos();
            var state = level.getBlockState(pos);
            Block b = state.getBlock();

            if(b instanceof InfestedBedBlock){
                return state.use(level, player, hand,hitResult);
            }

            if (isValidBed(state)) {
                Direction dir = state.getValue(BedBlock.FACING);

                //get head
                if (state.getValue(BedBlock.PART) != BedPart.HEAD) {
                    pos = pos.relative(dir);
                    state = level.getBlockState(pos);
                    if (!state.is(b)) {
                        return InteractionResult.PASS;
                    }
                }
                //bed bug egg infestation
                ItemStack itemInHand = player.getItemInHand(hand);
                if (itemInHand.getItem() instanceof BedbugEggsItem bb) {
                    return bb.useOnBed(player, hand, itemInHand, state, pos, hitResult);
                }

                //fallsback on bed logic for non sleep action
                if (BedBlock.canSetSpawn(level) && !player.isSecondaryUseActive() && !bedBlocked(level, pos, dir) &&
                        !player.isCrouching()) {

                    //tries clearing double bed
                    boolean occupied = state.getValue(BedBlock.OCCUPIED);
                    if (occupied) {
                        var list = level.getEntitiesOfClass(BedEntity.class, new AABB(pos));
                        if (list.size() > 0) {
                            BedEntity bedEntity = list.get(0);
                            if (!bedEntity.isDoubleBed()) return InteractionResult.PASS;

                            //assumes other state is valid because bed would have noticed otherwise
                            bedEntity.clearDoubleBed();
                            state = state.setValue(BedBlock.OCCUPIED, false);
                            pos = bedEntity.getDoubleBedPos();
                            level.setBlockAndUpdate(pos, state);

                            occupied = false;

                        } else {
                            BlockPos doublePos = BedEntity.getInverseDoubleBedPos(pos, state);
                            list = level.getEntitiesOfClass(BedEntity.class, new AABB(doublePos));
                            if (list.size() > 0) {
                                BedEntity bedEntity = list.get(0);
                                if (!bedEntity.isDoubleBed()) return InteractionResult.PASS;

                                bedEntity.clearDoubleBed();
                                state = state.setValue(BedBlock.OCCUPIED, false);
                                level.setBlockAndUpdate(pos, state);

                                occupied = false;
                            }
                        }
                    }

                    if (!occupied) {

                        boolean extraConditions = CommonConfigs.LAY_WHEN_ON_COOLDOWN.get() ||
                                checkExtraSleepConditions(player, pos);
                        if (!extraConditions) return InteractionResult.sidedSuccess(level.isClientSide);

                        BedEntity.layDown(state, level, pos, player);
                        //always success to prevent use action
                        return InteractionResult.SUCCESS;
                    }
                }
            }
        }
        return InteractionResult.PASS;
    }

    private static boolean bedBlocked(Level level, BlockPos pos, Direction direction) {
        BlockPos blockPos = pos.above();
        return !freeAt(level, blockPos) || !freeAt(level, blockPos.relative(direction.getOpposite()));
    }


    protected static boolean freeAt(Level level, BlockPos pos) {
        return !level.getBlockState(pos).isSuffocating(level, pos);
    }

    @EventCalled
    public static Vec3 getSleepingPosition(Entity entity, BlockState state, BlockPos pos) {
        //sleep started
        if (entity.level.isClientSide) ClientEvents.onSleepStarted(entity, state, pos);
        if (state.getBlock() instanceof IModBed iModBed) {
            return iModBed.getSleepingPosition(state, pos);
        } else if (state.is(BlockTags.BEDS)) {
            Vec3 c = Vec3.ZERO;
            //vanilla places player 2 pixels above bed. Player then falls down
            if (CommonConfigs.FIX_BED_POSITION.get()) {
                c = c.add(pos.getX() + 0.5, pos.getY() + 9 / 16f, pos.getZ() + 0.5);
            }
            if (entity instanceof Player player) {
                PlayerSleepData data = SleepTightPlatformStuff.getPlayerSleepData(player);
                if (data.usingDoubleBed()) {
                    c = BedEntity.getDoubleBedOffset(state.getValue(BedBlock.FACING), c);
                }
            }
            if (c != Vec3.ZERO) return c;
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
        NetworkHandler.CHANNEL.sendToClientPlayer(player, new ClientBoundNightmarePacket());
    }

    //server sided
    public static void onPlayerSleepFinished(ServerPlayer player, long dayTimeDelta) {
        var p = player.getSleepingPos();
        if (p.isPresent()) {
            BlockPos pos = p.get();
            PlayerSleepData playerCap = SleepTightPlatformStuff.getPlayerSleepData(player);
            BlockState state = player.level.getBlockState(pos);
            ISleepTightBed bed = (ISleepTightBed) Blocks.RED_BED;
            if (state.getBlock() instanceof ISleepTightBed b) {
                bed = b;
            }
            BedData data = null;
            if (player.level.getBlockEntity(pos) instanceof IExtraBedDataProvider tile) {
                data = tile.st_getBedData();
                playerCap.onNightSleptIntoBed(data, player);

            }

            if (bed.st_canSpawnBedbugs()) {
                WakeUpEncounterHelper.trySpawningBedbug(pos,  player, data);
            }

            SleepEffectsHelper.applyEffectsOnWakeUp(playerCap, player, dayTimeDelta, pos, bed, state, data);

            playerCap.addInsomnia(player, bed.st_getCooldown());
            playerCap.syncToClient(player);
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
            //clears double beds
            else if (state.is(BlockTags.BEDS) && player instanceof ServerPlayer serverPlayer) {
                var data = SleepTightPlatformStuff.getPlayerSleepData(player);
                if (data.usingDoubleBed()) {
                    BlockPos doublePos = BedEntity.getDoubleBedPos(pos, state);
                    BlockState doubleState = player.level.getBlockState(doublePos);
                    if (doubleState.is(BlockTags.BEDS)) {
                        doubleState = doubleState.setValue(BedBlock.OCCUPIED, false);
                        if (doubleState == state) {
                            player.level.setBlockAndUpdate(doublePos, doubleState);
                        }
                    }
                    data.setDoubleBed(false);
                    data.syncToClient(serverPlayer);
                }
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

    //similar to what below but isnt time related
    @EventCalled
    public static boolean checkExtraSleepConditions(Player player, @Nullable BlockPos bedPos) {
        if (SleepTightPlatformStuff.getPlayerSleepData(player).getInsomniaCooldown(player) > 0) {
            if (!player.level.isClientSide) {
                String s = isDayTime(player.level) ? "message.sleep_tight.insomnia.day" :
                        "message.sleep_tight.insomnia.night";
                player.displayClientMessage(Component.translatable(s), true);
            }
            return false;
        }
        if (!SleepEffectsHelper.checkExtraRequirements(player, bedPos)) return false;
        return true;
    }

    //this is responsible to check if player can sleep at this precise time
    @EventCalled
    public static InteractionResult onCheckSleepTime(Level level, BlockPos pos) {
        if (level.getBlockState(pos).getBlock() instanceof IModBed bed) {
            return bed.canSleepAtTime(level);
        }
        return InteractionResult.PASS;
    }

    @EventCalled
    public static void onEntityKilled(LivingEntity entity, Entity killer) {
        if (!entity.isRemoved() && entity.level instanceof ServerLevel serverLevel) {
            if (killer instanceof LivingEntity le && killer.wasKilled(serverLevel, entity)) {
                InvigoratedEffect.onLivingDeath(serverLevel, entity, le);
            }
        }
    }


    public static boolean isDayTime(Level level) {
        long dayTime = level.getDayTime() % 24000L;
        if (dayTime > 500L && dayTime < 11500L) {
            return true;
        }
        return false;
    }


}
