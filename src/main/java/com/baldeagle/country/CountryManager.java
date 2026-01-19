package com.baldeagle.country;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CountryManager {

    private static final Map<UUID, Country> countries = new HashMap<>();

    // Create a country if the name is available
    public static Country createCountry(String name, UUID creatorUUID) {
        if (getCountryByName(name) != null) {
            throw new IllegalArgumentException("Country name already exists");
        }
        Country country = new Country(name, creatorUUID);
        countries.put(country.getId(), country);
        return country;
    }

    public static Country getCountry(UUID id) {
        return countries.get(id);
    }

    public static Country getCountryByName(String name) {
        return countries.values().stream()
                .filter(c -> c.getName().equalsIgnoreCase(name))
                .findFirst().orElse(null);
    }

    public static Map<UUID, Country> getAllCountries() {
        return countries;
    }

    public static void clear() {
        countries.clear();
    }
}
