package dev.retomc.torchguide;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

final class TorchGuideTracker {
    private Level level;
    private BlockPos origin;
    private Placement pendingPlacement;
    private Placement undoPlacement;
    private BlockPos previousOrigin;
    private List<BlockPos> guides = List.of();
    private boolean enabled;
    private long ticks;

    boolean enabled() {
        return enabled;
    }

    boolean toggle() {
        enabled = !enabled;
        return enabled;
    }

    List<BlockPos> guides() {
        return guides;
    }

    void setLevel(Level level) {
        if (this.level == level) return;
        this.level = level;
        origin = null;
        pendingPlacement = null;
        undoPlacement = null;
        previousOrigin = null;
        guides = List.of();
        ticks = 0;
    }

    void queuePlacement(BlockPos pos, int undoWindowTicks) {
        if (enabled && !level.getBlockState(pos).is(Blocks.TORCH)) {
            pendingPlacement = new Placement(pos.immutable(), ticks, undoWindowTicks);
        }
    }

    void tick(BlockPos playerPos, boolean holdsTorch, int gap, int range) {
        ticks++;
        boolean originChanged = false;
        if (undoPlacement != null) {
            if (ticks - undoPlacement.createdAtTick() > undoPlacement.undoWindowTicks()) {
                undoPlacement = null;
                previousOrigin = null;
            } else if (level.hasChunkAt(undoPlacement.pos())
                && !level.getBlockState(undoPlacement.pos()).is(Blocks.TORCH)) {
                origin = previousOrigin;
                originChanged = true;
                undoPlacement = null;
                previousOrigin = null;
            }
        }
        if (pendingPlacement != null) {
            if (enabled && level.getBlockState(pendingPlacement.pos()).is(Blocks.TORCH)) {
                previousOrigin = origin;
                origin = pendingPlacement.pos();
                undoPlacement = pendingPlacement;
                originChanged = true;
            }
            pendingPlacement = null;
        }

        if (!enabled || origin == null || !holdsTorch) {
            guides = List.of();
        } else if (originChanged || ticks % 5 == 0) {
            guides = findGuides(playerPos, gap + 1, range);
        }
    }

    private record Placement(BlockPos pos, long createdAtTick, int undoWindowTicks) {}

    private List<BlockPos> findGuides(BlockPos playerPos, int spacing, int range) {
        int minX = playerPos.getX() - range;
        int minZ = playerPos.getZ() - range;
        int firstX = minX + Math.floorMod(origin.getX() - minX, spacing);
        int firstZ = minZ + Math.floorMod(origin.getZ() - minZ, spacing);
        int topY = Math.min(playerPos.getY() + 4, level.getMaxBuildHeight() - 1);
        int bottomY = Math.max(playerPos.getY() - 8, level.getMinBuildHeight());
        BlockState torch = Blocks.TORCH.defaultBlockState();
        BlockPos.MutableBlockPos candidate = new BlockPos.MutableBlockPos();
        List<BlockPos> positions = new ArrayList<>();

        for (int x = firstX; x <= playerPos.getX() + range; x += spacing) {
            for (int z = firstZ; z <= playerPos.getZ() + range; z += spacing) {
                int dx = x - playerPos.getX();
                int dz = z - playerPos.getZ();
                if (dx * dx + dz * dz > range * range) continue;
                candidate.set(x, playerPos.getY(), z);
                if (!level.getChunkSource().hasChunk(x >> 4, z >> 4)) continue;

                for (int y = topY; y >= bottomY; y--) {
                    candidate.set(x, y, z);
                    if (level.getBlockState(candidate).canBeReplaced()
                        && level.getFluidState(candidate).isEmpty()
                        && torch.canSurvive(level, candidate)) {
                        positions.add(candidate.immutable());
                        break;
                    }
                }
            }
        }
        return positions;
    }
}
