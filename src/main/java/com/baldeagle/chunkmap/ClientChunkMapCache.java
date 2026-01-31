package com.baldeagle.chunkmap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client-side cache for received terrain snapshots.
 *
 * Pure data holder (no net.minecraft.client references) so it is safe to load on
 * either side even though it is only used on the client.
 */
public final class ClientChunkMapCache {

    // dimension -> (chunkKey -> snapshot)
    private static final Map<Integer, Map<Long, ChunkTerrainSnapshot>> CACHE =
        new HashMap<>();

    private ClientChunkMapCache() {}

    public static void putAll(
        int dimension,
        int grid,
        List<ChunkTerrainSnapshot> snapshots
    ) {
        if (snapshots == null || snapshots.isEmpty()) {
            return;
        }
        Map<Long, ChunkTerrainSnapshot> dim = CACHE.computeIfAbsent(
            dimension,
            k -> new HashMap<>()
        );
        for (ChunkTerrainSnapshot s : snapshots) {
            if (s == null || s.grid != grid) {
                continue;
            }
            long key = (((long) s.chunkX) << 32) | (s.chunkZ & 0xffffffffL);
            dim.put(key, s);
        }
    }

    public static ChunkTerrainSnapshot get(int dimension, int chunkX, int chunkZ) {
        Map<Long, ChunkTerrainSnapshot> dim = CACHE.get(dimension);
        if (dim == null) {
            return null;
        }
        long key = (((long) chunkX) << 32) | (chunkZ & 0xffffffffL);
        return dim.get(key);
    }

    public static void clear(int dimension) {
        CACHE.remove(dimension);
    }
}
