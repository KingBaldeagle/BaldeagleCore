package com.baldeagle.chunkmap;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.ChunkProviderServer;

public final class ChunkTerrainSampler {

    private ChunkTerrainSampler() {}

    public static ChunkTerrainSnapshot sample(
        World world,
        int chunkX,
        int chunkZ,
        int grid
    ) {
        if (world == null) {
            return null;
        }
        if (grid <= 0 || grid > ChunkMapConstants.MAX_GRID) {
            return null;
        }

        // Avoid generating new terrain just to render the map (server-side chunk provider).
        if (world.getChunkProvider() instanceof ChunkProviderServer) {
            ChunkProviderServer cps =
                (ChunkProviderServer) world.getChunkProvider();
            if (!cps.chunkExists(chunkX, chunkZ)) {
                return null;
            }
        }

        int samples = grid * grid;
        byte[] heights = new byte[samples];
        int[] stateIds = new int[samples];

        int cellSize = 16 / grid; // grid is clamped so this stays >= 2 for DEFAULT_GRID=4
        int centerOffset = cellSize / 2;

        int i = 0;
        for (int gx = 0; gx < grid; gx++) {
            for (int gz = 0; gz < grid; gz++) {
                int worldX = (chunkX << 4) + (gx * cellSize) + centerOffset;
                int worldZ = (chunkZ << 4) + (gz * cellSize) + centerOffset;

                BlockPos top = world.getTopSolidOrLiquidBlock(
                    new BlockPos(worldX, 0, worldZ)
                );
                int y = top != null ? top.getY() : 0;
                if (y < 0) y = 0;
                if (y > 255) y = 255;

                IBlockState state =
                    top != null
                        ? world.getBlockState(top)
                        : Blocks.AIR.getDefaultState();
                // Best-effort: ensure we're not reporting air as surface.
                if (state == null) {
                    state = Blocks.AIR.getDefaultState();
                }
                if (state.getBlock() == Blocks.AIR && y > 0) {
                    BlockPos down = top.down();
                    IBlockState downState = world.getBlockState(down);
                    if (downState != null) {
                        state = downState;
                        y = Math.max(0, down.getY());
                    }
                }

                heights[i] = (byte) (y & 0xFF);
                stateIds[i] = Block.getStateId(state);
                i++;
            }
        }

        return new ChunkTerrainSnapshot(
            chunkX,
            chunkZ,
            grid,
            heights,
            stateIds
        );
    }
}
