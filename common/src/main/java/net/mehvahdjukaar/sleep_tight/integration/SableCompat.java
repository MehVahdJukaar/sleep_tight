package net.mehvahdjukaar.sleep_tight.integration;

import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class SableCompat {

    public static Vec3 projectOutOfSubLevel(Level level, Vec3 pos) {
        return SableCompanion.INSTANCE.projectOutOfSubLevel(level, (Position) pos);
    }

    public static Vec3 rotateOutOfSubLevel(Level level, BlockPos pos, Vec3 offset) {
        SubLevelAccess subLevel = SableCompanion.INSTANCE.getContaining(level, pos);
        return subLevel == null ? offset : subLevel.logicalPose().transformNormal(offset);
    }

    public static <T extends Entity> List<T> getEntitiesIncludingSubLevels(Level level, Class<T> type, AABB box,
                                                                           Predicate<? super T> filter) {
        SableCompanion sable = SableCompanion.INSTANCE;
        SubLevelAccess originSubLevel = sable.getContaining(level, box.getCenter());
        List<T> found = new ArrayList<>(level.getEntitiesOfClass(type, box, filter));

        BoundingBox3d worldBox = new BoundingBox3d(box);
        if (originSubLevel != null) {
            worldBox.transform(originSubLevel.logicalPose());
            found.addAll(level.getEntitiesOfClass(type, new AABB(worldBox.minX, worldBox.minY, worldBox.minZ,
                    worldBox.maxX, worldBox.maxY, worldBox.maxZ), filter));
        }

        for (SubLevelAccess subLevel : sable.getAllIntersecting(level, worldBox)) {
            if (subLevel == originSubLevel) continue;
            BoundingBox3d subBox = new BoundingBox3d(worldBox);
            subBox.transformInverse(subLevel.logicalPose());
            found.addAll(level.getEntitiesOfClass(type, new AABB(subBox.minX, subBox.minY, subBox.minZ,
                    subBox.maxX, subBox.maxY, subBox.maxZ), filter));
        }
        return found;
    }
}
