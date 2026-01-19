package com.baldeagle.country;

import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerEconomy {

    private final Map<UUID, Double> balances = new HashMap<>();

    public double getBalance(UUID player) {
        return balances.getOrDefault(player, 0.0);
    }

    public void setBalance(UUID player, double amount) {
        balances.put(player, amount);
    }

    public void addBalance(UUID player, double amount) {
        balances.put(player, getBalance(player) + amount);
    }

    public void subtractBalance(UUID player, double amount) {
        double current = getBalance(player);
        if (amount > current) throw new IllegalArgumentException("Insufficient funds");
        balances.put(player, current - amount);
    }

    // Save/load to CountryStorage
    public void readFromNBT(CountryStorage storage) {
        for (Map.Entry<UUID, Double> entry : storage.getPlayerBalances().entrySet()) {
            balances.put(entry.getKey(), entry.getValue());
        }
    }

    public void writeToNBT(CountryStorage storage) {
        storage.getPlayerBalances().clear();
        storage.getPlayerBalances().putAll(balances);
    }
}
