package com.baldeagle.country;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerEconomy {

    private final Map<UUID, Long> balances = new HashMap<>();

    public long getBalance(UUID player) {
        return balances.getOrDefault(player, 0L);
    }

    public void setBalance(UUID player, long amount) {
        balances.put(player, Math.max(0L, amount));
    }

    public void addBalance(UUID player, long amount) {
        balances.put(player, Math.max(0L, getBalance(player) + amount));
    }

    public void subtractBalance(UUID player, long amount) {
        long current = getBalance(player);
        if (amount > current) throw new IllegalArgumentException(
            "Insufficient funds"
        );
        balances.put(player, current - amount);
    }

    // Save/load to CountryStorage
    public void readFromNBT(CountryStorage storage) {
        for (Map.Entry<UUID, Long> entry : storage
            .getPlayerBalances()
            .entrySet()) {
            balances.put(entry.getKey(), entry.getValue());
        }
    }

    public void writeToNBT(CountryStorage storage) {
        storage.getPlayerBalances().clear();
        storage.getPlayerBalances().putAll(balances);
    }
}
