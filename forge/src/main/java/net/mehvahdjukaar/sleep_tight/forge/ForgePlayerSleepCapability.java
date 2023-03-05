package net.mehvahdjukaar.sleep_tight.forge;

import net.mehvahdjukaar.sleep_tight.core.BedData;
import net.mehvahdjukaar.sleep_tight.core.PlayerSleepData;
import net.mehvahdjukaar.sleep_tight.common.IExtraBedDataProvider;
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

//actual capability provider (which provides itself as a cap instance)
public class ForgePlayerSleepCapability extends PlayerSleepData implements ICapabilitySerializable<CompoundTag> {

    public static final Capability<ForgePlayerSleepCapability> TOKEN = CapabilityManager.get(new CapabilityToken<>() {
    });

    @Nonnull
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, Direction facing) {
        return capability == TOKEN ?
                LazyOptional.of(() -> this).cast() : LazyOptional.empty();
    }

    @Nullable
    public static BedData getHomeBedIfHere(Player player, BlockPos pos) {
        ForgePlayerSleepCapability c = player.getCapability(ForgePlayerSleepCapability.TOKEN).orElse(null);
        if (c != null && player.level.getBlockEntity(pos) instanceof IExtraBedDataProvider bed) {
            BedData bedCap = bed.getBedData();
            if (bedCap.getId().equals(c.getHomeBed()) && bedCap.isHomeBedFor(player)) {
                return bedCap;
            }
        }
        return null;
    }


}

