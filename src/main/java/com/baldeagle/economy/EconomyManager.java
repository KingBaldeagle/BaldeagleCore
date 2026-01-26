package com.baldeagle.economy;

import com.baldeagle.country.Country;
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
        return getData(world).getCountryBalances().getOrDefault(country, 0L);
    }

    public static void depositCountry(
        World world,
        String country,
        long amount
    ) {
        EconomyData data = getData(world);
        Map<String, Long> balances = data.getCountryBalances();
        balances.put(country, getCountryBalance(world, country) + amount);
        data.markDirty();
    }

    public static boolean withdrawCountry(
        World world,
        String country,
        long amount
    ) {
        EconomyData data = getData(world);
        Map<String, Long> balances = data.getCountryBalances();
        long current = getCountryBalance(world, country);
        if (current >= amount) {
            balances.put(country, current - amount);
            data.markDirty();
            return true;
        }
        return false;
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

        Map<String, Long> countryBalances = data.getCountryBalances();
        for (Map.Entry<String, Long> entry : countryBalances.entrySet()) {
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
            long balance = country.getBalance();
            if (balance <= 0) {
                continue;
            }
            long interest = Math.round(balance * rate);
            if (interest > 0) {
                country.setBalance(balance + interest);
                storageChanged = true;
            }
        }

        if (storageChanged) {
            storage.markDirty();
        }
    }
}
