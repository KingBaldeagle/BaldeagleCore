package com.baldeagle.chunkmap;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.World;

public final class ChunkTerrainSnapshotCache {

    private static final class Cached {

        final ChunkTerrainSnapshot snapshot;
        final long sampledAt;

        Cached(ChunkTerrainSnapshot snapshot, long sampledAt) {
            this.snapshot = snapshot;
            this.sampledAt = sampledAt;
        }
    }

    // dimension -> (chunkKey -> cached snapshot)
    private static final Map<Integer, Map<Long, Cached>> CACHE = new HashMap<>();

    private ChunkTerrainSnapshotCache() {}

    public static ChunkTerrainSnapshot getOrSample(
        World world,
        int chunkX,
        int chunkZ,
        int grid
    ) {
        if (world == null) {
            return null;
        }

        int dim = world.provider.getDimension();
        Map<Long, Cached> dimCache = CACHE.computeIfAbsent(
            dim,
            k -> new HashMap<>()
        );
        long key = (((long) chunkX) << 32) | (chunkZ & 0xffffffffL);

        Cached cached = dimCache.get(key);
        long now = world.getTotalWorldTime();
        if (
            cached != null &&
            cached.snapshot != null &&
            cached.snapshot.grid == grid &&
            (now - cached.sampledAt) < ChunkMapConstants.SNAPSHOT_TTL_TICKS
        ) {
            return cached.snapshot;
        }

        ChunkTerrainSnapshot fresh = ChunkTerrainSampler.sample(
            world,
            chunkX,
            chunkZ,
            grid
        );
        if (fresh == null) {
            return null;
        }
        dimCache.put(key, new Cached(fresh, now));
        return fresh;
    }
}
