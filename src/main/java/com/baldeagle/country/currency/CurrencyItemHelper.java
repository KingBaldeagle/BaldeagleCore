package com.baldeagle.country.currency;

import com.baldeagle.country.Country;
import com.baldeagle.country.CountryManager;
import com.baldeagle.country.CountryStorage;
import com.baldeagle.country.items.ItemBill;
import com.baldeagle.country.items.ItemCoin;
import com.baldeagle.country.items.ModItems;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

public final class CurrencyItemHelper {

    public static final String NBT_COUNTRY = "country_id";
    public static final String NBT_DENOMINATION = "denomination";
    public static final String NBT_TYPE = "type";

    private CurrencyItemHelper() {}

    public static boolean isCurrency(ItemStack stack) {
        return (
            !stack.isEmpty() &&
            (stack.getItem() instanceof ItemCoin ||
                stack.getItem() instanceof ItemBill)
        );
    }

    public static ItemStack createCurrencyStack(
        Country country,
        CurrencyDenomination denomination,
        int amount
    ) {
        if (country == null || denomination == null || amount <= 0) {
            return ItemStack.EMPTY;
        }

        net.minecraft.item.Item item = ModItems.getCurrencyItem(denomination);
        if (item == null) {
            item =
                denomination.getType() == CurrencyType.COIN
                    ? ModItems.COIN_1
                    : ModItems.BILL_50;
        }
        ItemStack stack = new ItemStack(item, amount);

        applyCurrencyData(stack, country.getId(), denomination);
        return stack;
    }

    public static void applyCurrencyData(
        ItemStack stack,
        UUID countryId,
        CurrencyDenomination denomination
    ) {
        if (stack.isEmpty() || countryId == null || denomination == null) {
            return;
        }
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        tag.setString(NBT_COUNTRY, countryId.toString());
        tag.setString(NBT_DENOMINATION, denomination.getId());
        tag.setString(NBT_TYPE, denomination.getType().getNbtKey());
    }

    public static UUID getCountryId(ItemStack stack) {
        if (
            stack.isEmpty() ||
            stack.getTagCompound() == null ||
            !stack.getTagCompound().hasKey(NBT_COUNTRY)
        ) {
            return null;
        }
        try {
            return UUID.fromString(
                stack.getTagCompound().getString(NBT_COUNTRY)
            );
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public static Country getCountry(World world, ItemStack stack) {
        UUID id = getCountryId(stack);
        return id == null ? null : CountryManager.getCountry(world, id);
    }

    public static CurrencyDenomination getDenomination(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        if (
            stack.getTagCompound() != null &&
            stack.getTagCompound().hasKey(NBT_DENOMINATION)
        ) {
            CurrencyDenomination denomination = CurrencyDenomination.fromId(
                stack.getTagCompound().getString(NBT_DENOMINATION)
            );
            if (denomination != null) {
                return denomination;
            }
        }
        if (stack.getItem() instanceof ItemCoin) {
            return ((ItemCoin) stack.getItem()).getDenomination();
        }
        if (stack.getItem() instanceof ItemBill) {
            return ((ItemBill) stack.getItem()).getDenomination();
        }
        return null;
    }

    public static CurrencyType getType(ItemStack stack) {
        CurrencyDenomination denomination = getDenomination(stack);
        return denomination != null ? denomination.getType() : null;
    }

    public static long getFaceValue(ItemStack stack) {
        CurrencyDenomination denomination = getDenomination(stack);
        if (denomination == null) {
            return 0;
        }
        return (long) denomination.getValue() * stack.getCount();
    }

    public static double getStackMonetaryValue(World world, ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        CurrencyDenomination denomination = getDenomination(stack);
        if (denomination == null) {
            return 0;
        }
        Country country = getCountry(world, stack);
        double baseValue = denomination.getValue();
        if (country == null) {
            return baseValue * stack.getCount();
        }
        return (baseValue / country.getInflation()) * stack.getCount();
    }

    public static void removeFromCirculation(World world, ItemStack stack) {
        if (world == null || stack.isEmpty()) {
            return;
        }
        Country country = getCountry(world, stack);
        if (country == null) {
            return;
        }
        long faceValue = getFaceValue(stack);
        if (faceValue > 0) {
            country.removeFromCirculation(faceValue);
            CountryStorage.get(world).markDirty();
        }
    }

    public static boolean enforceCountryMatch(
        World world,
        ItemStack stack,
        Country targetCountry,
        EntityPlayer player
    ) {
        Country currencyCountry = getCountry(world, stack);
        if (currencyCountry == null || targetCountry == null) {
            return false;
        }
        if (!Objects.equals(currencyCountry.getId(), targetCountry.getId())) {
            if (player != null) {
                player.sendStatusMessage(
                    new TextComponentString(
                        "This currency belongs to " +
                            currencyCountry.getName() +
                            "."
                    ),
                    true
                );
            }
            return false;
        }
        return true;
    }
}
