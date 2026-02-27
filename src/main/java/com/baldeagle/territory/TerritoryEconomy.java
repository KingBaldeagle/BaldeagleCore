package com.baldeagle.territory;

import com.baldeagle.config.BaldeagleConfig;

public final class TerritoryEconomy {

    private static final long BASE_INCOME_PER_CHUNK = 100L;

    private TerritoryEconomy() {}

    public static long calculateIncome(int claimedChunks) {
        if (claimedChunks <= 0) {
            return 0L;
        }

        long income = claimedChunks * BASE_INCOME_PER_CHUNK;
        
        if (income > Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }
        
        return Math.max(0L, income);
    }
}
