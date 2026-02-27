package com.baldeagle.territory;

import com.baldeagle.country.Country;
import com.baldeagle.country.CountryManager;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

public final class CaptureSystem {

    public enum CaptureStage {
        CONTESTED,
        OCCUPIED,
        ANNEXED
    }

    public static final class CaptureEntry {
        public final UUID attackerId;
        public final UUID defenderId;
        public final BlockPos captureBlockPos;
        public final long startTime;
        public long captureProgressTicks;
        public CaptureStage stage;

        public CaptureEntry(
            UUID attackerId,
            UUID defenderId,
            BlockPos captureBlockPos,
            long startTime
        ) {
            this.attackerId = attackerId;
            this.defenderId = defenderId;
            this.captureBlockPos = captureBlockPos;
            this.startTime = startTime;
            this.captureProgressTicks = 0L;
            this.stage = CaptureStage.CONTESTED;
        }
    }

    private static final long TICKS_PER_HOUR = 1000L;
    private static final long HOURS_CONTESTED = 8L;
    private static final long HOURS_OCCUPIED = 12L;
    private static final long HOURS_ANNEXED = 8L;
    private static final long TOTAL_CAPTURE_HOURS = HOURS_CONTESTED + HOURS_OCCUPIED + HOURS_ANNEXED;
    private static final long TOTAL_CAPTURE_TICKS = TOTAL_CAPTURE_HOURS * TICKS_PER_HOUR;

    private static final long TICKS_CONTESTED = HOURS_CONTESTED * TICKS_PER_HOUR;
    private static final long TICKS_OCCUPIED = HOURS_OCCUPIED * TICKS_PER_HOUR;

    private CaptureSystem() {}

    public static void startCapture(
        World world,
        ChunkPos chunk,
        UUID attackerId,
        UUID defenderId,
        BlockPos captureBlockPos
    ) {
        TerritoryData data = TerritoryData.get(world);
        TerritoryManager.DimChunkKey key = TerritoryManager.chunkKey(world, chunk);
        
        data.getCaptureData().put(key, new CaptureEntry(
            attackerId,
            defenderId,
            captureBlockPos,
            world.getTotalWorldTime()
        ));
        
        data.markDirty();
        
        broadcastCaptureEvent(world, attackerId, defenderId, chunk, "started");
    }

    public static void resetCapture(World world, ChunkPos chunk) {
        TerritoryData data = TerritoryData.get(world);
        TerritoryManager.DimChunkKey key = TerritoryManager.chunkKey(world, chunk);
        
        CaptureEntry entry = data.getCaptureData().remove(key);
        if (entry != null) {
            broadcastCaptureEvent(world, entry.attackerId, entry.defenderId, chunk, "reset");
        }
        
        data.markDirty();
    }

    public static void processCaptureTick(World world) {
        if (world.isRemote) {
            return;
        }
        if (world.provider.getDimension() != 0) {
            return;
        }

        TerritoryData data = TerritoryData.get(world);
        
        if (!isCaptureActive(world)) {
            return;
        }

        Map<TerritoryManager.DimChunkKey, CaptureEntry> captures = data.getCaptureData();
        
        Set<TerritoryManager.DimChunkKey> toRemove = new HashSet<>();
        
        for (Map.Entry<TerritoryManager.DimChunkKey, CaptureEntry> entry : captures.entrySet()) {
            CaptureEntry capture = entry.getValue();
            
            updateStage(capture);
            
            if (isPaused(world, capture)) {
                continue;
            }
            
            capture.captureProgressTicks++;
            
            if (capture.captureProgressTicks >= TOTAL_CAPTURE_TICKS) {
                completeCapture(world, entry.getKey(), capture);
                toRemove.add(entry.getKey());
            } else if (capture.stage == CaptureStage.ANNEXED) {
                checkAnnexation(world, entry.getKey(), capture);
            }
        }
        
        for (TerritoryManager.DimChunkKey key : toRemove) {
            captures.remove(key);
        }
        
        if (!toRemove.isEmpty()) {
            data.markDirty();
        }
    }

    private static void updateStage(CaptureEntry capture) {
        long progress = capture.captureProgressTicks;
        
        if (progress < TICKS_CONTESTED) {
            capture.stage = CaptureStage.CONTESTED;
        } else if (progress < TICKS_CONTESTED + TICKS_OCCUPIED) {
            capture.stage = CaptureStage.OCCUPIED;
        } else {
            capture.stage = CaptureStage.ANNEXED;
        }
    }

    private static boolean isPaused(World world, CaptureEntry capture) {
        Country attacker = CountryManager.getCountry(world, capture.attackerId);
        Country defender = CountryManager.getCountry(world, capture.defenderId);
        
        boolean attackerOnline = hasOnlineMember(world, capture.attackerId);
        boolean defenderOnline = hasOnlineMember(world, capture.defenderId);
        
        return !attackerOnline && !defenderOnline;
    }

    private static boolean hasOnlineMember(World world, UUID countryId) {
        Country country = CountryManager.getCountry(world, countryId);
        if (country == null) {
            return false;
        }
        
        for (UUID memberId : country.getMembers().keySet()) {
            if (world.getPlayerEntityByUUID(memberId) != null) {
                return true;
            }
        }
        return false;
    }

    private static void completeCapture(World world, TerritoryManager.DimChunkKey key, CaptureEntry capture) {
        TerritoryData data = TerritoryData.get(world);
        
        data.getClaims().remove(key);
        
        data.getClaims().put(key, new TerritoryData.ClaimEntry(
            capture.attackerId,
            capture.captureBlockPos
        ));
        
        Country attacker = CountryManager.getCountry(world, capture.attackerId);
        if (attacker != null) {
            attacker.addTreasury(50);
        }
        
        broadcastCaptureEvent(world, capture.attackerId, capture.defenderId, 
            new ChunkPos(key.chunkX, key.chunkZ), "completed");
    }

    private static void checkAnnexation(World world, TerritoryManager.DimChunkKey key, CaptureEntry capture) {
        if (capture.captureProgressTicks % TICKS_PER_HOUR == 0) {
            Country attacker = CountryManager.getCountry(world, capture.attackerId);
            Country defender = CountryManager.getCountry(world, capture.defenderId);
            
            if (attacker != null) {
                attacker.addTreasury(10);
            }
        }
    }

    public static boolean hasSupplyLine(World world, ChunkPos targetChunk, UUID attackerId) {
        Set<TerritoryManager.DimChunkKey> visited = new HashSet<>();
        return checkSupplyLine(world, targetChunk, attackerId, visited, 0);
    }

    private static boolean checkSupplyLine(
        World world, 
        ChunkPos current, 
        UUID attackerId, 
        Set<TerritoryManager.DimChunkKey> visited,
        int depth
    ) {
        if (depth > 100) {
            return false;
        }
        
        TerritoryManager.DimChunkKey key = TerritoryManager.chunkKey(world, current);
        
        if (visited.contains(key)) {
            return false;
        }
        visited.add(key);
        
        UUID ownerId = TerritoryManager.getOwningCountryId(world, current);
        if (ownerId != null && ownerId.equals(attackerId)) {
            return true;
        }
        
        int[][] neighbors = {
            {current.x + 1, current.z},
            {current.x - 1, current.z},
            {current.x, current.z + 1},
            {current.x, current.z - 1}
        };
        
        for (int[] neighbor : neighbors) {
            ChunkPos neighborChunk = new ChunkPos(neighbor[0], neighbor[1]);
            
            UUID neighborOwner = TerritoryManager.getOwningCountryId(world, neighborChunk);
            if (neighborOwner != null && neighborOwner.equals(attackerId)) {
                return true;
            }
            
            if (neighborOwner == null) {
                continue;
            }
            
            if (checkSupplyLine(world, neighborChunk, attackerId, visited, depth + 1)) {
                return true;
            }
        }
        
        return false;
    }

    public static void defenderSlowCapture(World world, ChunkPos chunk, UUID defenderId) {
        TerritoryData data = TerritoryData.get(world);
        TerritoryManager.DimChunkKey key = TerritoryManager.chunkKey(world, chunk);
        CaptureEntry capture = data.getCaptureData().get(key);
        
        if (capture != null && capture.defenderId.equals(defenderId)) {
            capture.captureProgressTicks = Math.max(0, capture.captureProgressTicks - 100);
        }
    }

    public static CaptureEntry getCapture(World world, ChunkPos chunk) {
        TerritoryData data = TerritoryData.get(world);
        TerritoryManager.DimChunkKey key = TerritoryManager.chunkKey(world, chunk);
        return data.getCaptureData().get(key);
    }

    public static boolean isCaptureActive(World world) {
        return world.playerEntities != null && !world.playerEntities.isEmpty();
    }

    public static long getTicksRemaining(CaptureEntry capture) {
        return Math.max(0, TOTAL_CAPTURE_TICKS - capture.captureProgressTicks);
    }

    public static double getCaptureProgress(CaptureEntry capture) {
        return (double) capture.captureProgressTicks / TOTAL_CAPTURE_TICKS;
    }

    private static void broadcastCaptureEvent(
        World world, 
        UUID attackerId, 
        UUID defenderId, 
        ChunkPos chunk,
        String eventType
    ) {
        Country attacker = CountryManager.getCountry(world, attackerId);
        Country defender = CountryManager.getCountry(world, defenderId);
        
        String attackerName = attacker != null ? attacker.getName() : "Unknown";
        String defenderName = defender != null ? defender.getName() : "Unknown";
        
        String message;
        switch (eventType) {
            case "started":
                message = String.format("[WAR] %s has started capturing a chunk from %s at (%d, %d)",
                    attackerName, defenderName, chunk.x, chunk.z);
                break;
            case "reset":
                message = String.format("[WAR] Capture at (%d, %d) has been reset", chunk.x, chunk.z);
                break;
            case "completed":
                message = String.format("[WAR] %s has successfully captured a chunk from %s at (%d, %d)",
                    attackerName, defenderName, chunk.x, chunk.z);
                break;
            default:
                message = String.format("[WAR] Capture update at (%d, %d)", chunk.x, chunk.z);
        }
        
        world.getMinecraftServer().getPlayerList().sendMessage(
            new net.minecraft.util.text.TextComponentString(message)
        );
    }
}
