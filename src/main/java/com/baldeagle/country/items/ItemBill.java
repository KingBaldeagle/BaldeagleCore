package com.baldeagle.country.items;

import net.minecraft.item.Item;

public class ItemBill extends Item {

    private final int value;

    public ItemBill(int value) {
        this.value = value;
        setMaxStackSize(16);
    }

    public int getValue() {
        return value;
    }
}
