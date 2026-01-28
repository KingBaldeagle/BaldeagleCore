package com.baldeagle.economy;

import com.baldeagle.country.Country;
import com.baldeagle.country.CountryManager;
import com.baldeagle.country.CountryStorage;
import java.util.Map;
import java.util.UUID;
import net.minecraft.world.World;

public class EconomyManager {

    private EconomyManager() {}

    private static EconomyData getData(World world) {
        return EconomyData.get(world);
    }

    public static long getPlayerBalance(World world, UUID player) {
        return getData(world).getPlayerBalances().getOrDefault(player, 0L);
    }

    public static void depositPlayer(World world, UUID player, long amount) {
        EconomyData data = getData(world);
        Map<UUID, Long> balances = data.getPlayerBalances();
        balances.put(player, getPlayerBalance(world, player) + amount);
        data.markDirty();
    }

    public static boolean withdrawPlayer(
        World world,
        UUID player,
        long amount
    ) {
        EconomyData data = getData(world);
        Map<UUID, Long> balances = data.getPlayerBalances();
        long current = getPlayerBalance(world, player);
        if (current >= amount) {
            balances.put(player, current - amount);
            data.markDirty();
            return true;
        }
        return false;
    }

    public static long getCountryBalance(World world, String country) {
        if (world == null || country == null) {
            return 0L;
        }
        Country c = CountryManager.getCountryByName(world, country);
        return c != null ? c.getBalance() : 0L;
    }

    public static void depositCountry(
        World world,
        String country,
        long amount
    ) {
        if (world == null || country == null || amount <= 0L) {
            return;
        }
        Country c = CountryManager.getCountryByName(world, country);
        if (c == null) {
            return;
        }
        c.setBalance(c.getBalance() + amount);
        CountryStorage.get(world).markDirty();
    }

    public static boolean withdrawCountry(
        World world,
        String country,
        long amount
    ) {
        if (world == null || country == null || amount <= 0L) {
            return false;
        }
        Country c = CountryManager.getCountryByName(world, country);
        if (c == null) {
            return false;
        }
        long current = c.getBalance();
        if (current < amount) {
            return false;
        }
        c.setBalance(current - amount);
        CountryStorage.get(world).markDirty();
        return true;
    }

    public static void applyInterest(World world, double rate) {
        EconomyData data = getData(world);
        boolean dataChanged = false;

        Map<UUID, Long> playerBalances = data.getPlayerBalances();
        for (Map.Entry<UUID, Long> entry : playerBalances.entrySet()) {
            long current = entry.getValue();
            if (current <= 0) {
                continue;
            }
            long interest = Math.round(current * rate);
            if (interest > 0) {
                entry.setValue(current + interest);
                dataChanged = true;
            }
        }

        if (dataChanged) {
            data.markDirty();
        }

        CountryStorage storage = CountryStorage.get(world);
        boolean storageChanged = false;
        for (Country country : storage.getCountriesMap().values()) {
            long before = country.getBalance();
            country.applyInterest(rate);
            if (country.getBalance() != before) {
                storageChanged = true;
            }
        }

        if (storageChanged) {
            storage.markDirty();
        }
    }
}
