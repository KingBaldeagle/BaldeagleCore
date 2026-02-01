package com.baldeagle.territory;

import com.baldeagle.bank.ModBlocks;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.block.state.IBlockState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

public final class TerritoryManager {

    private TerritoryManager() {}

    public static final class DimChunkKey {

        public final int dimension;
        public final int chunkX;
        public final int chunkZ;

        public DimChunkKey(int dimension, int chunkX, int chunkZ) {
            this.dimension = dimension;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof DimChunkKey)) {
                return false;
            }
            DimChunkKey other = (DimChunkKey) obj;
            return (
                dimension == other.dimension &&
                chunkX == other.chunkX &&
                chunkZ == other.chunkZ
            );
        }

        @Override
        public int hashCode() {
            int result = dimension;
            result = 31 * result + chunkX;
            result = 31 * result + chunkZ;
            return result;
        }
    }

    public static DimChunkKey chunkKey(int dimension, int chunkX, int chunkZ) {
        return new DimChunkKey(dimension, chunkX, chunkZ);
    }

    public static DimChunkKey chunkKey(World world, ChunkPos chunk) {
        int dim = world != null ? world.provider.getDimension() : 0;
        return chunkKey(dim, chunk.x, chunk.z);
    }

    public static DimChunkKey chunkKey(World world, int chunkX, int chunkZ) {
        int dim = world != null ? world.provider.getDimension() : 0;
        return chunkKey(dim, chunkX, chunkZ);
    }

    public static TerritoryData.ClaimEntry getClaim(
        World world,
        ChunkPos chunk
    ) {
        if (world == null || chunk == null) {
            return null;
        }

        TerritoryData data = TerritoryData.get(world);
        DimChunkKey key = chunkKey(world, chunk);
        TerritoryData.ClaimEntry claim = data.getClaims().get(key);
        if (claim == null) {
            return null;
        }

        // A chunk is only considered claimed if its registered flag still exists in the chunk.
        if (!isValidFlag(world, chunk, claim.flagPos)) {
            data.getClaims().remove(key);
            data.markDirty();
            return null;
        }

        return claim;
    }

    public static boolean isChunkClaimed(World world, ChunkPos chunk) {
        return getClaim(world, chunk) != null;
    }

    public static UUID getOwningCountryId(World world, ChunkPos chunk) {
        TerritoryData.ClaimEntry claim = getClaim(world, chunk);
        return claim != null ? claim.countryId : null;
    }

    public static boolean claimChunk(
        World world,
        ChunkPos chunk,
        UUID countryId,
        BlockPos flagPos
    ) {
        if (
            world == null ||
            chunk == null ||
            countryId == null ||
            flagPos == null
        ) {
            return false;
        }
        if (world.isRemote) {
            return false;
        }

        TerritoryData data = TerritoryData.get(world);
        DimChunkKey key = chunkKey(world, chunk);
        if (data.getClaims().containsKey(key)) {
            // Existing claim (validity checked on access).
            return false;
        }

        if (chunkHasOtherFlag(world, chunk, flagPos)) {
            // Physical enforcement: one claim flag per chunk.
            return false;
        }

        if (!isValidFlag(world, chunk, flagPos)) {
            return false;
        }

        data
            .getClaims()
            .put(key, new TerritoryData.ClaimEntry(countryId, flagPos));
        data.markDirty();
        return true;
    }

    public static void unclaimChunk(World world, ChunkPos chunk) {
        if (world == null || chunk == null || world.isRemote) {
            return;
        }
        TerritoryData data = TerritoryData.get(world);
        if (data.getClaims().remove(chunkKey(world, chunk)) != null) {
            data.markDirty();
        }
    }

    public static void unclaimChunkIfFlagMatches(
        World world,
        BlockPos flagPos
    ) {
        if (world == null || flagPos == null || world.isRemote) {
            return;
        }

        ChunkPos chunk = new ChunkPos(flagPos);
        TerritoryData data = TerritoryData.get(world);
        DimChunkKey key = chunkKey(world, chunk);
        TerritoryData.ClaimEntry claim = data.getClaims().get(key);
        if (claim == null) {
            return;
        }
        if (!flagPos.equals(claim.flagPos)) {
            return;
        }
        data.getClaims().remove(key);
        data.markDirty();
    }

    public static Map<UUID, Integer> getClaimCounts(MinecraftServer server) {
        Map<UUID, Integer> counts = new HashMap<>();
        if (server == null) {
            return counts;
        }

        for (World world : server.worlds) {
            if (world == null || world.isRemote) {
                continue;
            }

            int dim = world.provider.getDimension();
            TerritoryData data = TerritoryData.get(world);
            for (Map.Entry<DimChunkKey, TerritoryData.ClaimEntry> entry : data
                .getClaims()
                .entrySet()) {
                if (entry.getKey().dimension != dim) {
                    continue;
                }
                TerritoryData.ClaimEntry claim = entry.getValue();
                counts.put(
                    claim.countryId,
                    counts.getOrDefault(claim.countryId, 0) + 1
                );
            }
        }

        return counts;
    }

    /**
     * Returns true if the given chunk already contains a claim flag block at a
     * position other than {@code ignorePos}.
     */
    public static boolean chunkHasOtherFlag(
        World world,
        ChunkPos chunkPos,
        BlockPos ignorePos
    ) {
        if (world == null || chunkPos == null) {
            return false;
        }

        Chunk chunk = world.getChunk(chunkPos.x, chunkPos.z);
        if (chunk == null) {
            return false;
        }

        ExtendedBlockStorage[] storages = chunk.getBlockStorageArray();
        for (ExtendedBlockStorage storage : storages) {
            if (storage == null || storage.isEmpty()) {
                continue;
            }
            int yBase = storage.getYLocation();
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        if (
                            storage.get(x, y, z).getBlock() !=
                            ModBlocks.CLAIM_FLAG
                        ) {
                            continue;
                        }
                        BlockPos found = new BlockPos(
                            (chunkPos.x << 4) + x,
                            yBase + y,
                            (chunkPos.z << 4) + z
                        );
                        if (ignorePos != null && ignorePos.equals(found)) {
                            continue;
                        }
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private static boolean isValidFlag(
        World world,
        ChunkPos chunk,
        BlockPos flagPos
    ) {
        if (world == null || chunk == null || flagPos == null) {
            return false;
        }
        if (
            new ChunkPos(flagPos).x != chunk.x ||
            new ChunkPos(flagPos).z != chunk.z
        ) {
            return false;
        }
        IBlockState state = world.getBlockState(flagPos);
        return state != null && state.getBlock() == ModBlocks.CLAIM_FLAG;
    }
}
