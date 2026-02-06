package com.baldeagle.blocks.vault;

import com.baldeagle.blocks.vault.tile.TileEntityVault;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public final class VaultManager {

    private VaultManager() {}

    public static int getAvailableGold(World world, UUID countryId) {
        if (world == null || countryId == null) {
            return 0;
        }
        int total = 0;
        for (TileEntityVault vault : getLoadedVaults(world, countryId)) {
            total += vault.getGoldCount();
        }
        return total;
    }

    public static boolean consumeGold(World world, UUID countryId, int amount) {
        if (world == null || countryId == null || amount <= 0) {
            return false;
        }

        int remaining = amount;
        for (TileEntityVault vault : getLoadedVaults(world, countryId)) {
            if (remaining <= 0) break;
            remaining -= vault.consumeGold(remaining);
        }
        return remaining <= 0;
    }

    private static List<TileEntityVault> getLoadedVaults(
        World world,
        UUID countryId
    ) {
        List<TileEntityVault> result = new ArrayList<>();
        if (world == null || countryId == null) {
            return result;
        }
        for (TileEntity tile : new ArrayList<>(world.loadedTileEntityList)) {
            if (tile instanceof TileEntityVault) {
                TileEntityVault vault = (TileEntityVault) tile;
                if (countryId.equals(vault.getCountryId())) {
                    result.add(vault);
                }
            }
        }
        return result;
    }
}
