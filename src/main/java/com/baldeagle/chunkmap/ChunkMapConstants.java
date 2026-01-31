package com.baldeagle.chunkmap;

public final class ChunkMapConstants {

    // Default view radius in chunks (radius 8 => 17x17 chunks).
    public static final int DEFAULT_RADIUS = 8;

    // Default sampling grid per chunk (4 => 4x4 samples).
    public static final int DEFAULT_GRID = 4;

    // Hard limits to protect server performance.
    public static final int MAX_RADIUS = 32;
    public static final int MAX_GRID = 8;

    // Cache TTL for terrain snapshots (in ticks).
    public static final long SNAPSHOT_TTL_TICKS = 24000L;

    private ChunkMapConstants() {}
}
