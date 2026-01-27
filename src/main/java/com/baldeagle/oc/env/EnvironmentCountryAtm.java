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
        return tile.getWorld();
    }

    @Callback(
        doc = "function(uuid:string):number -- Returns the player's balance."
    )
    public Object[] getBalance(Context context, Arguments args) {
        try {
            World world = getWorld();
            UUID playerUuid = OCUtil.parseUuid(args.checkString(0));
            if (world == null || world.isRemote) {
                return new Object[] { null, "Server only." };
            }
            EntityPlayerMP player = world
                .getMinecraftServer()
                .getPlayerList()
                .getPlayerByUUID(playerUuid);
            if (player == null) {
                return new Object[] { null, "Player must be online." };
            }
            OCUtil.requireCanInteract(context, player.getName());
            long balance = EconomyManager.getPlayerBalance(world, playerUuid);
            return new Object[] { balance };
        } catch (Exception e) {
            return new Object[] { null, e.getMessage() };
        }
    }

    @Callback(
        doc = "function(uuid:string, amount:number):boolean,string|nil -- Withdraws physical currency into the player's inventory."
    )
    public Object[] withdraw(Context context, Arguments args) {
        try {
            World world = getWorld();
            if (world == null || world.isRemote) {
                return new Object[] { false, "Server only." };
            }

            UUID playerUuid = OCUtil.parseUuid(args.checkString(0));
            long amount = Math.max(0L, args.checkLong(1));
            if (amount <= 0L) {
                return new Object[] { false, "Amount must be > 0." };
            }

            EntityPlayerMP player = world
                .getMinecraftServer()
                .getPlayerList()
                .getPlayerByUUID(playerUuid);
            if (player == null) {
                return new Object[] { false, "Player must be online." };
            }

            String playerName = player.getName();
            OCUtil.requireCanInteract(context, playerName);

            if (
                tile.getPos() != null &&
                player.getDistanceSq(
                    tile.getPos().getX() + 0.5,
                    tile.getPos().getY() + 0.5,
                    tile.getPos().getZ() + 0.5
                ) >
                64
            ) {
                return new Object[] { false, "Player too far from ATM." };
            }

            Country country = OCUtil.requireActorCountry(world, playerUuid);

            boolean success = EconomyManager.withdrawPlayer(
                world,
                playerUuid,
                amount
            );
            if (!success) {
                return new Object[] { false, "Insufficient funds." };
            }

            long remaining = amount;
            List<CurrencyDenomination> denominations = Arrays.stream(
                CurrencyDenomination.values()
            )
                .sorted(
                    Comparator.comparingInt(
                        CurrencyDenomination::getValue
                    ).reversed()
                )
                .collect(Collectors.toList());

            for (CurrencyDenomination denom : denominations) {
                if (denom == null) continue;
                long count = remaining / denom.getValue();
                if (count <= 0) continue;

                int maxStack =
                    denom.getType() ==
                    com.baldeagle.country.currency.CurrencyType.COIN
                        ? 64
                        : 16;
                while (count > 0) {
                    int give = (int) Math.min(count, (long) maxStack);
                    ItemStack stack = CurrencyItemHelper.createCurrencyStack(
                        country,
                        denom,
                        give
                    );
                    if (!stack.isEmpty()) {
                        if (!player.inventory.addItemStackToInventory(stack)) {
                            player.dropItem(stack, false);
                        }
                    }
                    count -= give;
                }

                remaining = remaining % denom.getValue();
                if (remaining <= 0) break;
            }

            country.applyMinting(
                amount,
                MintingConstants.MINT_INFLATION_FACTOR
            );
            CountryStorage.get(world).markDirty();
            return new Object[] { true };
        } catch (Exception e) {
            return new Object[] { false, e.getMessage() };
        }
    }
}
