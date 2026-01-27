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
        super("country_exchange");
        this.tile = tile;
    }

    @Override
    protected World getWorld() {
        return tile.getWorld();
    }

    @Callback(doc = "function(targetCountry:string):number -- Returns the current exchange rate (including fee and liquidity for 1 unit).")
    public Object[] getExchangeRate(Context context, Arguments args) {
        try {
            World world = getWorld();
            UUID actor = OCUtil.resolveActorUuid(context, world);
            Country source = OCUtil.requireActorCountry(world, actor);

            String targetName = args.checkString(0);
            Country target = CountryManager.getCountryByName(world, targetName);
            if (target == null) {
                return new Object[] { null, "Target country not found." };
            }

            double rate = CurrencyMath.computeExchangeRate(source, target);
            if (rate <= 0D) {
                return new Object[] { 0D };
            }
            double liquidity = CurrencyMath.computeLiquidityMultiplier(source, 1);
            double fee = source.getExchangeFee();
            double feeMultiplier = 1.0D - Math.max(0.0D, Math.min(0.99D, fee));
            return new Object[] { rate * liquidity * feeMultiplier };
        } catch (Exception e) {
            return new Object[] { null, e.getMessage() };
        }
    }

    @Callback(doc = "function(amount:number, targetCountry:string):number -- Converts the amount using current rate (including fee and liquidity).")
    public Object[] convertCurrency(Context context, Arguments args) {
        try {
            World world = getWorld();
            UUID actor = OCUtil.resolveActorUuid(context, world);
            Country source = OCUtil.requireActorCountry(world, actor);

            long amount = Math.max(0L, args.checkLong(0));
            if (amount <= 0L) {
                return new Object[] { 0L };
            }

            String targetName = args.checkString(1);
            Country target = CountryManager.getCountryByName(world, targetName);
            if (target == null) {
                return new Object[] { null, "Target country not found." };
            }

            double rate = CurrencyMath.computeExchangeRate(source, target);
            if (rate <= 0D) {
                return new Object[] { 0L };
            }
            double liquidity = CurrencyMath.computeLiquidityMultiplier(source, amount);
            double fee = source.getExchangeFee();
            double feeMultiplier = 1.0D - Math.max(0.0D, Math.min(0.99D, fee));
            double finalRate = rate * liquidity * feeMultiplier;
            long output = (long) Math.floor(amount * finalRate);
            return new Object[] { Math.max(0L, output) };
        } catch (Exception e) {
            return new Object[] { null, e.getMessage() };
        }
    }
}
