package net.mehvahdjukaar.sleep_tight.test.debug;

import net.mehvahdjukaar.moonlight.api.platform.network.Message;
import net.mehvahdjukaar.sleep_tight.SleepTight;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;

public class ClientBoundPathDebugMessage implements Message {
    public static final TypeAndCodec<RegistryFriendlyByteBuf, ClientBoundPathDebugMessage> TYPE = Message.makeType(
            SleepTight.res("path_debug"),
            ClientBoundPathDebugMessage::new
    );

    private final int entityId;
    private final DebugPath path;
    private final float nodeHalfWidth;

    public ClientBoundPathDebugMessage(FriendlyByteBuf buf) {
        this.entityId = buf.readVarInt();
        this.path = DebugPath.read(buf);
        this.nodeHalfWidth = buf.readFloat();
    }

    public ClientBoundPathDebugMessage(int entityId, DebugPath path, float nodeHalfWidth) {
        this.entityId = entityId;
        this.path = path;
        this.nodeHalfWidth = nodeHalfWidth;
    }

    @Override
    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(this.entityId);
        this.path.write(buf);
        buf.writeFloat(this.nodeHalfWidth);
    }

    @Override
    public void handle(Context context) {
        PathDebugRenderer.INSTANCE.addPath(this.entityId, this.path, this.nodeHalfWidth);
    }

    @Override
    public Type<?> type() {
        return TYPE.type();
    }
}
