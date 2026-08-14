package net.mehvahdjukaar.sleep_tight.core;

import net.mehvahdjukaar.moonlight.api.misc.EventCalled;
import net.mehvahdjukaar.moonlight.api.platform.network.NetworkHelper;
import net.mehvahdjukaar.sleep_tight.STPlatStuff;
import net.mehvahdjukaar.sleep_tight.SleepTight;
import net.mehvahdjukaar.sleep_tight.client.ClientEvents;
import net.mehvahdjukaar.sleep_tight.common.InvigoratedEffect;
import net.mehvahdjukaar.sleep_tight.common.blocks.IModBed;
import net.mehvahdjukaar.sleep_tight.common.blocks.ISleepTightBed;
import net.mehvahdjukaar.sleep_tight.common.blocks.NightBagBlock;
import net.mehvahdjukaar.sleep_tight.common.entities.BedEntity;
import net.mehvahdjukaar.sleep_tight.common.items.BedbugEggsItem;
import net.mehvahdjukaar.sleep_tight.common.network.ClientBoundNightmarePacket;
import net.mehvahdjukaar.sleep_tight.common.network.ClientBoundParticleMessage;
import net.mehvahdjukaar.sleep_tight.configs.CommonConfigs;
import net.mehvahdjukaar.sleep_tight.integration.HandcraftedCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
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
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BedBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.StreamSupport;

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
                    if (!pd.isBedLastSleptInto(STPlatStuff.getBedDataIfPresent(level, pos))) {
                        return false;
                    }
                }
                var behavior = CommonConfigs.EXPLOSION_BEHAVIOR.get();
                if (!behavior.canRespawn()) {
                    if (behavior == CommonConfigs.ExplosionBehavior.ALLOWS_SLEEPING_NO_RESPAWN
                            || !BedBlock.canSetSpawn(level) || !level.dimensionType().natural()) {
                        return false;
                    }
                }
            }
            //has to cover all our beds, not just night bags. fabric has no other hook, so a hammock would
            //set a respawn point that resolves to nothing later and sends you to world spawn
            if (block instanceof IModBed modBed && !modBed.canSetSpawn()) {
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

        BedData data = STPlatStuff.getBedDataIfPresent(level, pos);
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
            var ret = HandcraftedCompat.placeSheet(state, pos, player, hand, hitResult).result();
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
                if (occupied) {
                    occupied = isReallyOccupied(level, pos, state);
                    if (!occupied) state = level.getBlockState(pos);
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

    //beds can get stuck occupied if the sleeper vanishes without waking up (chunk unload, crash, another mod
    //moving the block). only fix is breaking the bed, so we just clear the flag when nobody is in it
    public static boolean isReallyOccupied(Level level, BlockPos pos, BlockState state) {
        if (!state.hasProperty(BedBlock.OCCUPIED) || !state.getValue(BedBlock.OCCUPIED)) return false;
        //the entity that holds a player laying down, and the double bed one sits on the block next to it
        if (!level.getEntitiesOfClass(BedEntity.class, new AABB(pos).inflate(1.5)).isEmpty()) return true;
        for (var e : level.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(2))) {
            if (e.isSleeping()) return true;
        }
        if (!level.isClientSide) {
            level.setBlockAndUpdate(pos, state.setValue(BedBlock.OCCUPIED, false));
        }
        return false;
    }

    private static boolean bedBlocked(Level level, BlockPos pos, Direction direction) {
        BlockPos blockPos = pos.above();
        return !freeAt(level, blockPos) || !freeAt(level, blockPos.relative(direction.getOpposite()));
    }


    protected static boolean freeAt(Level level, BlockPos pos) {
        return !level.getBlockState(pos).isSuffocating(level, pos);
    }

    //absolute position, only for our own beds, where nothing else has a say on where the sleeper goes
    @Nullable
    @EventCalled
    public static Vec3 getSleepingPosition(Entity entity, BlockState state, BlockPos pos) {
        //sleep started
        if (entity.level().isClientSide) ClientEvents.onSleepStarted(entity, state, pos);
        if (state.getBlock() instanceof IModBed iModBed) {
            return iModBed.getSleepingPosition(state, pos);
        }
        return null;
    }

    //offset added on top of wherever the game put the sleeper, for vanilla and modded beds.
    //other mods move that position for good reasons (Sable pulls it out of its sublevels) so we don't replace it
    @Nullable
    @EventCalled
    public static Vec3 getSleepingPositionOffset(Entity entity, BlockState state) {
        if (!state.is(BlockTags.BEDS)) return null;
        Vec3 offset = Vec3.ZERO;
        //vanilla places player 2 pixels above bed. Player then falls down
        if (CommonConfigs.FIX_BED_POSITION.get()) {
            offset = offset.add(0, 9 / 16f - 0.6875, 0);
        }
        if (entity instanceof Player player) {
            PlayerSleepData data = STPlatStuff.getPlayerSleepData(player);
            if (data.usingDoubleBed()) {
                offset = BedEntity.getDoubleBedOffset(state.getValue(BedBlock.FACING), offset);
            }
        }
        return offset == Vec3.ZERO ? null : offset;
    }


    private static void onEncounter(ServerPlayer player, boolean mobSpawned, long wakeTime) {
        if (mobSpawned) {
            var c = STPlatStuff.getPlayerSleepData(player);
            c.setInsomniaCooldown(wakeTime, player.level().getGameTime(), CommonConfigs.ENCOUNTER_INSOMNIA_DURATION.get());
            c.setLasWokenUpTime(wakeTime);
            c.setConsecutiveNightsSlept(0);

            c.syncToClient(player);
        } else {
            player.displayClientMessage(Component.translatable("message.sleep_tight.encounter"), true);
        }
    }

    private static void onNightmare(ServerPlayer player, long wakeTime) {
        var c = STPlatStuff.getPlayerSleepData(player);
        c.setInsomniaCooldown(wakeTime, player.level().getGameTime(), CommonConfigs.NIGHTMARE_INSOMNIA_DURATION.get());
        c.setLasWokenUpTime(wakeTime);
        c.setConsecutiveNightsSlept(0);

        c.syncToClient(player);
        player.displayClientMessage(Component.translatable("message.sleep_tight.nightmare"), true);
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 20 * 3, 0, false, false, false,
                null
                // , Optional.of(new MobEffectInstance.FactorData(20, 10, 1, 1, 20 * 3, 1, true))
        ));
        NetworkHelper.sendToClientPlayer(player, new ClientBoundNightmarePacket());
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
            BedData data = STPlatStuff.getBedDataIfPresent(level, pos);
            if (data != null) {
                playerCap.increaseNightSleptInThisBed(data, player);
                //bed level is stored on the bed, without this it's never saved or sent to the client
                //and the bed looks like it doesn't level up
                syncBedDataToClients(level.getBlockEntity(getBedHead(state, pos)));
            }

            playerCap.increaseConsecutiveNightSleptCounter(wakeUpTime);
            playerCap.setLasWokenUpTime(wakeUpTime);

            if (bed.st_canSpawnBedbugs()) {
                BedbugSpawner.tryWakeUpSpawn(pos, player, data);
            }

            SleepEffectsHelper.applyEffectsOnWakeUp(playerCap, player, dayTimeDelta, pos, bed, state, data);

            playerCap.setInsomniaCooldown(wakeUpTime, player.level().getGameTime(), bed.st_getCooldown());
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

    //similar to what below but isnt time related
    @EventCalled
    public static boolean checkExtraSleepConditions(Player player, @Nullable BlockPos bedPos) {
        Level level = player.level();
        BedData bedData = STPlatStuff.getBedDataIfPresent(level, bedPos);
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
                BedData bedData = STPlatStuff.getBedDataIfPresent(newPlayer.level(), pos);
                if (bedData != null) {
                    BedEntity.layDown(state, pos, newPlayer);
                }
            }
        }
    }

    //all called by mixins

    public static boolean shouldCancelRespawnHere(Player player, DimensionTransition transition) {
        if (CommonConfigs.ONLY_RESPAWN_IN_HOME_BED.get()) {
            BedData bedData = STPlatStuff.getBedDataIfPresent(player.level(), BlockPos.containing(transition.pos()));
            if (bedData != null && !STPlatStuff.getPlayerSleepData(player).isBedLastSleptInto(bedData)) {
                return true;
            }
        }
        return false;
    }

    public static void onProjectileHitBed(Projectile projectile, Level level, BlockState state, BlockHitResult hit) {
        if (projectile instanceof ThrownPotion tp) {
            BlockPos myPos = hit.getBlockPos();
            BlockPos pos = getBedHead(state, myPos);
            BedData data = STPlatStuff.getBedDataIfPresent(level, pos);
            if (data != null && data.isInfested()) {
                var p = tp.getItem().get(DataComponents.POTION_CONTENTS);
                if (p != null && StreamSupport.stream(p.getAllEffects().spliterator(),false)
                        .anyMatch(e -> e.getEffect() == MobEffects.HARM)) {
                    level.playSound(null, pos, SoundEvents.SILVERFISH_DEATH, SoundSource.BLOCKS, 1, 1.3f);
                    if (level instanceof ServerLevel sl) {
                        NetworkHelper.sendToAllClientPlayersInRange(sl, pos, 32,
                                ClientBoundParticleMessage.bedbugInfest(pos,
                                        state.getValue(BedBlock.FACING).getOpposite()));
                    }
                    data.setBedBug(null);
                    syncBedDataToClients(level.getBlockEntity(pos));
                }
            }
        }
    }

    @EventCalled
    public static void spawnAfterBreakBed(BlockState state, ServerLevel level, BlockPos pos, @Nullable BlockEntity be) {
        if (level.isClientSide) return;
        BedData data = be == null ? STPlatStuff.getBedDataIfPresent(level, pos) : STPlatStuff.getBedDataIfPresent(be);
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

    public static void syncBedDataToClients(@Nullable BlockEntity be) {
        if (be == null) return;
        Level level = be.getLevel();
        if (level == null || level.isClientSide) return;
        SleepTight.BED_DATA.sync(be);
        be.setChanged();
    }

    public static void animateTickBed(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextFloat() < 0.3) {
            BedData data = STPlatStuff.getBedDataIfPresent(level, pos);
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

    //infests a random subset of beds that a structure piece (woodland mansion) just placed.
    //works on any bed block entity (vanilla or modded) since it matches BlockTags.BEDS, not a specific block.
    public static void infestStructureBeds(WorldGenLevel level, ChunkPos chunkPos, BoundingBox pieceBox, RandomSource random) {
        if (!CommonConfigs.BEDBUGS_ENABLED.get()) return;
        double chance = CommonConfigs.MANSION_INFESTATION_CHANCE.get();
        if (chance <= 0) return;

        //iterate only the positions that actually have a block entity in this chunk, rather than probing
        //every coordinate. copy the set since we read block states/entities while walking it.
        ChunkAccess chunk = level.getChunk(chunkPos.x, chunkPos.z);
        List<BlockEntity> toInfest = null;
        for (BlockPos p : new ArrayList<>(chunk.getBlockEntitiesPos())) {
            if (!pieceBox.isInside(p)) continue;
            BlockState state = level.getBlockState(p);
            if (!state.is(BlockTags.BEDS)) continue;
            //only the head holds the data; skips feet so a double bed is counted once
            if (!state.hasProperty(BedBlock.PART) || state.getValue(BedBlock.PART) != BedPart.HEAD) continue;
            if (random.nextFloat() >= chance) continue;

            BlockEntity be = level.getBlockEntity(p);
            if (be == null) continue;
            if (toInfest == null) toInfest = new ArrayList<>();
            toInfest.add(be);
        }
        if (toInfest == null) return;

        //defer mutating the data attachment to the main thread, since postProcess runs on a worldgen worker thread.

        MinecraftServer server = level.getLevel().getServer();
        if (server == null) return;
        List<BlockEntity> beds = toInfest;
        server.executeIfPossible(() -> {
            for (BlockEntity be : beds) {
                if (be.isRemoved()) continue;
                BedData data = SleepTight.BED_DATA.getOrCreate(be);
                if (data.isInfested()) continue;
                CompoundTag tag = new CompoundTag();
                tag.putString("id", SleepTight.BEDBUG_ENTITY.getId().toString());
                data.setBedBug(tag);
                syncBedDataToClients(be);
            }
        });
    }

    public static boolean shouldHaveBedData(BlockEntity blockEntity) {
        BlockState state = blockEntity.getBlockState();
        if (state.is(SleepTight.LAYING_BED_BLACKLIST)) return false;
        return (state.getBlock() instanceof BedBlock && state.getValue(BedBlock.PART) == BedPart.HEAD)
                || (blockEntity instanceof BedBlockEntity be && be.getBlockState().hasProperty(BedBlock.PART) &&
                be.getBlockState().getValue(BedBlock.PART) == BedPart.HEAD);
    }
}
