package net.mehvahdjukaar.sleep_tight.network;

import net.mehvahdjukaar.moonlight.api.platform.network.ChannelHandler;
import net.mehvahdjukaar.moonlight.api.platform.network.Message;
import net.mehvahdjukaar.sleep_tight.SleepTightClient;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public class ClientBoundParticlePacket implements Message {
    private final BlockPos first;
    private final BlockPos second;

    public ClientBoundParticlePacket(FriendlyByteBuf buf) {
        this.first = buf.readBlockPos();
        this.second = buf.readBlockPos();
    }

    public ClientBoundParticlePacket(BlockPos first, BlockPos second) {
        this.first = first;
        this.second = second;
    }

    @Override
    public void writeToBuffer(FriendlyByteBuf buf) {
        buf.writeBlockPos(first);
        buf.writeBlockPos(second);
    }

    @Override
    public void handle(ChannelHandler.Context context) {
        Level level = SleepTightClient.getPlayer().level;
        spawnParticleOn(first, level);
        spawnParticleOn(second, level);
    }

    private void spawnParticleOn(BlockPos pos, Level level) {
        for (int i = 0; i < 6 + level.random.nextInt(10); i++) {
            float x = pos.getX() + level.random.nextFloat();
            float z = pos.getZ() + level.random.nextFloat();
            float y = pos.getY() + 9 / 16f;
            level.addParticle(ParticleTypes.SMOKE, x, y, z, 0, 0, 0);
        }
    }
}
