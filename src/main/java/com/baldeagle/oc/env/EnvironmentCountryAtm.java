package com.baldeagle.oc.env;

import com.baldeagle.country.Country;
import com.baldeagle.country.CountryStorage;
import com.baldeagle.country.currency.CurrencyDenomination;
import com.baldeagle.country.currency.CurrencyItemHelper;
import com.baldeagle.country.mint.MintingConstants;
import com.baldeagle.economy.EconomyManager;
import com.baldeagle.economy.atm.TileEntityAtm;
import com.baldeagle.oc.OCUtil;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class EnvironmentCountryAtm extends EnvironmentBase {

    private final TileEntityAtm tile;

    public EnvironmentCountryAtm(TileEntityAtm tile) {
        super("country_atm");
        this.tile = tile;
    }

    @Override
    protected World getWorld() {
        return tile != null ? tile.getWorld() : null;
    }

    @Callback(
        doc = "function(uuid:string):number|nil -- Returns player balance."
    )
    public Object[] getBalance(Context context, Arguments args) {
        try {
            World world = getWorld();
            if (world == null || world.isRemote) {
                return new Object[] { null };
            }

            UUID uuid = OCUtil.parseUuid(args.checkString(0));
            return new Object[] {
                EconomyManager.getPlayerBalance(world, uuid),
            };
        } catch (Throwable t) {
            return new Object[] { null };
        }
    }

    @Callback(
        doc = "function(uuid:string, amount:number):boolean,string|nil -- Withdraws currency."
    )
    public Object[] withdraw(Context context, Arguments args) {
        try {
            World world = getWorld();
            if (world == null || world.isRemote) {
                return new Object[] { false, "Server only." };
            }

            UUID uuid = OCUtil.parseUuid(args.checkString(0));
            long amount = args.checkLong(1);
            if (amount <= 0) {
                return new Object[] { false, "Amount must be > 0." };
            }

            EntityPlayerMP player = world
                .getMinecraftServer()
                .getPlayerList()
                .getPlayerByUUID(uuid);

            if (player == null) {
                return new Object[] { false, "Player must be online." };
            }

            OCUtil.requireCanInteract(context, player.getName());

            if (
                player.getDistanceSq(
                    tile.getPos().getX() + 0.5,
                    tile.getPos().getY() + 0.5,
                    tile.getPos().getZ() + 0.5
                ) >
                64
            ) {
                return new Object[] { false, "Too far from ATM." };
            }

            Country country = OCUtil.requireActorCountry(world, uuid);

            if (!EconomyManager.withdrawPlayer(world, uuid, amount)) {
                return new Object[] { false, "Insufficient funds." };
            }

            List<CurrencyDenomination> denoms = Arrays.stream(
                CurrencyDenomination.values()
            )
                .sorted(
                    Comparator.comparingInt(
                        CurrencyDenomination::getValue
                    ).reversed()
                )
                .collect(Collectors.toList());

            long remaining = amount;
            for (CurrencyDenomination denom : denoms) {
                long count = remaining / denom.getValue();
                while (count > 0) {
                    int maxStack =
                        denom.getType() ==
                        com.baldeagle.country.currency.CurrencyType.COIN
                            ? 64
                            : 16;

                    int give = (int) Math.min(count, maxStack);

                    ItemStack stack = CurrencyItemHelper.createCurrencyStack(
                        country,
                        denom,
                        give
                    );
                    if (!player.inventory.addItemStackToInventory(stack)) {
                        player.dropItem(stack, false);
                    }
                    count -= give;
                }
                remaining %= denom.getValue();
            }

            country.applyMinting(
                amount,
                MintingConstants.MINT_INFLATION_FACTOR
            );
            CountryStorage.get(world).markDirty();

            return new Object[] { true };
        } catch (Throwable t) {
            return new Object[] { false, t.getMessage() };
        }
    }
}
