package com.baldeagle.oc.env;

import com.baldeagle.country.Country;
import com.baldeagle.country.CountryManager;
import com.baldeagle.country.CountryStorage;
import com.baldeagle.economy.EconomyManager;
import com.baldeagle.oc.OCUtil;
import com.baldeagle.oc.gov.TileEntityGovernmentComputer;
import java.util.Map;
import java.util.UUID;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import net.minecraft.world.World;

public class EnvironmentCountryGov extends EnvironmentBase {

    private final TileEntityGovernmentComputer tile;

    public EnvironmentCountryGov(TileEntityGovernmentComputer tile) {
        super("country_gov");
        this.tile = tile;
    }

    @Override
    protected World getWorld() {
        return tile.getWorld();
    }

    private Country requireBoundCountry() {
        World world = getWorld();
        if (world == null) {
            throw new IllegalArgumentException("World unavailable.");
        }
        UUID id = tile.getCountryId();
        if (id == null) {
            throw new IllegalArgumentException("Government computer is unbound.");
        }
        Country country = CountryManager.getCountry(world, id);
        if (country == null) {
            throw new IllegalArgumentException("Country not found.");
        }
        return country;
    }

    @Callback(doc = "function(role:string, amount:number):boolean,string|nil -- Pays salary to all members with that role (from country treasury).")
    public Object[] paySalary(Context context, Arguments args) {
        try {
            World world = getWorld();
            UUID actor = OCUtil.resolveActorUuid(context, world);
            Country country = requireBoundCountry();
            OCUtil.requireAuthorized(country, actor);

            String roleName = args.checkString(0);
            long amount = Math.max(0L, args.checkLong(1));
            if (amount <= 0L) {
                return new Object[] { false, "Amount must be > 0." };
            }

            Country.Role role;
            try {
                role = Country.Role.valueOf(roleName.trim().toUpperCase());
            } catch (Exception e) {
                return new Object[] { false, "Invalid role." };
            }

            long count = 0L;
            for (Map.Entry<UUID, Country.Role> entry : country.getMembers().entrySet()) {
                if (entry.getValue() == role) {
                    count++;
                }
            }
            if (count <= 0L) {
                return new Object[] { true, 0L };
            }

            long total = amount * count;
            if (country.getBalance() < total) {
                return new Object[] { false, "Insufficient country funds." };
            }
            boolean ok = EconomyManager.withdrawCountry(
                world,
                country.getName(),
                total
            );
            if (!ok) {
                return new Object[] { false, "Insufficient country funds." };
            }

            country.setBalance(country.getBalance() - total);

            for (Map.Entry<UUID, Country.Role> entry : country.getMembers().entrySet()) {
                if (entry.getValue() != role) {
                    continue;
                }
                EconomyManager.depositPlayer(world, entry.getKey(), amount);
            }

            CountryStorage.get(world).markDirty();
            return new Object[] { true, count };
        } catch (Exception e) {
            return new Object[] { false, e.getMessage() };
        }
    }

    @Callback(doc = "function():number|nil -- Returns the bound country's inflation index.")
    public Object[] getInflation(Context context, Arguments args) {
        try {
            Country country = requireBoundCountry();
            return new Object[] { country.getInflation() };
        } catch (Exception e) {
            return new Object[] { null };
        }
    }

    @Callback(doc = "function():number|nil -- Returns the bound country's reserve value.")
    public Object[] getReserves(Context context, Arguments args) {
        try {
            Country country = requireBoundCountry();
            return new Object[] { country.getTreasury() };
        } catch (Exception e) {
            return new Object[] { null };
        }
    }
}
