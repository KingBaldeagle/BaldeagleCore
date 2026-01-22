package com.baldeagle.country;

import com.baldeagle.bank.ModBlocks;
import java.util.Map;
import java.util.UUID;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class CountryManager {

    public static Map<UUID, Country> getAllCountries(World world) {
        return CountryStorage.get(world).getCountriesMap();
    }

    public static void clear(World world) {
        getAllCountries(world).clear();
        CountryStorage.get(world).markDirty();
    }

    public static Country createCountry(
        World world,
        String name,
        UUID creatorUUID
    ) {
        Map<UUID, Country> countries = getAllCountries(world);

        for (Country c : countries.values()) {
            if (c.getName().equalsIgnoreCase(name)) {
                throw new IllegalArgumentException(
                    "Country name already exists"
                );
            }
        }

        Country country = new Country(name, creatorUUID);
        countries.put(country.getId(), country);
        CountryStorage.get(world).markDirty();

        if (!world.isRemote) {
            EntityPlayerMP creator = world
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
