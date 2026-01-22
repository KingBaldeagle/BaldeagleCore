package com.baldeagle.country.currency;

import java.util.Locale;

public enum CurrencyType {
    COIN("coin"),
    BILL("bill");

    private final String nbtKey;

    CurrencyType(String nbtKey) {
        this.nbtKey = nbtKey;
    }

    public String getNbtKey() {
        return nbtKey;
    }

    public static CurrencyType fromNbt(String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        for (CurrencyType type : values()) {
            if (type.nbtKey.equalsIgnoreCase(key)) {
                return type;
            }
        }
        try {
            return CurrencyType.valueOf(key.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
