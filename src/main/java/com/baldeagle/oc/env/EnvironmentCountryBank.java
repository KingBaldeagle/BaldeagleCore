package com.baldeagle.oc.env;

import com.baldeagle.blocks.bank.TileEntityBank;
import com.baldeagle.country.Country;
import com.baldeagle.country.CountryStorage;
import com.baldeagle.economy.EconomyManager;
import com.baldeagle.oc.OCUtil;
import java.util.UUID;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import net.minecraft.world.World;

public class EnvironmentCountryBank extends EnvironmentBase {

    private static final double WIRE_FEE = 0.05D;

    private final TileEntityBank tile;

    public EnvironmentCountryBank(TileEntityBank tile) {
        super("country_bank");
        this.tile = tile;
    }

    @Override
    protected World getWorld() {
        return tile.getWorld();
    }

    @Callback(
        doc = "function():number -- Returns the country treasury balance for the first authorized computer user."
    )
    public Object[] getBalance(Context context, Arguments args) {
        try {
            World world = getWorld();
            UUID actor = OCUtil.resolveActorUuid(context, world);
            Country country = OCUtil.requireActorCountry(world, actor);
            OCUtil.requireAuthorized(country, actor);
            return new Object[] { country.getBalance() };
        } catch (Exception e) {
            return new Object[] { null, e.getMessage() };
        }
    }

    @Callback(
        doc = "function(uuid:string, amount:number):boolean,string|nil -- Transfers funds from country treasury into the target player's balance."
    )
    public Object[] depositPlayer(Context context, Arguments args) {
        try {
            World world = getWorld();
            UUID actor = OCUtil.resolveActorUuid(context, world);
            Country country = OCUtil.requireActorCountry(world, actor);
            OCUtil.requireAuthorized(country, actor);

            UUID target = OCUtil.parseUuid(args.checkString(0));
            long amount = Math.max(0L, args.checkLong(1));
            if (amount <= 0L) {
                return new Object[] { false, "Amount must be > 0." };
            }

            boolean ok = EconomyManager.withdrawCountry(
                world,
                country.getName(),
                amount
            );
            if (!ok) {
                return new Object[] { false, "Insufficient country funds." };
            }

            EconomyManager.depositPlayer(world, target, amount);
            CountryStorage.get(world).markDirty();
            return new Object[] { true };
        } catch (Exception e) {
            return new Object[] { false, e.getMessage() };
        }
    }

    @Callback(
        doc = "function(uuid:string, amount:number):boolean,string|nil -- Withdraws funds from the target player's balance into the country treasury."
    )
    public Object[] withdrawPlayer(Context context, Arguments args) {
        try {
            World world = getWorld();
            UUID actor = OCUtil.resolveActorUuid(context, world);
            Country country = OCUtil.requireActorCountry(world, actor);
            OCUtil.requireAuthorized(country, actor);

            UUID target = OCUtil.parseUuid(args.checkString(0));
            long amount = Math.max(0L, args.checkLong(1));
            if (amount <= 0L) {
                return new Object[] { false, "Amount must be > 0." };
            }

            boolean ok = EconomyManager.withdrawPlayer(world, target, amount);
            if (!ok) {
                return new Object[] { false, "Insufficient player funds." };
            }

            EconomyManager.depositCountry(world, country.getName(), amount);
            CountryStorage.get(world).markDirty();
            return new Object[] { true };
        } catch (Exception e) {
            return new Object[] { false, e.getMessage() };
        }
    }

    @Callback(
        doc = "function(senderUUID:string, receiverUUID:string, amount:number):boolean,string|nil -- Transfers funds between players with a fee routed to the sender's country treasury."
    )
    public Object[] wireTransfer(Context context, Arguments args) {
        try {
            World world = getWorld();
            UUID actor = OCUtil.resolveActorUuid(context, world);
            Country country = OCUtil.requireActorCountry(world, actor);
            OCUtil.requireAuthorized(country, actor);

            UUID sender = OCUtil.parseUuid(args.checkString(0));
            UUID receiver = OCUtil.parseUuid(args.checkString(1));
            if (!sender.equals(actor)) {
                return new Object[] {
                    false,
                    "Sender must be a computer user.",
                };
            }
            long amount = Math.max(0L, args.checkLong(2));
            if (amount <= 0L) {
                return new Object[] { false, "Amount must be > 0." };
            }

            boolean ok = EconomyManager.withdrawPlayer(world, sender, amount);
            if (!ok) {
                return new Object[] { false, "Insufficient sender funds." };
            }

            long fee = (long) Math.floor(amount * WIRE_FEE);
            long net = Math.max(0L, amount - fee);
            if (net > 0L) {
                EconomyManager.depositPlayer(world, receiver, net);
            }
            if (fee > 0L) {
                EconomyManager.depositCountry(world, country.getName(), fee);
                CountryStorage.get(world).markDirty();
            }

            return new Object[] { true };
        } catch (Exception e) {
            return new Object[] { false, e.getMessage() };
        }
    }

    @Callback(doc = "function():number -- Returns the wire transfer fee rate.")
    public Object[] getWireFeeRate(Context context, Arguments args) {
        return new Object[] { WIRE_FEE };
    }
}
