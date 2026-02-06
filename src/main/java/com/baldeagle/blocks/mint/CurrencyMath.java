package com.baldeagle.blocks.mint;

import com.baldeagle.country.Country;

public final class CurrencyMath {

    private CurrencyMath() {}

    public static double computeExchangeRate(Country source, Country target) {
        if (source == null || target == null) {
            return 0;
        }
        double baseA = source.getExchangeValue();
        double baseB = target.getExchangeValue();
        if (baseA <= 0 || baseB <= 0) {
            return 0;
        }

        double infA = source.getInflation();
        double infB = target.getInflation();
        if (infA <= 0 || infB <= 0) {
            return 0;
        }

        double realA = baseA / infA;
        double realB = baseB / infB;
        if (realA <= 0 || realB <= 0) {
            return 0;
        }

        return realA / realB;
    }

    public static double computeLiquidityMultiplier(
        Country source,
        long inputFaceValue
    ) {
        if (source == null || inputFaceValue <= 0) {
            return 1.0D;
        }
        long supply = Math.max(1, source.getMoneyInCirculation());
        double fraction = inputFaceValue / (double) supply;
        return (
            1.0D /
            (1.0D + (fraction * MintingConstants.EXCHANGE_LIQUIDITY_FACTOR))
        );
    }
}
