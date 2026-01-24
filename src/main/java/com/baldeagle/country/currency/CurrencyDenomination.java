package com.baldeagle.country.currency;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public enum CurrencyDenomination {
    COIN_1(CurrencyType.COIN, 1, "coin_1"),
    COIN_5(CurrencyType.COIN, 5, "coin_5"),
    COIN_10(CurrencyType.COIN, 10, "coin_10"),
    BILL_50(CurrencyType.BILL, 50, "bill_50"),
    BILL_100(CurrencyType.BILL, 100, "bill_100");

    private final CurrencyType type;
    private final int value;
    private final String id;

    CurrencyDenomination(CurrencyType type, int value, String id) {
        this.type = type;
        this.value = value;
        this.id = id;
    }

    public CurrencyType getType() {
        return type;
    }

    public int getValue() {
        return value;
    }

    public String getId() {
        return id;
    }

    public static CurrencyDenomination fromId(String id) {
        if (id == null) {
            return null;
        }
        for (CurrencyDenomination denomination : values()) {
            if (denomination.id.equalsIgnoreCase(id)) {
                return denomination;
            }
        }
        return null;
    }

    public static CurrencyDenomination firstOfType(CurrencyType type) {
        return Arrays.stream(values())
            .filter(denomination -> denomination.type == type)
            .min(Comparator.comparingInt(CurrencyDenomination::getValue))
            .orElse(null);
    }

    public static CurrencyDenomination next(
        CurrencyDenomination current,
        boolean forward
    ) {
        List<CurrencyDenomination> sorted = Arrays.stream(values())
            .filter(denomination -> denomination.type == current.type)
            .sorted(Comparator.comparingInt(CurrencyDenomination::getValue))
            .collect(Collectors.toList());

        if (sorted.isEmpty()) {
            return current;
        }

        int index = sorted.indexOf(current);
        if (index < 0) {
            return sorted.get(0);
        }

        int nextIndex =
            (index + (forward ? 1 : -1) + sorted.size()) % sorted.size();
        return sorted.get(nextIndex);
    }

    public static CurrencyDenomination nextAny(
        CurrencyDenomination current,
        boolean forward
    ) {
        List<CurrencyDenomination> sorted = Arrays.stream(values())
            .sorted(
                Comparator.comparingInt(
                    CurrencyDenomination::getValue
                ).thenComparingInt(a -> a.type.ordinal())
            )
            .collect(Collectors.toList());

        if (sorted.isEmpty()) {
            return current;
        }

        int index = sorted.indexOf(current);
        if (index < 0) {
            return sorted.get(0);
        }

        int nextIndex =
            (index + (forward ? 1 : -1) + sorted.size()) % sorted.size();
        return sorted.get(nextIndex);
    }
}
