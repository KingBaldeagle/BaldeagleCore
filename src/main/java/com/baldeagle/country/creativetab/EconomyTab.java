package com.baldeagle.country.creativetab;

import com.baldeagle.country.items.ModItems;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;

public class EconomyTab extends CreativeTabs {

    public static final EconomyTab INSTANCE = new EconomyTab();

    private EconomyTab() {
        super("economy"); // internal tab ID
    }

    @Override
    public ItemStack createIcon() {
        return new ItemStack(ModItems.COIN_1);
    }
}
