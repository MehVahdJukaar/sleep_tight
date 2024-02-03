package net.mehvahdjukaar.sleep_tight.common.network;

import net.mehvahdjukaar.moonlight.api.platform.network.Message;
import net.mehvahdjukaar.moonlight.api.platform.network.NetworkDir;
import net.mehvahdjukaar.moonlight.api.platform.network.NetworkHelper;
import net.mehvahdjukaar.sleep_tight.common.tiles.HammockTile;
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
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeBoolean(leftPressed);
    }

    @Override
    public void handle(NetworkHelper.Context context) {
        if (context.getDirection() == NetworkDir.CLIENTBOUND) {
            Level level = Minecraft.getInstance().cameraEntity.level();
            if (level.getBlockEntity(pos) instanceof HammockTile tile) {
                if (leftPressed) {
                    tile.accelerateLeft();
                } else {
                    tile.accelerateRight();
                }
            }
        } else {
            var p = context.getSender();
            NetworkHelper.sentToAllClientPlayersTrackingEntity(p,this);
        }
    }
}
