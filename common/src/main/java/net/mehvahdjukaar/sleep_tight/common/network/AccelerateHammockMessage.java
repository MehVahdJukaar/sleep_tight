package net.mehvahdjukaar.sleep_tight.common.network;

import net.mehvahdjukaar.moonlight.api.platform.network.Message;
import net.mehvahdjukaar.moonlight.api.platform.network.NetworkHelper;
import net.mehvahdjukaar.sleep_tight.SleepTight;
import net.mehvahdjukaar.sleep_tight.common.tiles.HammockTile;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.Level;

//bi directional
public class AccelerateHammockMessage implements Message {

    public static final TypeAndCodec<RegistryFriendlyByteBuf, AccelerateHammockMessage> TYPE = Message.makeType(
            SleepTight.res("accelerate_hammock"),
            AccelerateHammockMessage::new
    );

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
    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeBoolean(leftPressed);
    }


    @Override
    public void handle(Context context) {
        if (context.getDirection() == NetworkDir.CLIENT_BOUND) {
            Level level = Minecraft.getInstance().cameraEntity.level();
            if (level.getBlockEntity(pos) instanceof HammockTile tile) {
                if (leftPressed) {
                    tile.accelerateLeft();
                } else {
                    tile.accelerateRight();
                }
            }
        } else {
            var p = context.getPlayer();
            NetworkHelper.sendToAllClientPlayersTrackingEntity(p, this);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE.type();
    }
}
