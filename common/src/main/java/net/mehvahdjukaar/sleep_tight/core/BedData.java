package net.mehvahdjukaar.sleep_tight.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.sleep_tight.SleepTight;
import net.mehvahdjukaar.sleep_tight.common.network.ClientBoundSyncBedCapMessage;
import net.mehvahdjukaar.sleep_tight.common.network.NetworkHandler;
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
            UUIDUtil.STRING_CODEC.listOf().optionalFieldOf("home_bed_to")
                    .forGetter(d -> d.seenPlayers.isEmpty() ? Optional.empty() : Optional.of(new ArrayList<>(d.seenPlayers))),
            CompoundTag.CODEC.optionalFieldOf("bed_bug").forGetter(d -> Optional.ofNullable(d.bedBug))
    ).apply(instance, BedData::new));

    protected UUID id;
    protected Set<UUID> seenPlayers;//unused
    @Nullable
    protected CompoundTag bedBug;

    private BedData(UUID id, Optional<List<UUID>> homeBedTo, Optional<CompoundTag> bedBug) {
        this.id = id;
        this.seenPlayers = new HashSet<>(homeBedTo.orElse(List.of()));
        this.bedBug = bedBug.orElse(null);
    }

    public BedData() {
        this(UUID.randomUUID(), Optional.empty(), Optional.empty());
    }

    public void onHomeBedActivated(Player player) {
        //this.seenPlayers.add(player.getUUID());
    }

    public UUID getId() {
        return id;
    }

    public boolean isInfested() {
        return bedBug != null;
    }

    public Set<UUID> getSeenPlayers() {
        return Collections.unmodifiableSet(seenPlayers);
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
                "seenPlayers=" + seenPlayers + ']';
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

