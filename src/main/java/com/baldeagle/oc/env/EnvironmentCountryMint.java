package com.baldeagle.oc.env;

import com.baldeagle.country.Country;
import com.baldeagle.country.CountryManager;
import com.baldeagle.country.mint.tile.TileEntityMint;
import com.baldeagle.oc.OCUtil;
import java.util.UUID;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;

public class EnvironmentCountryMint extends EnvironmentBase {

    private final TileEntityMint tile;

    public EnvironmentCountryMint(TileEntityMint tile) {
        super("country_mint");
        this.tile = tile;
    }

    @Override
    protected World getWorld() {
        return tile != null ? tile.getWorld() : null;
    }

    @Callback(
        doc = "function(amount:number):boolean,string|nil -- Mints physical currency."
    )
    public Object[] mint(Context context, Arguments args) {
        try {
            World world = getWorld();
            if (world == null || world.isRemote) {
                return new Object[] { false, "Server only." };
            }

            UUID actor = OCUtil.resolveActorUuid(context, world);
            if (actor == null) {
                return new Object[] { false, "No OC player bound." };
            }

            EntityPlayerMP player = world
                .getMinecraftServer()
                .getPlayerList()
                .getPlayerByUUID(actor);

            if (player == null) {
                return new Object[] { false, "Player must be online." };
            }

            Country country = tile.getCountry();
            if (country == null) {
                Country actorCountry = CountryManager.getCountryForPlayer(
                    world,
                    actor
                );
                if (actorCountry == null) {
                    return new Object[] { false, "Join a country first." };
                }
                tile.setCountryId(actorCountry.getId());
                country = actorCountry;
            }

            OCUtil.requireAuthorized(country, actor);

            long amount = args.checkLong(0);
            if (amount <= 0) {
                return new Object[] { false, "Amount must be > 0." };
            }

            if (!tile.performMintByValue(player, amount)) {
                return new Object[] { false, "Minting failed." };
            }

            return new Object[] { true };
        } catch (Throwable t) {
            return new Object[] { false, t.getMessage() };
        }
    }

    @Callback(doc = "function():number|nil -- Returns treasury balance.")
    public Object[] getCountryBalance(Context context, Arguments args) {
        try {
            Country country = tile.getCountry();
            return new Object[] {
                country != null ? country.getBalance() : null,
            };
        } catch (Throwable t) {
            return new Object[] { null };
        }
    }
}
