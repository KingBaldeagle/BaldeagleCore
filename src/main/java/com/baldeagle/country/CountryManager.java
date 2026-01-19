package com.baldeagle.country;

import com.feed_the_beast.ftbu.api.IForgeTeam;
import com.feed_the_beast.ftbu.api.FTBUApi; // FTBU API
import net.minecraft.world.World;
import net.minecraft.world.storage.WorldSavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CountryManager {

    private static Map<UUID, Country> countries = new HashMap<>();

    // Load all teams as countries
    public static void registerAllCountries(World world) {
        // FTBU API provides all teams
        for (IForgeTeam team : FTBUApi.getTeamManager().getTeams()) {
            registerCountry(team);
        }
    }

    public static Country registerCountry(IForgeTeam team) {
        UUID teamID = team.getId();
        if (!countries.containsKey(teamID)) {
            Country country = new Country(team.getTitle(), teamID);
            countries.put(teamID, country);
        }
        return countries.get(teamID);
    }

    public static Country getCountry(UUID teamID) {
        return countries.get(teamID);
    }

    public static Country getCountry(String name) {
        for (Country c : countries.values()) {
            if (c.getName().equalsIgnoreCase(name)) return c;
        }
        return null;
    }

    public static Map<UUID, Country> getCountries() {
        return countries;
    }
}