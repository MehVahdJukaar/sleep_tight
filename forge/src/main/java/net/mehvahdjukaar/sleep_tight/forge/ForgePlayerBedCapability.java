package net.mehvahdjukaar.sleep_tight.forge;

import net.mehvahdjukaar.sleep_tight.common.BedCapability;
import net.mehvahdjukaar.sleep_tight.common.PlayerBedCapability;
import net.mehvahdjukaar.sleep_tight.common.ISleepTightBed;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

//actual capability provider (which provides itself as a cap instance)
public class ForgePlayerBedCapability extends PlayerBedCapability implements ICapabilitySerializable<CompoundTag> {

    public static final Capability<ForgePlayerBedCapability> TOKEN = CapabilityManager.get(new CapabilityToken<>() {
    });

    @Nonnull
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, Direction facing) {
        return capability == TOKEN ?
                LazyOptional.of(() -> this).cast() : LazyOptional.empty();
    }

    @Nullable
    public static BedCapability getHomeBedIfHere(Player player, BlockPos pos) {
        ForgePlayerBedCapability c = player.getCapability(ForgePlayerBedCapability.TOKEN).orElse(null);
        if (c != null && player.level.getBlockEntity(pos) instanceof ISleepTightBed bed) {
            BedCapability bedCap = bed.getBedCap();
            if (bedCap.getId().equals(c.getHomeBed())) {
                return bedCap;
            }
        }
        return null;
    }


}

