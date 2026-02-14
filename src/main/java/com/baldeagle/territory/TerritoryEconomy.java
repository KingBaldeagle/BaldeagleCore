package com.baldeagle.territory;

import com.baldeagle.config.BaldeagleConfig;

public final class TerritoryEconomy {

    private TerritoryEconomy() {}

    public static long calculateIncome(int claimedChunks) {
        if (claimedChunks <= 0) {
            return 0L;
        }

        // Each additional chunk adds a bonus to ALL chunks
        // Example with base=100, bonus=0.05: 3 chunks = 3 * 100 * (1 + 2*0.05) = 330
        double bonusMultiplier = 1.0D + (claimedChunks - 1) * (BaldeagleConfig.territoryChunkMultiplier - 1.0D);
        double incomePerChunk = BaldeagleConfig.territoryBaseChunkIncome * bonusMultiplier;
        double totalIncome = incomePerChunk * claimedChunks;
        if (Double.isNaN(totalIncome) || Double.isInfinite(totalIncome)) {
            return 0L;
        }
        return Math.max(0L, Math.round(totalIncome));
    }
}
