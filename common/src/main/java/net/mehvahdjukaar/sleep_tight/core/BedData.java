package net.mehvahdjukaar.sleep_tight.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.sleep_tight.SleepTight;
import net.mehvahdjukaar.sleep_tight.common.network.ClientBoundSyncBedCapMessage;
import net.mehvahdjukaar.sleep_tight.common.network.NetworkHandler;
import net.mehvahdjukaar.sleep_tight.configs.CommonConfigs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.*;

//only data associated with a vanilla bed here
public class BedData {

    public static final ResourceLocation ID = SleepTight.res("bed_data");

    public static final Codec<BedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.STRING_CODEC.fieldOf("id").forGetter(BedData::getId),
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.BYTE).optionalFieldOf("bed_level")
                    .forGetter(d -> d.bedLevel.isEmpty() ? Optional.empty() : Optional.of(d.bedLevel)),
            CompoundTag.CODEC.optionalFieldOf("bed_bug").forGetter(d -> Optional.ofNullable(d.bedBug))
    ).apply(instance, BedData::new));

    protected UUID id;
    protected Map<UUID, Byte> bedLevel;
    @Nullable
    protected CompoundTag bedBug;

    private BedData(UUID id, Optional<Map<UUID, Byte>> homeBedTo, Optional<CompoundTag> bedBug) {
        this.id = id;
        this.bedLevel = new HashMap<>(homeBedTo.orElse(Collections.emptyMap()));
        this.bedBug = bedBug.orElse(null);
    }

    public BedData() {
        this(UUID.randomUUID(), Optional.empty(), Optional.empty());
    }

    public void incrementBedLevel(Player player) {
        UUID playerId = player.getUUID();
        byte value = (byte) Math.min(CommonConfigs.HOME_BED_MAX_LEVEL.get(),
                this.bedLevel.getOrDefault(playerId, (byte) 0) + 1);
        this.bedLevel.put(playerId, value);
    }

    public byte getBedLevel(Player player) {
        return this.bedLevel.getOrDefault(player.getUUID(), (byte) 0);
    }

    public Map<UUID, Byte> getBedLevels() {
        return Collections.unmodifiableMap(bedLevel);
    }

    public UUID getId() {
        return id;
    }

    public boolean isInfested() {
        return bedBug != null;
    }

    public void setBedBug(@Nullable CompoundTag entityTag) {
        this.bedBug = entityTag;
    }

    public CompoundTag getBedBug() {
        return bedBug;
    }

    public void syncToClient(ServerPlayer player, BlockPos pos) {
        NetworkHandler.CHANNEL.sendToClientPlayer(player, new ClientBoundSyncBedCapMessage(pos, this));
    }

    @Override
    public String toString() {
        return "BedData[" +
                "id=" + id + ", " +
                "bedBug=" + (bedBug != null ? "present" : "null") + ", " +
                "bedLevel=" + bedLevel +
                ']';
    }

    public void acceptFromServer(UUID id, boolean hasBedBug) {
        this.id = id;
        if (hasBedBug) {
            if (this.bedBug == null) {
                this.bedBug = new CompoundTag(); //create empty tag
            }
        } else {
            this.bedBug = null; //remove bedbug
        }
    }
}

