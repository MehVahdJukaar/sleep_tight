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
    private final MobDebugInfo mobInfo;

    public ClientBoundPathDebugMessage(FriendlyByteBuf buf) {
        this.entityId = buf.readVarInt();
        this.path = DebugPath.read(buf);
        this.nodeHalfWidth = buf.readFloat();
        this.mobInfo = MobDebugInfo.read(buf);
    }

    public ClientBoundPathDebugMessage(int entityId, DebugPath path, float nodeHalfWidth, MobDebugInfo mobInfo) {
        this.entityId = entityId;
        this.path = path;
        this.nodeHalfWidth = nodeHalfWidth;
        this.mobInfo = mobInfo;
    }

    @Override
    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(this.entityId);
        this.path.write(buf);
        buf.writeFloat(this.nodeHalfWidth);
        this.mobInfo.write(buf);
    }

    @Override
    public void handle(Context context) {
        PathDebugRenderer.INSTANCE.addPath(this.entityId, this.path, this.nodeHalfWidth, this.mobInfo);
    }

    @Override
    public Type<?> type() {
        return TYPE.type();
    }
}
