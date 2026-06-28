package net.mehvahdjukaar.sleep_tight.common.entities;

import net.mehvahdjukaar.sleep_tight.SleepTight;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumSet;

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
            BlockGetter blockGetter = this.level;

            BlockState state = blockGetter.getBlockState(blockPos);
            if (state.is(SleepTight.BEDBUG_WALK_THROUGH)) return blockPos.getY();
            VoxelShape voxelShape = state.getCollisionShape(blockGetter, blockPos);
            return blockPos.getY() + (voxelShape.isEmpty() ? 0.0 : voxelShape.max(Direction.Axis.Y));
        }

        // same as super, with bedbug-specific path type tweaks applied per cell
        @Override
        public BlockPathTypes getBlockPathTypes(BlockGetter level, int x, int y, int z,
                                                EnumSet<BlockPathTypes> enumSet, BlockPathTypes type, BlockPos mobPos) {
            for (int i = 0; i < this.entityWidth; ++i) {
                for (int j = 0; j < this.entityHeight; ++j) {
                    for (int k = 0; k < this.entityDepth; ++k) {
                        int l = i + x;
                        int m = j + y;
                        int n = k + z;
                        BlockPathTypes pathType = this.getBlockPathType(level, l, m, n);
                        pathType = this.evaluateBlockPathType(level, mobPos, pathType);
                        pathType = modifyPathType(level, l, m, n, pathType);
                        if (i == 0 && j == 0 && k == 0) {
                            type = pathType;
                        }
                        enumSet.add(pathType);
                    }
                }
            }

            return type;
        }

        private static BlockPathTypes modifyPathType(BlockGetter blockGetter, int x, int y, int z, BlockPathTypes nodeType) {
            if (nodeType == BlockPathTypes.DOOR_OPEN || nodeType == BlockPathTypes.DOOR_WOOD_CLOSED ||
                    nodeType == BlockPathTypes.WALKABLE_DOOR) return BlockPathTypes.OPEN;
            if (nodeType == BlockPathTypes.BLOCKED && blockGetter.getBlockState(BlockPos.containing(x, y, z))
                    .getBlock() instanceof BedBlock) {
                return BlockPathTypes.WALKABLE;
            }
            return nodeType;
        }
    }
}
