package com.baldeagle.country;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;

public class CountryStorage extends WorldSavedData {

    private static final String DATA_NAME = "baldeagle_countries";

    private final Map<UUID, Country> countries = new HashMap<>();

    public CountryStorage() {
        super(DATA_NAME);
    }

    public CountryStorage(String name) {
        super(name);
    }

    public Map<UUID, Country> getCountriesMap() {
        return countries;
    }

    private final Map<UUID, Long> playerBalances = new HashMap<>();

    public Map<UUID, Long> getPlayerBalances() {
        return playerBalances;
    }

    public static CountryStorage get(World world) {
        MapStorage storage = world.getMapStorage();
        CountryStorage instance = (CountryStorage) storage.getOrLoadData(
            CountryStorage.class,
            DATA_NAME
        );
        if (instance == null) {
            instance = new CountryStorage();
            storage.setData(DATA_NAME, instance);
        }
        return instance;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        countries.clear();
        NBTTagList list = nbt.getTagList("countries", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound countryTag = list.getCompoundTagAt(i);
            Country country = Country.fromNBT(countryTag);
            countries.put(country.getId(), country);
        }

        playerBalances.clear();
        NBTTagList players = nbt.getTagList("playerBalances", 10);
        for (int i = 0; i < players.tagCount(); i++) {
            NBTTagCompound entry = players.getCompoundTagAt(i);
            String uuidString = entry.getString("UUID");
            if (uuidString == null || uuidString.isEmpty()) {
                continue;
            }
            UUID uuid = UUID.fromString(uuidString);
            long balance = entry.getLong("Balance");
            playerBalances.put(uuid, balance);
        }
        // Validate alliance invariants after all countries are loaded.
        boolean changed = false;
        for (Country c : countries.values()) {
            // Remove invalid/self references.
            if (c.getAllies().remove(c.getId())) {
                changed = true;
            }
            if (c.getIncomingAllianceRequests().remove(c.getId())) {
                changed = true;
            }
            if (c.getWars().remove(c.getId())) {
                changed = true;
            }

            if (c.getPresidentUuid() == null) {
                // Can't manage alliances without a president, but preserve data.
            }

            c.getAllies().removeIf(id -> !countries.containsKey(id));
            c
                .getIncomingAllianceRequests()
                .removeIf(id -> !countries.containsKey(id));
            c.getWars().removeIf(id -> !countries.containsKey(id));
        }

        // Enforce bidirectional allies: if A lists B, B must list A.
        for (Country a : countries.values()) {
            for (java.util.UUID bId : new java.util.HashSet<>(a.getAllies())) {
                Country b = countries.get(bId);
                if (b == null) {
                    continue;
                }
                if (!b.getAllies().contains(a.getId())) {
                    b.getAllies().add(a.getId());
                    changed = true;
                }
            }
        }

        // Enforce bidirectional wars: if A lists B, B must list A.
        for (Country a : countries.values()) {
            for (java.util.UUID bId : new java.util.HashSet<>(a.getWars())) {
                Country b = countries.get(bId);
                if (b == null) {
                    continue;
                }
                if (!b.getWars().contains(a.getId())) {
                    b.getWars().add(a.getId());
                    changed = true;
                }
            }
        }

        if (changed) {
            markDirty();
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        NBTTagList list = new NBTTagList();
        for (Country c : countries.values()) {
            list.appendTag(c.writeToNBT());
        }
        compound.setTag("countries", list);
        return compound;
    }
}
