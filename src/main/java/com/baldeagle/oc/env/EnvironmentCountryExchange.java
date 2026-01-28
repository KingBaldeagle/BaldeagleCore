package com.baldeagle.oc.env;

import com.baldeagle.country.Country;
import com.baldeagle.country.CountryManager;
import com.baldeagle.country.mint.CurrencyMath;
import com.baldeagle.country.mint.tile.TileEntityCurrencyExchange;
import com.baldeagle.oc.OCUtil;
import java.util.UUID;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import net.minecraft.world.World;

public class EnvironmentCountryExchange extends EnvironmentBase {

    private final TileEntityCurrencyExchange tile;

    public EnvironmentCountryExchange(TileEntityCurrencyExchange tile) {
        super("baldeagle_exchange");
        this.tile = tile;
    }

    @Override
    protected World getWorld() {
        return tile != null ? tile.getWorld() : null;
    }

    /* --------------------------------------------------------------------- */
    /* Internal helpers                                                       */
    /* --------------------------------------------------------------------- */

    private Object[] error(String msg) {
        return new Object[] { null, msg };
    }

    private Object[] ok(long value) {
        return new Object[] { value, null };
    }

    private Object[] ok(double value) {
        return new Object[] { value, null };
    }

    private double computeFinalRate(
        Country source,
        Country target,
        long amount
    ) {
        double baseRate = CurrencyMath.computeExchangeRate(source, target);
        if (baseRate <= 0D) return 0D;

        double liquidity = CurrencyMath.computeLiquidityMultiplier(
            source,
            amount
        );

        double fee = source.getExchangeFee();
        double feeMultiplier = 1.0D - Math.max(0.0D, Math.min(0.99D, fee));

        return baseRate * liquidity * feeMultiplier;
    }

    /* --------------------------------------------------------------------- */
    /* OC callbacks                                                           */
    /* --------------------------------------------------------------------- */

    @Callback(
        doc = "function(amount:number, targetCountry:string):number|nil,string|nil " +
            "-- Returns the final exchange rate for a given amount (includes fee & liquidity)."
    )
    public Object[] getExchangeRate(Context context, Arguments args) {
        try {
            World world = getWorld();
            if (world == null) {
                return error("World not available.");
            }

            UUID actor = OCUtil.resolveActorUuid(context, world);
            Country source = OCUtil.requireActorCountry(world, actor);
            OCUtil.requireAuthorized(source, actor);

            long amount = Math.max(0L, args.checkLong(0));
            if (amount <= 0L) {
                return ok(0D);
            }

            String targetName = args.checkString(1);
            Country target = CountryManager.getCountryByName(world, targetName);
            if (target == null) {
                return error("Target country not found.");
            }

            double rate = computeFinalRate(source, target, amount);
            return ok(rate);
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }

    @Callback(
        doc = "function(amount:number, targetCountry:string):number|nil,string|nil " +
            "-- Converts the amount using current rate (includes fee & liquidity)."
    )
    public Object[] convertCurrency(Context context, Arguments args) {
        try {
            World world = getWorld();
            if (world == null) {
                return error("World not available.");
            }

            UUID actor = OCUtil.resolveActorUuid(context, world);
            Country source = OCUtil.requireActorCountry(world, actor);
            OCUtil.requireAuthorized(source, actor);

            long amount = Math.max(0L, args.checkLong(0));
            if (amount <= 0L) {
                return ok(0L);
            }

            String targetName = args.checkString(1);
            Country target = CountryManager.getCountryByName(world, targetName);
            if (target == null) {
                return error("Target country not found.");
            }

            double rate = computeFinalRate(source, target, amount);
            long output = (long) Math.floor(amount * rate);

            return ok(Math.max(0L, output));
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }
}
