package com.baldeagle.chunkmap;

import java.util.UUID;

public final class ChunkOwnershipInfo {

    public final int chunkX;
    public final int chunkZ;
    public final UUID ownerCountryId; // null if unclaimed
    public final String ownerName; // empty if unclaimed/unknown
    public final ChunkRelation relation;
    public final long incomePerDay; // informational (0 if unclaimed/unknown)

    public ChunkOwnershipInfo(
        int chunkX,
        int chunkZ,
        UUID ownerCountryId,
        String ownerName,
        ChunkRelation relation,
        long incomePerDay
    ) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.ownerCountryId = ownerCountryId;
        this.ownerName = ownerName != null ? ownerName : "";
        this.relation = relation != null ? relation : ChunkRelation.NEUTRAL;
        this.incomePerDay = Math.max(0L, incomePerDay);
    }
}
