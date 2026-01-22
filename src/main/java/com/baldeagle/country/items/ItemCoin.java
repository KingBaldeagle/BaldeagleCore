package com.baldeagle.country.items;

import com.baldeagle.country.Country;
import com.baldeagle.country.currency.CurrencyDenomination;
import com.baldeagle.country.currency.CurrencyItemHelper;
import com.baldeagle.country.currency.CurrencyType;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemCoin extends Item {

    private final CurrencyDenomination denomination;

    public ItemCoin() {
        this(CurrencyDenomination.COIN_1);
    }

    public ItemCoin(CurrencyDenomination denomination) {
        this.denomination = denomination;
        setMaxStackSize(64);
    }

    public CurrencyDenomination getDenomination() {
        return denomination;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(
        ItemStack stack,
        @Nullable World world,
        List<String> tooltip,
        ITooltipFlag flag
    ) {
        CurrencyDenomination denomination = CurrencyItemHelper.getDenomination(
            stack
        );
        tooltip.add(
            TextFormatting.GRAY +
                I18n.format(
                    "tooltip.baldeaglecore.denomination",
                    denomination != null ? denomination.getValue() : "?"
                )
        );

        if (world != null) {
            Country country = CurrencyItemHelper.getCountry(world, stack);
            if (country != null) {
                tooltip.add(TextFormatting.GOLD + country.getName());
                tooltip.add(
                    TextFormatting.RED +
                        I18n.format(
                            "tooltip.baldeaglecore.inflation",
                            String.format("%.3f", country.getInflation())
                        )
                );
            } else {
                tooltip.add(
                    TextFormatting.DARK_GRAY +
                        I18n.format("tooltip.baldeaglecore.unbound")
                );
            }
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public String getItemStackDisplayName(ItemStack stack) {
        CurrencyDenomination denomination = CurrencyItemHelper.getDenomination(
            stack
        );
        NBTTagCompound tag = stack.getTagCompound();
        CurrencyType type = CurrencyType.COIN;
        String denominationText =
            denomination != null
                ? Integer.toString(denomination.getValue())
                : "?";
        String countryName = "Generic";
        if (tag != null && tag.hasKey(CurrencyItemHelper.NBT_COUNTRY)) {
            try {
                UUID id = UUID.fromString(
                    tag.getString(CurrencyItemHelper.NBT_COUNTRY)
                );
                countryName = id.toString().substring(0, 8);
            } catch (IllegalArgumentException ignored) {}
        }
        return I18n.format(
            "item.baldeaglecore.currency_coin",
            denominationText,
            countryName,
            type.getNbtKey()
        );
    }
}
