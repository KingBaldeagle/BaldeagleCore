package com.baldeagle.territory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.storage.WorldSavedData;

public class TerritoryData extends WorldSavedData {

    public static final String DATA_NAME = "baldeaglecore-territory";

    public static final class ClaimEntry {

        public final UUID countryId;
        public final BlockPos flagPos;

        public ClaimEntry(UUID countryId, BlockPos flagPos) {
            this.countryId = countryId;
            this.flagPos = flagPos;
        }
    }

    // dimension+chunk -> claim entry
    private final Map<TerritoryManager.DimChunkKey, ClaimEntry> claims =
        new HashMap<>();

    // Used by the income tick handler (only read/written on overworld instance).
    private long lastPayoutTime = 0L;

    public TerritoryData() {
        super(DATA_NAME);
    }

    public TerritoryData(String name) {
        super(name);
    }

    public static TerritoryData get(World world) {
        TerritoryData data = (TerritoryData) world
            .getMapStorage()
            .getOrLoadData(TerritoryData.class, DATA_NAME);
        if (data == null) {
            data = new TerritoryData();
            world.getMapStorage().setData(DATA_NAME, data);
        }
        return data;
    }

    public Map<TerritoryManager.DimChunkKey, ClaimEntry> getClaims() {
        return claims;
    }

    public long getLastPayoutTime() {
        return lastPayoutTime;
    }

    public void setLastPayoutTime(long lastPayoutTime) {
        this.lastPayoutTime = Math.max(0L, lastPayoutTime);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        claims.clear();

        lastPayoutTime = nbt.getLong("LastPayoutTime");

        NBTTagList list = nbt.getTagList("Claims", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound tag = list.getCompoundTagAt(i);
            int chunkX = tag.getInteger("ChunkX");
            int chunkZ = tag.getInteger("ChunkZ");
            int dimension = tag.hasKey("Dimension")
                ? tag.getInteger("Dimension")
                : 0;
            UUID countryId = UUID.fromString(tag.getString("CountryId"));

            int fx = tag.getInteger("FlagX");
            int fy = tag.getInteger("FlagY");
            int fz = tag.getInteger("FlagZ");
            BlockPos flagPos = new BlockPos(fx, fy, fz);

            claims.put(
                TerritoryManager.chunkKey(dimension, chunkX, chunkZ),
                new ClaimEntry(countryId, flagPos)
            );
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        nbt.setLong("LastPayoutTime", lastPayoutTime);

        NBTTagList list = new NBTTagList();
        for (Map.Entry<
            TerritoryManager.DimChunkKey,
            ClaimEntry
        > entry : claims.entrySet()) {
            TerritoryManager.DimChunkKey key = entry.getKey();
            ClaimEntry claim = entry.getValue();

            NBTTagCompound tag = new NBTTagCompound();
            tag.setInteger("Dimension", key.dimension);
            tag.setInteger("ChunkX", key.chunkX);
            tag.setInteger("ChunkZ", key.chunkZ);
            tag.setString("CountryId", claim.countryId.toString());
            tag.setInteger("FlagX", claim.flagPos.getX());
            tag.setInteger("FlagY", claim.flagPos.getY());
            tag.setInteger("FlagZ", claim.flagPos.getZ());
            list.appendTag(tag);
        }
        nbt.setTag("Claims", list);
        return nbt;
    }
}
