package com.baldeagle.territory;

import com.baldeagle.country.Country;
import com.baldeagle.country.CountryManager;
import com.baldeagle.country.CountryStorage;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

public final class NationManager {

    private NationManager() {}

    public static Country getNation(World world, UUID nationId) {
        return CountryManager.getCountry(world, nationId);
    }

    public static Country getNationByName(World world, String name) {
        return CountryManager.getCountryByName(world, name);
    }

    public static Country getNationForPlayer(World world, UUID playerId) {
        return CountryManager.getCountryForPlayer(world, playerId);
    }

    public static boolean setCapital(
        World world,
        UUID nationId,
        ChunkPos capitalChunk,
        BlockPos flagPos
    ) {
        Country nation = CountryManager.getCountry(world, nationId);
        if (nation == null) {
            return false;
        }

        if (!TerritoryManager.isChunkClaimed(world, capitalChunk)) {
            return false;
        }

        UUID ownerId = TerritoryManager.getOwningCountryId(world, capitalChunk);
        if (!nationId.equals(ownerId)) {
            return false;
        }

        return true;
    }

    public static Set<ChunkPos> getNationChunks(
        World world,
        UUID nationId
    ) {
        Set<ChunkPos> chunks = new HashSet<>();
        
        World countryWorld = getCountryWorld(world);
        TerritoryData data = TerritoryData.get(countryWorld);
        
        for (Map.Entry<TerritoryManager.DimChunkKey, TerritoryData.ClaimEntry> entry : 
             data.getClaims().entrySet()) {
            if (entry.getValue().countryId.equals(nationId)) {
                chunks.add(new ChunkPos(entry.getKey().chunkX, entry.getKey().chunkZ));
            }
        }
        
        return chunks;
    }

    public static int getChunkCount(World world, UUID nationId) {
        return getNationChunks(world, nationId).size();
    }

    public static boolean isChunkContested(World world, ChunkPos chunk) {
        CaptureSystem.CaptureEntry capture = CaptureSystem.getCapture(world, chunk);
        return capture != null && capture.stage == CaptureSystem.CaptureStage.CONTESTED;
    }

    public static boolean isChunkOccupied(World world, ChunkPos chunk) {
        CaptureSystem.CaptureEntry capture = CaptureSystem.getCapture(world, chunk);
        return capture != null && capture.stage == CaptureSystem.CaptureStage.OCCUPIED;
    }

    public static boolean isChunkUnderCapture(World world, ChunkPos chunk) {
        return CaptureSystem.getCapture(world, chunk) != null;
    }

    public static RelationshipSystem.RelationshipState getRelation(
        World world,
        UUID nation1Id,
        UUID nation2Id
    ) {
        return RelationshipSystem.getRelationship(world, nation1Id, nation2Id);
    }

    public static boolean declareWar(
        World world,
        UUID attackerId,
        UUID targetId
    ) {
        if (!RelationshipSystem.canDeclareWar(world, attackerId, targetId)) {
            return false;
        }
        return WarManager.declareWar(world, attackerId, targetId);
    }

    public static boolean endWar(
        World world,
        UUID nation1Id,
        UUID nation2Id
    ) {
        return WarManager.endWar(world, nation1Id, nation2Id);
    }

    public static boolean proposeAlliance(
        World world,
        UUID requesterId,
        UUID targetId
    ) {
        return RelationshipSystem.proposeAlliance(world, requesterId, targetId);
    }

    public static boolean acceptAlliance(
        World world,
        UUID acceptorId,
        UUID requesterId
    ) {
        return RelationshipSystem.acceptAlliance(world, acceptorId, requesterId);
    }

    public static boolean breakAlliance(
        World world,
        UUID breakerId,
        UUID targetId
    ) {
        return RelationshipSystem.breakAlliance(world, breakerId, targetId);
    }

    public static void broadcastToNation(
        World world,
        UUID nationId,
        String message
    ) {
        Country nation = CountryManager.getCountry(world, nationId);
        if (nation == null) {
            return;
        }

        for (UUID memberId : nation.getMembers().keySet()) {
            net.minecraft.entity.player.EntityPlayer player = 
                world.getPlayerEntityByUUID(memberId);
            if (player != null) {
                player.sendStatusMessage(new TextComponentString(message), true);
            }
        }
    }

    public static String getNationInfo(World world, UUID nationId) {
        Country nation = CountryManager.getCountry(world, nationId);
        if (nation == null) {
            return "Nation not found";
        }

        int chunkCount = getChunkCount(world, nationId);
        long income = TerritoryEconomy.calculateIncome(chunkCount);

        StringBuilder info = new StringBuilder();
        info.append("§6=== ").append(nation.getName()).append(" ===\n");
        info.append("§eBalance: §f").append(nation.getBalance()).append("\n");
        info.append("§eTreasury: §f").append(nation.getTreasury()).append("\n");
        info.append("§eChunks: §f").append(chunkCount).append("\n");
        info.append("§eIncome: §f").append(income).append("/day\n");
        info.append("§eMembers: §f").append(nation.getMembers().size()).append("\n");
        info.append("§eAllies: §f").append(nation.getAllies().size()).append("\n");
        info.append("§eWars: §f").append(nation.getWars().size());

        return info.toString();
    }

    private static World getCountryWorld(World world) {
        if (world == null) {
            return null;
        }
        if (world.getMinecraftServer() == null) {
            return world;
        }
        World overworld = world.getMinecraftServer().getWorld(0);
        return overworld != null ? overworld : world;
    }
}
