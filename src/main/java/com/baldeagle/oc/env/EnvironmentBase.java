package com.baldeagle.oc.env;

import com.baldeagle.country.Country;
import com.baldeagle.country.CountryManager;
import com.baldeagle.oc.OCUtil;
import java.util.UUID;
import li.cil.oc.api.Network;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import net.minecraft.world.World;

public abstract class EnvironmentBase extends AbstractManagedEnvironment {

    protected abstract World getWorld();

    protected EnvironmentBase(String componentName) {
        setNode(
            Network
                .newNode(this, Visibility.Network)
                .withComponent(componentName)
                .create()
        );
    }

    @Callback(doc = "function(uuid:string):string|nil -- Returns the player's country name (or nil).")
    public Object[] getPlayerCountry(Context context, Arguments args) {
        try {
            World world = getWorld();
            UUID uuid = OCUtil.parseUuid(args.checkString(0));
            Country country = CountryManager.getCountryForPlayer(world, uuid);
            return new Object[] { country != null ? country.getName() : null };
        } catch (Exception e) {
            return new Object[] { null };
        }
    }

    @Callback(doc = "function(uuid:string):string|nil -- Returns the player's role in their country (or nil).")
    public Object[] getPlayerRole(Context context, Arguments args) {
        try {
            World world = getWorld();
            UUID uuid = OCUtil.parseUuid(args.checkString(0));
            Country country = CountryManager.getCountryForPlayer(world, uuid);
            if (country == null) {
                return new Object[] { null };
            }
            Country.Role role = country.getRole(uuid);
            return new Object[] {
                role != null ? role.name().toLowerCase() : null,
            };
        } catch (Exception e) {
            return new Object[] { null };
        }
    }
}
