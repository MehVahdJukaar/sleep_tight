package net.mehvahdjukaar.sleep_tight.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.sleep_tight.common.tiles.IExtraBedDataProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.*;

//only data associated with a vanilla bed here
public final class BedData {

    public static final Codec<BedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.STRING_CODEC.fieldOf("id").forGetter(BedData::getId),
            UUIDUtil.STRING_CODEC.listOf().optionalFieldOf("home_bed_to")
                    .forGetter(d -> d.seenPlayers.isEmpty() ? Optional.empty() : Optional.of(new ArrayList<>(d.seenPlayers)))
    ).apply(instance, (uuid, uuids) -> new BedData(uuid, uuids.orElse(List.of()))));

    private final UUID id;
    private final Set<UUID> seenPlayers;//unused

    private BedData(UUID id, Collection<UUID> homeBedTo) {
        this.id = id;
        this.seenPlayers = new HashSet<>(homeBedTo);
    }

    public BedData() {
        this(UUID.randomUUID(), new HashSet<>());
    }

    public void onHomeBedSet(Player player) {
        //this.seenPlayers.add(player.getUUID());
    }

    @Nullable
    public static BedData get(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof IExtraBedDataProvider bed) {
            return bed.st_getBedData();
        } else {
            return null;
        }
    }

    public UUID getId() {
        return id;
    }

    @Override
    public String toString() {
        return "BedData[" +
                "id=" + id + ", " +
                "seenPlayers=" + seenPlayers + ']';
    }


}

