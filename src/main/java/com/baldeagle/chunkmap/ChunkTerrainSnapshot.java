package com.baldeagle.chunkmap;

public final class ChunkTerrainSnapshot {

    public final int chunkX;
    public final int chunkZ;
    public final int grid;
    public final byte[] heights; // unsigned bytes [0..255]
    public final int[] stateIds; // Block.getStateId(IBlockState)

    public ChunkTerrainSnapshot(
        int chunkX,
        int chunkZ,
        int grid,
        byte[] heights,
        int[] stateIds
    ) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.grid = grid;
        this.heights = heights;
        this.stateIds = stateIds;
    }

    public int samples() {
        return grid * grid;
    }
}
