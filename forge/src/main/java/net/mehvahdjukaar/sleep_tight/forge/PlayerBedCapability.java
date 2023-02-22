package net.mehvahdjukaar.sleep_tight.forge;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BedBlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

//actual capability provider (which provides itself as a cap instance)
public class PlayerBedCapability implements ICapabilitySerializable<CompoundTag> {

    public static final Capability<PlayerBedCapability> TOKEN = CapabilityManager.get(new CapabilityToken<>() {
    });

    @Nullable
    private UUID bedID = null;
    private long lastNightmareTimestamp = -1;

    @Nonnull
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, Direction facing) {
        return capability == TOKEN ?
                LazyOptional.of(() -> this).cast() : LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        CompoundTag t = new CompoundTag();
        if(bedID != null) t.putUUID("bed_id", bedID);
        t.putLong("last_nightmare_time", lastNightmareTimestamp);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        if(tag.contains("bed_id")) this.bedID = tag.getUUID("bed_id");
        this.lastNightmareTimestamp = tag.getLong("last_nightmare_time");
    }

    public long getTimeSinceNightmare(Player player) {
        return player.level.getGameTime() - lastNightmareTimestamp;
    }

    //call on boh sides. wont sync. we'll blindly trust the client here
    public void addNightmare(Player player) {
        this.lastNightmareTimestamp = player.level.getGameTime();
    }

    public void assignHomeBed(Player player, UUID bedId) {
        this.bedID = bedId;
    }

    @Nullable
    public UUID getHomeBed() {
        return bedID;
    }


}

