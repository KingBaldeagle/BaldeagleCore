package com.baldeagle.economy;

import net.minecraft.world.storage.WorldSavedData;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EconomyData extends WorldSavedData {
    public static final String DATA_NAME = "baldeaglecore-economy";

    private Map<UUID, Long> playerBalances = new HashMap<>();
    private Map<String, Long> countryBalances = new HashMap<>();

    public EconomyData() {
        super(DATA_NAME);
    }

    public EconomyData(String name) {
        super(name);
    }

    public static EconomyData get(World world) {
        EconomyData data = (EconomyData) world.getMapStorage().getOrLoadData(EconomyData.class, DATA_NAME);
        if (data == null) {
            data = new EconomyData();
            world.getMapStorage().setData(DATA_NAME, data);
        }
        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        // Player balances
        NBTTagList players = nbt.getTagList("Players", 10);
        for (int i = 0; i < players.tagCount(); i++) {
            NBTTagCompound entry = players.getCompoundTagAt(i);
            UUID uuid = UUID.fromString(entry.getString("UUID"));
            long balance = entry.getLong("Balance");
            playerBalances.put(uuid, balance);
        }

        // Country balances
        NBTTagList countries = nbt.getTagList("Countries", 10);
        for (int i = 0; i < countries.tagCount(); i++) {
            NBTTagCompound entry = countries.getCompoundTagAt(i);
            String name = entry.getString("Name");
            long balance = entry.getLong("Balance");
            countryBalances.put(name, balance);
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        // Player balances
        NBTTagList players = new NBTTagList();
        for (Map.Entry<UUID, Long> entry : playerBalances.entrySet()) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString("UUID", entry.getKey().toString());
            tag.setLong("Balance", entry.getValue());
            players.appendTag(tag);
        }
        nbt.setTag("Players", players);

        // Country balances
        NBTTagList countries = new NBTTagList();
        for (Map.Entry<String, Long> entry : countryBalances.entrySet()) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString("Name", entry.getKey());
            tag.setLong("Balance", entry.getValue());
            countries.appendTag(tag);
        }
        nbt.setTag("Countries", countries);

        return nbt;
    }

    // Getter for manager
    public Map<UUID, Long> getPlayerBalances() {
        return playerBalances;
    }

    public Map<String, Long> getCountryBalances() {
        return countryBalances;
    }
}
