package net.mehvahdjukaar.sleep_tight.forge;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BedBlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

//actual capability provider (which provides itself as a cap instance)
public class ModBedCapability implements ICapabilitySerializable<CompoundTag> {

    public static final Capability<ModBedCapability> TOKEN = CapabilityManager.get(new CapabilityToken<>() {
    });

    private final Map<UUID, Integer> timeSleptPerPlayer = new HashMap<>();
    @Nullable
    private UUID id = null;


    @Nonnull
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, Direction facing) {
        return capability == TOKEN ?
                LazyOptional.of(() -> this).cast() : LazyOptional.empty();
    }

    @Override
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

    @Override
    public void deserializeNBT(CompoundTag tag) {
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

    public void increaseTimeSlept(Player player) {
        this.timeSleptPerPlayer.merge(player.getUUID(), 1, Integer::sum);
    }

    public int getTimeSlept(Player player) {
        return this.timeSleptPerPlayer.getOrDefault(player.getUUID(), 0);
    }

    @Nullable
    public static ModBedCapability getHomeBedIfHere(Player player, BlockPos pos) {
        PlayerBedCapability c = player.getCapability(PlayerBedCapability.TOKEN).orElse(null);
        if (c != null && player.level.getBlockEntity(pos) instanceof BedBlockEntity bed) {
            ModBedCapability bedCap = bed.getCapability(ModBedCapability.TOKEN).orElse(null);
            if (bedCap != null && bedCap.getId().equals(c.getHomeBed())) {
                return bedCap;
            }
        }
        return null;
    }
}

