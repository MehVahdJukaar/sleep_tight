package net.mehvahdjukaar.sleep_tight.common;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.UUID;

public abstract class PlayerBedCapability {

    @Nullable
    private UUID bedID = null;
    private long lastNightmareTimestamp = -1;

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        if(bedID != null) tag.putUUID("bed_id", bedID);
        tag.putLong("last_nightmare_time", lastNightmareTimestamp);
        return tag;
    }

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

    public void assignHomeBed(UUID bedId) {
        this.bedID = bedId;
    }

    @Nullable
    public UUID getHomeBed() {
        return bedID;
    }

    public long getLastNightmareTimestamp() {
        return lastNightmareTimestamp;
    }

    public void acceptFromServer(UUID id, long time) {
        this.bedID = id;
        this.lastNightmareTimestamp = time;
    }
}
