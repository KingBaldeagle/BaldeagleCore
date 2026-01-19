package com.baldeagle.country.items;

import net.minecraft.item.Item;

public class ItemCoin extends Item {

    private final int value;

    public ItemCoin(int value) {
        this.value = value;
        setMaxStackSize(64);
    }

    public int getValue() {
        return value;
    }
}
