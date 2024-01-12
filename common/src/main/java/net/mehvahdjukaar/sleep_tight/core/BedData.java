package net.mehvahdjukaar.sleep_tight.core;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

//only data associated with a vanilla bed here
public class BedData {

    private final Set<UUID> homeBedTo = new HashSet<>();
    @Nullable
    private UUID id = null;

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        ListTag listtag = new ListTag();
        if (!homeBedTo.isEmpty()) {
            for (var e : homeBedTo) {
                var t = new CompoundTag();
                t.putUUID("player", e);
                listtag.add(t);
            }
            tag.put("owners", listtag);
        }
        if (id != null) {
            tag.putUUID("id", id);
        }
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        if (tag == null || tag.isEmpty()) return;
        if (tag.contains("owners")) {
            ListTag listTag = tag.getList("owners", ListTag.TAG_COMPOUND);
            this.homeBedTo.clear();
            for (int i = 0; i < listTag.size(); ++i) {
                CompoundTag c = listTag.getCompound(i);
                this.homeBedTo.add(c.getUUID("player"));
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

    public boolean isEmpty() {
        return this.id == null;
    }

    public void setHomeBedFor(Player player) {
        this.homeBedTo.add(player.getUUID());
    }

    public boolean isHomeBedFor(Player player) {
        return this.homeBedTo.contains(player.getUUID());
    }

}
