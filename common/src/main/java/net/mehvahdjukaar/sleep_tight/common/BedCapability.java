package net.mehvahdjukaar.sleep_tight.common;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BedCapability {

    private final Map<UUID, Integer> timeSleptPerPlayer = new HashMap<>();
    @Nullable
    private UUID id = null;

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        ListTag listtag = new ListTag();
        if (!timeSleptPerPlayer.isEmpty()) {
            for (var e : timeSleptPerPlayer.entrySet()) {
                CompoundTag t = new CompoundTag();
                t.putInt("times", e.getValue());
                t.putUUID("player", e.getKey());
                listtag.add(t);
            }
            tag.put("sleep_counter", listtag);
        }
        if (id != null) {
            tag.putUUID("id", id);
        }
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        if (tag == null) return;
        if (tag.contains("data")) {
            ListTag listTag = tag.getList("data", ListTag.TAG_COMPOUND);
            this.timeSleptPerPlayer.clear();
            for (int i = 0; i < listTag.size(); ++i) {
                CompoundTag c = listTag.getCompound(i);
                this.timeSleptPerPlayer.put(c.getUUID("player"), c.getInt("times"));
            }
        }
        if (tag.contains("id")) {
            this.id = tag.getUUID("id");
        }
    }

    public UUID getId() {
        if (id == null) id = UUID.randomUUID();
        return id;
    }

    public void increaseTimeSleptOn(Player player) {
        this.timeSleptPerPlayer.merge(player.getUUID(), 1, Integer::sum);
    }

    public int getTimeSlept(Player player) {
        return this.timeSleptPerPlayer.getOrDefault(player.getUUID(), 0);
    }


    public boolean isEmpty() {
        return this.id == null && timeSleptPerPlayer.isEmpty();
    }
}
