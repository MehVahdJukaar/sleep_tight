package net.mehvahdjukaar.sleep_tight.integration.network;

import net.mehvahdjukaar.moonlight.api.platform.network.ChannelHandler;
import net.mehvahdjukaar.moonlight.api.platform.network.Message;
import net.mehvahdjukaar.moonlight.api.platform.network.NetworkDir;
import net.mehvahdjukaar.sleep_tight.common.HammockBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;

//bi directional
public class AccelerateHammockMessage implements Message {

    private final boolean leftPressed;
    private final BlockPos pos;

    public AccelerateHammockMessage(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.leftPressed = buf.readBoolean();
    }

    public AccelerateHammockMessage(BlockPos pos, boolean leftPressed) {
        this.leftPressed = leftPressed;
        this.pos = pos;
    }

    @Override
    public void writeToBuffer(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeBoolean(leftPressed);
    }

    @Override
    public void handle(ChannelHandler.Context context) {
        if (context.getDirection() == NetworkDir.PLAY_TO_CLIENT) {
            Level level = Minecraft.getInstance().cameraEntity.getLevel();
            if (level.getBlockEntity(pos) instanceof HammockBlockEntity tile) {
                if (leftPressed) {
                    tile.accelerateLeft();
                } else {
                    tile.accelerateRight();
                }
            }
        } else {
            var p = context.getSender();
            NetworkHandler.CHANNEL.sentToAllClientPlayersTrackingEntity(p,this);
        }
    }
}
