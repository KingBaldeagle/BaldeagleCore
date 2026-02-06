package com.baldeagle.country;

import com.baldeagle.ModBlocks;
import java.util.Map;
import java.util.UUID;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class CountryManager {

    private static World getCountryWorld(World world) {
        if (world == null) {
            return null;
        }
        if (world.getMinecraftServer() == null) {
            return world;
        }
        World overworld = world.getMinecraftServer().getWorld(0);
        return overworld != null ? overworld : world;
    }

    public static Map<UUID, Country> getAllCountries(World world) {
        World countryWorld = getCountryWorld(world);
        if (countryWorld == null) {
            throw new IllegalArgumentException("World is null");
        }
        return CountryStorage.get(countryWorld).getCountriesMap();
    }

    public static void clear(World world) {
        getAllCountries(world).clear();
        CountryStorage.get(getCountryWorld(world)).markDirty();
    }

    public static Country createCountry(
        World world,
        String name,
        UUID creatorUUID
    ) {
        World countryWorld = getCountryWorld(world);
        Map<UUID, Country> countries = getAllCountries(countryWorld);

        for (Country c : countries.values()) {
            if (c.getName().equalsIgnoreCase(name)) {
                throw new IllegalArgumentException(
                    "Country name already exists"
                );
            }
        }

        Country country = new Country(name, creatorUUID);
        countries.put(country.getId(), country);
        CountryStorage.get(countryWorld).markDirty();

        if (!countryWorld.isRemote) {
            EntityPlayerMP creator = countryWorld
                .getMinecraftServer()
                .getPlayerList()
                .getPlayerByUUID(creatorUUID);
            if (creator != null) {
                ItemStack mintBlock = new ItemStack(
                    Item.getItemFromBlock(ModBlocks.MINT)
                );
                if (!creator.inventory.addItemStackToInventory(mintBlock)) {
                    creator.dropItem(mintBlock, false);
                }
            }
        }

        return country;
    }

    public static Country getCountry(World world, UUID id) {
        return getAllCountries(world).get(id);
    }

    public static Country getCountryByName(World world, String name) {
        for (Country c : getAllCountries(world).values()) {
            if (c.getName().equalsIgnoreCase(name)) return c;
        }
        return null;
    }

    public static Country getCountryForPlayer(World world, UUID playerUUID) {
        for (Country country : getAllCountries(world).values()) {
            if (country.isMember(playerUUID)) {
                return country;
            }
        }
        return null;
    }
}
