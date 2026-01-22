package com.baldeagle.country.mint;

import com.baldeagle.country.Country;

public final class CurrencyMath {

    private CurrencyMath() {}

    public static double computeExchangeRate(Country source, Country target) {
        if (source == null || target == null) {
            return 0;
        }
        double valueA = source.getExchangeValue();
        double valueB = target.getExchangeValue();
        if (valueA <= 0 || valueB <= 0) {
            return 0;
        }
        return valueA / valueB;
    }
}
