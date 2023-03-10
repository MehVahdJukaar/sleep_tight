package net.mehvahdjukaar.sleep_tight.common;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public enum HammockPart implements StringRepresentable {
    HALF_HEAD("half_head", -1, false),
    MIDDLE("middle", 0, false),
    HALF_FOOT("half_foot", 1, false),
    HEAD("head", 0, true),
    FOOT("foot", 1, true);

    private final String name;
    private final int masterOffset;
    private final boolean onFence;
    private final float pivotOffset;

    HammockPart(String name, int offset, boolean onRope) {
        this.name = name;
        this.masterOffset = offset;
        this.onFence = onRope;
        this.pivotOffset = onRope ? 5/16f : 3 / 16f; //rotation pivot y offset
    }

    public float getPivotOffset() {
        return pivotOffset;
    }

    @Override
    public String toString() {
        return this.name;
    }

    public String getSerializedName() {
        return this.name;
    }

    //goes from head to foot
    public boolean isFoot() {
        return this == FOOT || this == HALF_FOOT;
    }

    public HammockPart getToFootPart() {
        return switch (this) {
            case HEAD -> FOOT;
            case HALF_HEAD -> MIDDLE;
            case MIDDLE -> HALF_FOOT;
            default -> null;
        };
    }

    public HammockPart getToHeadPart() {
        return switch (this) {
            case FOOT -> HEAD;
            case MIDDLE -> HALF_HEAD;
            case HALF_FOOT -> MIDDLE;
            default -> null;
        };
    }

    @Nullable
    public Direction getConnectionDirection(Direction myDirection) {
        return switch (this) {
            case FOOT, HALF_FOOT -> myDirection.getOpposite();
            case HEAD, HALF_HEAD -> myDirection;
            default -> null;
        };
    }

    public List<Pair<Direction, HammockPart>> getPiecesDirections(Direction myDirection) {
        var list = new ArrayList<Pair<Direction, HammockPart>>();
        var p = getToHeadPart();
        if (p != null) list.add(Pair.of(myDirection, p));
        var p1 = getToFootPart();
        if (p1 != null) list.add(Pair.of(myDirection.getOpposite(), p1));
        return list;
    }

    public int getMasterOffset() {
        return masterOffset;
    }

    public boolean isMaster() {
        return this.masterOffset == 0;
    }

    public boolean isOnFence() {
        return onFence;
    }

    @Nullable
    public Direction getRopeAttachmentDirection(Direction myDir) {
        return switch (this) {
            case FOOT, HALF_FOOT -> myDir.getOpposite();
            case HEAD, HALF_HEAD -> myDir;
            case MIDDLE -> null;
        };
    }
}
