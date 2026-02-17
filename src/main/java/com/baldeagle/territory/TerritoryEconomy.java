package com.baldeagle.territory;

import com.baldeagle.config.BaldeagleConfig;

public final class TerritoryEconomy {

    private TerritoryEconomy() {}

    public static long calculateIncome(int claimedChunks) {
        if (claimedChunks <= 0) {
            return 0L;
        }

        // Calculate per-chunk amount with bonus applied once per chunk count
        // Formula: chunks * base * (1 + (chunks-1) * bonusPercent)
        // Example: 4 chunks, base=100, bonus=5%
        // Per chunk: 100 * (1 + 3*0.05) = 100 * 1.15 = 115
        // Total: 115 * 4 = 460
        double bonusPercent = BaldeagleConfig.territoryChunkMultiplier - 1.0D;
        double perChunkMultiplier = 1.0D + (claimedChunks - 1) * bonusPercent;
        double incomePerChunk = BaldeagleConfig.territoryBaseChunkIncome * perChunkMultiplier;
        double totalIncome = incomePerChunk * claimedChunks;
        
        if (Double.isNaN(totalIncome) || Double.isInfinite(totalIncome)) {
            return 0L;
        }
        
        // Cap at reasonable maximum to prevent config errors from breaking economy
        if (totalIncome > Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }
        
        return Math.max(0L, Math.round(totalIncome));
    }
}
