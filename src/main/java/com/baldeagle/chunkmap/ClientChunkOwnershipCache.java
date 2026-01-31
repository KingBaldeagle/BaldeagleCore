package com.baldeagle.chunkmap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ClientChunkOwnershipCache {

    // dimension -> (chunkKey -> info)
    private static final Map<Integer, Map<Long, ChunkOwnershipInfo>> CACHE =
        new HashMap<>();

    private ClientChunkOwnershipCache() {}

    public static void putAll(int dimension, List<ChunkOwnershipInfo> infos) {
        if (infos == null || infos.isEmpty()) {
            return;
        }
        Map<Long, ChunkOwnershipInfo> dim = CACHE.computeIfAbsent(
            dimension,
            k -> new HashMap<>()
        );
        for (ChunkOwnershipInfo info : infos) {
            if (info == null) {
                continue;
            }
            long key = (((long) info.chunkX) << 32) | (info.chunkZ & 0xffffffffL);
            dim.put(key, info);
        }
    }

    public static ChunkOwnershipInfo get(int dimension, int chunkX, int chunkZ) {
        Map<Long, ChunkOwnershipInfo> dim = CACHE.get(dimension);
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
