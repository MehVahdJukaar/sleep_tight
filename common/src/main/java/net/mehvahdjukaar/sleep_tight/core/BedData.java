package net.mehvahdjukaar.sleep_tight.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.sleep_tight.common.tiles.IExtraBedDataProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

//only data associated with a vanilla bed here
public final class BedData {

    public static final Codec<BedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.STRING_CODEC.fieldOf("id").forGetter(BedData::getId),
            UUIDUtil.STRING_CODEC.listOf().fieldOf("home_bed_to").forGetter(d -> d.homeBedTo.stream().toList())
    ).apply(instance, BedData::new));

    private final UUID id;
    private final Set<UUID> homeBedTo;

    private BedData(UUID id, Collection<UUID> homeBedTo) {
        this.id = id;
        this.homeBedTo = new HashSet<>(homeBedTo);
    }

    public BedData() {
        this(UUID.randomUUID(), new HashSet<>());
    }

    public void setHomeBedFor(Player player) {
        this.homeBedTo.add(player.getUUID());
    }

    public boolean isHomeBedFor(Player player) {
        return this.homeBedTo.contains(player.getUUID());
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
                "homeBedTo=" + homeBedTo + ']';
    }


}

