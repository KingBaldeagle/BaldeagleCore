package com.baldeagle.country;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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

    // Inside CountryStorage.java
    private final Map<UUID, Double> playerBalances = new HashMap<>();

    public Map<UUID, Double> getPlayerBalances() {
        return playerBalances;
    }

    public static CountryStorage get(World world) {
        MapStorage storage = world.getMapStorage();
        CountryStorage instance = (CountryStorage) storage.getOrLoadData(CountryStorage.class, DATA_NAME);
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
