package net.mehvahdjukaar.sleep_tight.network;

import net.mehvahdjukaar.moonlight.api.platform.network.ChannelHandler;
import net.mehvahdjukaar.moonlight.api.platform.network.Message;
import net.mehvahdjukaar.sleep_tight.SleepTightClient;
import net.mehvahdjukaar.sleep_tight.common.DreamerEssenceTargetEntity;
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

    public static ClientBoundParticleMessage bedbug(BlockPos pos, Direction direction){
       return new ClientBoundParticleMessage(pos, direction.get2DDataValue());
    }

    public static ClientBoundParticleMessage dreamEssence(BlockPos pos){
        return new ClientBoundParticleMessage(pos, 4);
    }

        @Override
    public void writeToBuffer(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeInt(data);
    }

    @Override
    public void handle(ChannelHandler.Context context) {
        Level level = SleepTightClient.getPlayer().level;

        if(data != 4) {
            spawnParticleOn(pos, level);
            spawnParticleOn(pos.relative(Direction.from2DDataValue(data)), level);
        }else{
            DreamerEssenceTargetEntity.spawnDeathParticles(level, pos);
        }
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
