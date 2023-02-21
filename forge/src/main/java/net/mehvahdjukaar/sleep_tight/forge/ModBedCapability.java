package net.mehvahdjukaar.sleep_tight.forge;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

//actual capability provider (which provides itself as a cap instance)
public class ModBedCapability implements ICapabilitySerializable<CompoundTag> {

    public static final Capability<ModBedCapability> TOKEN = CapabilityManager.get(new CapabilityToken<>() {
    });

    private final Map<UUID, Integer> timeSleptPerPlayer = new HashMap<>();
    private UUID bedId = UUID.randomUUID();

    @Nonnull
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, Direction facing) {
        return capability == TOKEN ?
                LazyOptional.of(() -> this).cast() : LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        ListTag listtag = new ListTag();
        for (var e : timeSleptPerPlayer.entrySet()) {
            CompoundTag t = new CompoundTag();
            t.putInt("times", e.getValue());
            t.putUUID("player", e.getKey());
            listtag.add(t);
        }
        tag.put("sleep_counter", listtag);
        tag.putUUID("id", bedId);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        ListTag listTag = tag.getList("data", ListTag.TAG_COMPOUND);
        this.timeSleptPerPlayer.clear();
        for (int i = 0; i < listTag.size(); ++i) {
            CompoundTag c = listTag.getCompound(i);
            this.timeSleptPerPlayer.put(c.getUUID("player"), c.getInt("times"));
        }
        this.bedId = tag.getUUID("id");
    }

    public UUID getBedId() {
        return bedId;
    }

    public void increaseTimeSlept(Player player) {
        this.timeSleptPerPlayer.merge(player.getUUID(), 1, Integer::sum);
    }

    public int getTimeSlept(Player player) {
        return this.timeSleptPerPlayer.getOrDefault(player.getUUID(), 0);
    }
}

