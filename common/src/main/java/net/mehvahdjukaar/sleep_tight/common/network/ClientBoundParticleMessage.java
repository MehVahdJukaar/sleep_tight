package net.mehvahdjukaar.sleep_tight.common.network;

import net.mehvahdjukaar.moonlight.api.platform.network.Message;
import net.mehvahdjukaar.moonlight.api.platform.network.NetworkHelper;
import net.mehvahdjukaar.sleep_tight.SleepTightClient;
import net.mehvahdjukaar.sleep_tight.common.entities.DreamerEssenceTargetEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;

public class ClientBoundParticleMessage implements Message {
    private final BlockPos pos;
    private final int data;

    public ClientBoundParticleMessage(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.data = buf.readInt();
    }

    private ClientBoundParticleMessage(BlockPos pos, int data) {
        this.pos = pos;
        this.data = data;
    }

    public static ClientBoundParticleMessage bedbugInfest(BlockPos pos, Direction direction) {
        return new ClientBoundParticleMessage(pos, direction.get2DDataValue());
    }

    public static ClientBoundParticleMessage bedbugDoor(BlockPos pos) {
        return new ClientBoundParticleMessage(pos, 5);
    }

    public static ClientBoundParticleMessage dreamEssence(BlockPos pos) {
        return new ClientBoundParticleMessage(pos, 4);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeInt(data);
    }

    @Override
    public void handle(NetworkHelper.Context context) {
        Level level = SleepTightClient.getPlayer().level();

        if (data < 4) {
            spawnParticleOnBed(pos, level);
            spawnParticleOnBed(pos.relative(Direction.from2DDataValue(data)), level);
        } else if (data == 4) {
            DreamerEssenceTargetEntity.spawnDeathParticles(level, pos);
        } else {
            level.addDestroyBlockEffect(pos, level.getBlockState(pos));
        }
    }

    private void spawnParticleOnBed(BlockPos pos, Level level) {
        for (int i = 0; i < 6 + level.random.nextInt(10); i++) {
            float x = pos.getX() + level.random.nextFloat();
            float z = pos.getZ() + level.random.nextFloat();
            float y = pos.getY() + 9 / 16f;
            level.addParticle(ParticleTypes.SMOKE, x, y, z, 0, 0, 0);
        }
    }
}
