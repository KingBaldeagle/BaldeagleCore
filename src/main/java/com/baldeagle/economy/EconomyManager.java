package com.baldeagle.economy;

import java.util.UUID;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.World;

public class EconomyManager {

    private static final Map<UUID, Long> playerBalances = new HashMap<>();
    private static final Map<String, Long> countryBalances = new HashMap<>();

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

    public static boolean withdrawPlayer(World world, UUID player, long amount) {
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

    // Country methods
    public static long getCountryBalance(World world, String country) {
        return getData(world).getCountryBalances().getOrDefault(country, 0L);
    }

    public static void depositCountry(World world, String country, long amount) {
        EconomyData data = getData(world);
        Map<String, Long> balances = data.getCountryBalances();
        balances.put(country, getCountryBalance(world, country) + amount);
        data.markDirty();
    }

    public static boolean withdrawCountry(World world, String country, long amount) {
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

}
