package com.baldeagle.country.creativetab;

import com.baldeagle.country.items.ModItems;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;

public class BaldeagleCoreTab extends CreativeTabs {

    public static final BaldeagleCoreTab INSTANCE = new BaldeagleCoreTab();

    private BaldeagleCoreTab() {
        super("baldeaglecore"); // internal tab ID
    }

    @Override
    public ItemStack createIcon() {
        return new ItemStack(ModItems.COIN_1);
    }
}
