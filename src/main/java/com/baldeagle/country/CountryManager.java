package com.baldeagle.country;

import net.minecraft.world.World;

import java.util.Map;
import java.util.UUID;

public class CountryManager {

    // --- Get all countries for a world ---
    public static Map<UUID, Country> getAllCountries(World world) {
        return CountryStorage.get(world).getCountriesMap();
    }

    // --- Clear all countries in a world ---
    public static void clear(World world) {
        getAllCountries(world).clear();
        CountryStorage.get(world).markDirty();
    }

    // --- Create a new country in a world ---
    public static Country createCountry(World world, String name, UUID creatorUUID) {
        Map<UUID, Country> countries = getAllCountries(world);

        // Check name availability
        for (Country c : countries.values()) {
            if (c.getName().equalsIgnoreCase(name)) {
                throw new IllegalArgumentException("Country name already exists");
            }
        }

        Country country = new Country(name, creatorUUID);
        countries.put(country.getId(), country);

        // Save changes
        CountryStorage.get(world).markDirty();

        return country;
    }

    // --- Get country by UUID ---
    public static Country getCountry(World world, UUID id) {
        return getAllCountries(world).get(id);
    }

    // --- Get country by name ---
    public static Country getCountryByName(World world, String name) {
        for (Country c : getAllCountries(world).values()) {
            if (c.getName().equalsIgnoreCase(name)) return c;
        }
        return null;
    }
}
