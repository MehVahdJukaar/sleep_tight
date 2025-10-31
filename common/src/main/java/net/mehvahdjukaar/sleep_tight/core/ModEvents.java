package net.mehvahdjukaar.sleep_tight.core;

import net.mehvahdjukaar.moonlight.api.misc.EventCalled;
import net.mehvahdjukaar.sleep_tight.STPlatStuff;
import net.mehvahdjukaar.sleep_tight.SleepTight;
import net.mehvahdjukaar.sleep_tight.client.ClientEvents;
import net.mehvahdjukaar.sleep_tight.common.InvigoratedEffect;
import net.mehvahdjukaar.sleep_tight.common.blocks.IModBed;
import net.mehvahdjukaar.sleep_tight.common.blocks.ISleepTightBed;
import net.mehvahdjukaar.sleep_tight.common.blocks.InfestedBedBlock;
import net.mehvahdjukaar.sleep_tight.common.blocks.NightBagBlock;
import net.mehvahdjukaar.sleep_tight.common.entities.BedEntity;
import net.mehvahdjukaar.sleep_tight.common.items.BedbugEggsItem;
import net.mehvahdjukaar.sleep_tight.common.network.ClientBoundNightmarePacket;
import net.mehvahdjukaar.sleep_tight.common.network.ClientBoundParticleMessage;
import net.mehvahdjukaar.sleep_tight.common.network.ClientBoundSyncPlayerSleepCapMessage;
import net.mehvahdjukaar.sleep_tight.common.network.NetworkHandler;
import net.mehvahdjukaar.sleep_tight.configs.CommonConfigs;
import net.mehvahdjukaar.sleep_tight.integration.HandcraftedCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.LingeringPotionItem;
import net.minecraft.world.item.SplashPotionItem;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BedBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
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
                PlayerSleepData c = STPlatStuff.getPlayerSleepData(player);
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
                case SLEPT_SUCCESSFULLY -> onPlayerSleepFinished(player, dayTimeDelta, newWakeTime);
                case ENCOUNTER -> onEncounter(player, encounterSpawnedFor.contains(player), newWakeTime);
                case NIGHTMARE -> onNightmare(player, newWakeTime);
            }
        }

        return newWakeTime;
    }

    @EventCalled
    public static boolean canSetSpawn(Player player, @Nullable BlockPos pos) {
        if (pos != null) {
            Level level = player.level();
            Block block = level.getBlockState(pos).getBlock();

            if (block instanceof BedBlock) {
                if (CommonConfigs.ONLY_RESPAWN_IN_HOME_BED.get()) {
                    PlayerSleepData pd = STPlatStuff.getPlayerSleepData(player);
                    if (pd.isBedLastSleptInto(STPlatStuff.getBedData(level, pos))) {
                        return false;
                    }
                }
                if (!BedBlock.canSetSpawn(level) && !CommonConfigs.EXPLOSION_BEHAVIOR.get().canRespawn()) {
                    return false;
                }
            }
            if ((block instanceof NightBagBlock)) {
                return false;
            }
        }
        //pass
        return true;
    }


    @Nullable
    @EventCalled
    public static InteractionResult onRightClickBlock(Player player, Level level, InteractionHand hand, BlockHitResult hitResult) {
        if (player.isSpectator()) return null;//is this check even needed?
        BlockPos pos = hitResult.getBlockPos();
        var state = level.getBlockState(pos);
        Block b = state.getBlock();

        if (b instanceof InfestedBedBlock) {
            return state.use(level, player, hand, hitResult);
        }

        BedData data = STPlatStuff.getBedData(level, pos);
        if (data == null) return null;
        if (data.isInfested()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.getItem() instanceof LingeringPotionItem || stack.getItem() instanceof SplashPotionItem) {
                return InteractionResult.PASS;
            }
            player.displayClientMessage(Component.translatable("message.sleep_tight.bedbug"), true);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        Direction dir = state.getValue(BedBlock.FACING);

        if (SleepTight.HANDCRAFTED) {
            InteractionResult ret = HandcraftedCompat.placeSheet(state, pos, player, hand, hitResult);
            if (ret != InteractionResult.PASS) {
                return ret;
            }
        }

        //get head
        pos = getBedHead(state, pos);
        state = level.getBlockState(pos);
        if (!state.is(b)) {
            return InteractionResult.PASS;
        }
        //bed bug egg infestation
        ItemStack itemInHand = player.getItemInHand(hand);
        if (itemInHand.getItem() instanceof BedbugEggsItem bb) {
            return bb.useOnBed(player, hand, itemInHand, state, pos, hitResult);
        }


        //fallsback on bed logic for non sleep action
        if (BedBlock.canSetSpawn(level) && !player.isSecondaryUseActive() && !bedBlocked(level, pos, dir)) {

            //tries clearing double bed
            boolean occupied = state.getValue(BedBlock.OCCUPIED);
            if (occupied) {
                var list = level.getEntitiesOfClass(BedEntity.class, new AABB(pos));
                if (!list.isEmpty()) {
                    BedEntity bedEntity = list.get(0);
                    if (!bedEntity.isDoubleBed()) return InteractionResult.PASS;

                    //assumes other state is valid because bed would have noticed otherwise
                    bedEntity.clearDoubleBed();

                    pos = bedEntity.getDoubleBedPos();
                    state = state.setValue(BedBlock.OCCUPIED, false);
                    level.setBlockAndUpdate(pos, state);

                    occupied = false;

                } else {
                    BlockPos doublePos = BedEntity.getInverseDoubleBedPos(pos, state);
                    list = level.getEntitiesOfClass(BedEntity.class, new AABB(doublePos));
                    if (!list.isEmpty()) {
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

                BedEntity.layDown(state, pos, player);
                //always success to prevent use action
                return InteractionResult.SUCCESS;
            }
        }
        return null;
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
        if (entity.level().isClientSide) ClientEvents.onSleepStarted(entity, state, pos);
        if (state.getBlock() instanceof IModBed iModBed) {
            return iModBed.getSleepingPosition(state, pos);
        } else if (state.is(BlockTags.BEDS)) {
            Vec3 c = Vec3.ZERO;
            //vanilla places player 2 pixels above bed. Player then falls down
            if (CommonConfigs.FIX_BED_POSITION.get()) {
                c = c.add(pos.getX() + 0.5, pos.getY() + 9 / 16f, pos.getZ() + 0.5);
            }
            if (entity instanceof Player player) {
                PlayerSleepData data = STPlatStuff.getPlayerSleepData(player);
                if (data.usingDoubleBed()) {
                    c = BedEntity.getDoubleBedOffset(state.getValue(BedBlock.FACING), c);
                }
            }
            if (c != Vec3.ZERO) return c;
        }
        return null;
    }


    private static void onEncounter(ServerPlayer player, boolean mobSpawned, long wakeTime) {
        if (mobSpawned) {
            var c = STPlatStuff.getPlayerSleepData(player);
            c.setInsomniaCooldown(wakeTime, CommonConfigs.ENCOUNTER_INSOMNIA_DURATION.get());
            c.setLasWokenUpTime(wakeTime);
            c.resetConsecutiveNightSleptCounter();

            c.syncToClient(player);
        } else {
            player.displayClientMessage(Component.translatable("message.sleep_tight.encounter"), true);
        }
    }

    private static void onNightmare(ServerPlayer player, long wakeTime) {
        var c = STPlatStuff.getPlayerSleepData(player);
        c.setInsomniaCooldown(wakeTime, CommonConfigs.NIGHTMARE_INSOMNIA_DURATION.get());
        c.setLasWokenUpTime(wakeTime);
        c.resetConsecutiveNightSleptCounter();

        c.syncToClient(player);
        player.displayClientMessage(Component.translatable("message.sleep_tight.nightmare"), true);
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 20 * 3, 0, false, false, false,
                null, Optional.of(new MobEffectInstance.FactorData(20, 10, 1, 1, 20 * 3, 1, true))));
        NetworkHandler.CHANNEL.sendToClientPlayer(player, new ClientBoundNightmarePacket());
    }

    //server sided
    public static void onPlayerSleepFinished(ServerPlayer player, long dayTimeDelta, long wakeUpTime) {
        var p = player.getSleepingPos();
        if (p.isPresent()) {
            BlockPos pos = p.get();
            PlayerSleepData playerCap = STPlatStuff.getPlayerSleepData(player);
            Level level = player.level();
            BlockState state = level.getBlockState(pos);
            ISleepTightBed bed = (ISleepTightBed) Blocks.RED_BED;
            if (state.getBlock() instanceof ISleepTightBed b) {
                bed = b;
            }
            BedData data = STPlatStuff.getBedData(level, pos);
            if (data != null) {
                playerCap.increaseNightSleptInThisBed(data, player);
            }

            playerCap.increaseConsecutiveNightSleptCounter(wakeUpTime);
            playerCap.setLasWokenUpTime(wakeUpTime);

            if (bed.st_canSpawnBedbugs()) {
                WakeUpEncounterHelper.trySpawningBedbug(pos, player, data);
            }

            SleepEffectsHelper.applyEffectsOnWakeUp(playerCap, player, dayTimeDelta, pos, bed, state, data);

            playerCap.setInsomniaCooldown(wakeUpTime, bed.st_getCooldown());
            playerCap.syncToClient(player);
        }
    }

    //called regardless of sleep finished or not. both sides
    @EventCalled
    public static void onWokenUp(Player player, boolean hasWokenUpImmediately) {
        var p = player.getSleepingPos();
        if (p.isPresent()) {
            BlockPos pos = p.get();
            Level level = player.level();
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof IModBed bed) {
                bed.onLeftBed(state, pos, player);
            }
            //clears double beds
            else if (state.is(BlockTags.BEDS) && player instanceof ServerPlayer serverPlayer) {
                var data = STPlatStuff.getPlayerSleepData(player);
                if (data.usingDoubleBed()) {
                    BlockPos doublePos = BedEntity.getDoubleBedPos(pos, state);
                    BlockState doubleState = level.getBlockState(doublePos);
                    if (doubleState.is(BlockTags.BEDS)) {
                        doubleState = doubleState.setValue(BedBlock.OCCUPIED, false);
                        if (doubleState == state) {
                            level.setBlockAndUpdate(doublePos, doubleState);
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
        if (entity.level().getBlockState(newSpawn).getBlock() instanceof IModBed bed) {
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
        Level level = player.level();
        BedData bedData = STPlatStuff.getBedData(level, bedPos);
        if (bedData != null && bedData.isInfested()) {
            player.displayClientMessage(Component.translatable("message.sleep_tight.bedbug"), true);
            return false;
        }
        if (STPlatStuff.getPlayerSleepData(player).isOnSleepCooldown(player)) {
            if (!player.level().isClientSide) {
                String s = isDayTime(player.level()) ? "message.sleep_tight.insomnia.day" :
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
        if (!entity.isRemoved() && entity.level() instanceof ServerLevel serverLevel) {
            if (killer instanceof LivingEntity le && killer.killedEntity(serverLevel, entity)) {
                InvigoratedEffect.onLivingDeath(serverLevel, entity, le);
            }
        }
    }


    public static boolean isDayTime(Level level) {
        int dayDuration = Level.TICKS_PER_DAY;
        long dayTime = level.getDayTime() % dayDuration;
        if (dayTime > 100L && dayTime < (dayDuration / 2 - 100)) {
            return true;
        }
        return false;
    }


    public static void onPlayerRespawned(ServerPlayer newPlayer) {
        if (CommonConfigs.RESPAWN_LAYING.get()) {
            BlockPos pos = newPlayer.getRespawnPosition();
            if (pos != null) {
                BlockState state = newPlayer.level().getBlockState(pos);
                BedData bedData = STPlatStuff.getBedData(newPlayer.level(), pos);
                if (bedData != null) {
                    BedEntity.layDown(state, pos, newPlayer);
                }
            }
        }
    }

    //all called by mixins

    @SuppressWarnings("all")
    @Nullable
    public static Optional<Vec3> findSpawnPosition(ServerPlayer player, BlockPos spawnBlockPos, boolean isRespawnForced) {
        if (!isRespawnForced && (CommonConfigs.ONLY_RESPAWN_IN_HOME_BED.get() || true)) {
            BedData bedData = STPlatStuff.getBedData(player.level(), spawnBlockPos);
            if (bedData != null && !STPlatStuff.getPlayerSleepData(player).isBedLastSleptInto(bedData)) {
                return Optional.empty();
            }
        }
        return null;
    }


    public static void onProjectileHitBed(Projectile projectile, Level level, BlockState state, BlockHitResult hit) {
        if (projectile instanceof ThrownPotion tp) {
            BlockPos myPos = hit.getBlockPos();
            BlockPos pos = getBedHead(state, myPos);
            BedData data = STPlatStuff.getBedData(level, pos);
            if (data != null && data.isInfested()) {
                Potion p = PotionUtils.getPotion(tp.getItem());
                if (p.getEffects().stream().anyMatch(e -> e.getEffect() == MobEffects.HARM)) {
                    level.playSound(null, pos, SoundEvents.SILVERFISH_DEATH, SoundSource.BLOCKS, 1, 1.3f);
                    if (!level.isClientSide) {
                        NetworkHandler.CHANNEL.sendToAllClientPlayersInRange(level, pos, 32,
                                ClientBoundParticleMessage.bedbugInfest(pos,
                                        state.getValue(BedBlock.FACING).getOpposite()));
                    }
                    data.setBedBug(null);
                    //sync to clients
                    BlockEntity tile = level.getBlockEntity(pos);
                    if (tile != null) {
                        level.sendBlockUpdated(pos, state, state, 3);
                        tile.setChanged();
                    }
                }
            }
        }
    }

    @EventCalled
    public static void spawnAfterBreakBed(BlockState state, ServerLevel level, BlockPos pos, @Nullable BlockEntity be) {
        if (level.isClientSide) return;
        BedData data = STPlatStuff.getBedData(level, pos, be);
        //just head does it
        if (data != null && data.isInfested() && level.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS)) {
            CompoundTag tag = data.getBedBug();
            var opt = EntityType.create(tag, level);
            if (opt.isEmpty()) return;
            Entity entity = opt.get();
            pos = bedBedFeet(state, pos);
            entity.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
            level.addFreshEntity(entity);
            if (entity instanceof Mob le) {
                le.spawnAnim();
            }
        }
    }

    public static void animateTickBed(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextFloat() < 0.3) {
            BedData data = STPlatStuff.getBedData(level, pos);
            if (data != null && data.isInfested()) {
                float x = pos.getX() + level.random.nextFloat();
                float z = pos.getZ() + level.random.nextFloat();
                double y = state.getCollisionShape(level, pos).max(Direction.Axis.Y) + pos.getY();
                level.addParticle(SleepTight.BEDBUG_PARTICLE.get(), x, y + 0.01, z, 0, 0, 0);
            }
        }
    }

    public static BlockPos getBedHead(BlockState bed, BlockPos pos) {
        if (!bed.hasProperty(BedBlock.PART)) {
            return pos;
        }
        BedPart part = bed.getValue(BedBlock.PART);
        if (part == BedPart.HEAD) return pos;
        Direction dir = bed.getValue(BedBlock.FACING);
        return pos.relative(dir);
    }

    public static BlockPos bedBedFeet(BlockState bed, BlockPos pos) {
        if (!bed.hasProperty(BedBlock.PART)) {
            return pos;
        }
        BedPart part = bed.getValue(BedBlock.PART);
        if (part == BedPart.FOOT) return pos;
        Direction dir = bed.getValue(BedBlock.FACING);
        return pos.relative(dir.getOpposite());
    }

    public static boolean shouldHaveBedData(BlockEntity blockEntity) {
        BlockState state = blockEntity.getBlockState();
        if (state.is(SleepTight.LAYING_BED_BLACKLIST)) return false;
        return (state.getBlock() instanceof BedBlock && state.getValue(BedBlock.PART) == BedPart.HEAD)
                || (blockEntity instanceof BedBlockEntity be && be.getBlockState().hasProperty(BedBlock.PART) &&
                be.getBlockState().getValue(BedBlock.PART) == BedPart.HEAD);
    }
}
