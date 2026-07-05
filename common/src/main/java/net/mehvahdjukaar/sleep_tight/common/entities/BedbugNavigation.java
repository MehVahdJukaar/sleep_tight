package net.mehvahdjukaar.sleep_tight.common.entities;

import net.mehvahdjukaar.sleep_tight.SleepTight;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfindingContext;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumSet;
import java.util.Set;

public class BedbugNavigation extends GroundPathNavigation {

    public BedbugNavigation(BedbugEntity bedbug, Level level) {
        super(bedbug, level);
    }

    @Override
    protected PathFinder createPathFinder(int maxVisitedNodes) {
        this.nodeEvaluator = new BedbugNodeEvaluator();
        this.nodeEvaluator.setCanPassDoors(true);
        return new PathFinder(this.nodeEvaluator, maxVisitedNodes);
    }



    public static class BedbugNodeEvaluator extends WalkNodeEvaluator {

        @Override
        protected double getFloorLevel(BlockPos pos) {
            BlockPos blockPos = pos.below();
            BlockGetter blockGetter = this.currentContext.level();

            BlockState state = blockGetter.getBlockState(blockPos);
            if (state.is(SleepTight.BEDBUG_WALK_THROUGH)) return blockPos.getY();
            VoxelShape voxelShape = state.getCollisionShape(blockGetter, blockPos);
            return blockPos.getY() + (voxelShape.isEmpty() ? 0.0 : voxelShape.max(Direction.Axis.Y));
        }

        // same as super, with bedbug-specific path type tweaks applied per cell
        @Override
        public Set<PathType> getPathTypeWithinMobBB(PathfindingContext context, int x, int y, int z) {
            EnumSet<PathType> enumSet = EnumSet.noneOf(PathType.class);

            for (int i = 0; i < this.entityWidth; ++i) {
                for (int j = 0; j < this.entityHeight; ++j) {
                    for (int k = 0; k < this.entityDepth; ++k) {
                        int l = i + x;
                        int m = j + y;
                        int n = k + z;
                        PathType pathType = this.getPathType(context, l, m, n);
                        pathType = modifyPathType(context, l, m, n, pathType);
                        BlockPos blockPos = this.mob.blockPosition();
                        boolean bl = this.canPassDoors();
                        if (pathType == PathType.DOOR_WOOD_CLOSED && this.canOpenDoors() && bl) {
                            pathType = PathType.WALKABLE_DOOR;
                        }

                        if (pathType == PathType.DOOR_OPEN && !bl) {
                            pathType = PathType.BLOCKED;
                        }

                        if (pathType == PathType.RAIL && this.getPathType(context, blockPos.getX(), blockPos.getY(), blockPos.getZ()) != PathType.RAIL
                                && this.getPathType(context, blockPos.getX(), blockPos.getY() - 1, blockPos.getZ()) != PathType.RAIL) {
                            pathType = PathType.UNPASSABLE_RAIL;
                        }

                        enumSet.add(pathType);
                    }
                }
            }

            return enumSet;
        }

        private static PathType modifyPathType(PathfindingContext blockGetter, int x, int y, int z, PathType nodeType) {
            if (nodeType == PathType.DOOR_OPEN || nodeType == PathType.DOOR_WOOD_CLOSED ||
                    nodeType == PathType.WALKABLE_DOOR || nodeType == BlockPathTypes.TRAPDOOR)
                return PathType.OPEN;
            if (nodeType == PathType.BLOCKED && blockGetter.getBlockState(BlockPos.containing(x, y, z))
                    .getBlock() instanceof BedBlock) {
                return PathType.WALKABLE;
            }
            return nodeType;
        }
    }
}
