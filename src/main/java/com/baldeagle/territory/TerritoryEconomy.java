package com.baldeagle.territory;

public final class TerritoryEconomy {

    // Base income per claimed chunk, paid each interval.
    public static final long BASE_CHUNK_INCOME = 100L;

    // Territory bonus multiplier: each additional chunk increases income by 5%.
    // Example: 1 chunk => 1.00x, 2 chunks => 1.05x, 3 chunks => 1.1025x, ...
    public static final double PER_ADDITIONAL_CHUNK_MULTIPLIER = 1.05D;

    private TerritoryEconomy() {}

    public static long calculateIncome(int claimedChunks) {
        if (claimedChunks <= 0) {
            return 0L;
        }

        double multiplier = Math.pow(
            PER_ADDITIONAL_CHUNK_MULTIPLIER,
            Math.max(0, claimedChunks - 1)
        );
        double income = BASE_CHUNK_INCOME * (double) claimedChunks * multiplier;
        if (Double.isNaN(income) || Double.isInfinite(income)) {
            return 0L;
        }
        return Math.max(0L, Math.round(income));
    }
}
