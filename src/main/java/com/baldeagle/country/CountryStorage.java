package com.baldeagle.country;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.storage.WorldSavedData;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class CountryStorage extends WorldSavedData {

    private static final String DATA_NAME = "custom_countries";

    public CountryStorage() {
        super(DATA_NAME);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        CountryManager.clear();
        NBTTagList countryList = nbt.getTagList("countries", 10); // 10 = compound

        for (int i = 0; i < countryList.tagCount(); i++) {
            NBTTagCompound countryTag = countryList.getCompoundTagAt(i);

            String name = countryTag.getString("name");
            UUID id = UUID.fromString(countryTag.getString("id"));
            double balance = countryTag.getDouble("balance");

            // Create a temporary creator UUID, will overwrite members below
            Country country = new Country(name, UUID.randomUUID());

            try {
                // Replace UUID
                java.lang.reflect.Field idField = Country.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(country, id);
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Set balance
            try {
                java.lang.reflect.Field balanceField = Country.class.getDeclaredField("balance");
                balanceField.setAccessible(true);
                balanceField.set(country, balance);
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Load members
            NBTTagList membersList = countryTag.getTagList("members", 10);
            for (int j = 0; j < membersList.tagCount(); j++) {
                NBTTagCompound memberTag = membersList.getCompoundTagAt(j);
                UUID playerId = UUID.fromString(memberTag.getString("uuid"));
                Country.Role role = Country.Role.valueOf(memberTag.getString("role"));
                try {
                    java.lang.reflect.Field membersField = Country.class.getDeclaredField("members");
                    membersField.setAccessible(true);
                    @SuppressWarnings("unchecked")
                    Map<UUID, Country.Role> members = (Map<UUID, Country.Role>) membersField.get(country);
                    members.put(playerId, role);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // Load join requests
            NBTTagList requestsList = countryTag.getTagList("joinRequests", 8); // 8 = string
            try {
                java.lang.reflect.Field requestsField = Country.class.getDeclaredField("joinRequests");
                requestsField.setAccessible(true);
                @SuppressWarnings("unchecked")
                Set<UUID> joinRequests = (Set<UUID>) requestsField.get(country);
                for (int j = 0; j < requestsList.tagCount(); j++) {
                    joinRequests.add(UUID.fromString(requestsList.getStringTagAt(j)));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Register country
            CountryManager.getAllCountries().put(id, country);
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        NBTTagList countryList = new NBTTagList();

        for (Country c : CountryManager.getAllCountries().values()) {
            NBTTagCompound countryTag = new NBTTagCompound();
            countryTag.setString("name", c.getName());
            countryTag.setString("id", c.getId().toString());
            countryTag.setDouble("balance", c.getBalance());

            // Save members
            NBTTagList membersList = new NBTTagList();
            for (Map.Entry<UUID, Country.Role> entry : c.getMembers().entrySet()) {
                NBTTagCompound memberTag = new NBTTagCompound();
                memberTag.setString("uuid", entry.getKey().toString());
                memberTag.setString("role", entry.getValue().name());
                membersList.appendTag(memberTag);
            }
            countryTag.setTag("members", membersList);

            // Save join requests
            NBTTagList requestsList = new NBTTagList();
            for (UUID request : c.getJoinRequests()) {
                requestsList.appendTag(new net.minecraft.nbt.NBTTagString(request.toString()));
            }
            countryTag.setTag("joinRequests", requestsList);

            countryList.appendTag(countryTag);
        }

        nbt.setTag("countries", countryList);
        return nbt;
    }

    public static CountryStorage get(World world) {
        CountryStorage storage = (CountryStorage) world.getMapStorage().getOrLoadData(CountryStorage.class, DATA_NAME);
        if (storage == null) {
            storage = new CountryStorage();
            world.getMapStorage().setData(DATA_NAME, storage);
        }
        return storage;
    }
}
