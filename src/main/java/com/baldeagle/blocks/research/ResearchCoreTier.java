package com.baldeagle.blocks.research;

import com.baldeagle.items.ModItems;
import net.minecraft.item.Item;

public enum ResearchCoreTier {
    T1(ModItems.T1_CORE, 900L, "T1 Core"),
    T2(ModItems.T2_CORE, 8100L, "T2 Core"),
    T3(ModItems.T3_CORE, 72900L, "T3 Core"),
    T4(ModItems.T4_CORE, 656100L, "T4 Core"),
    T5(ModItems.T5_CORE, 5904900L, "T5 Core"),
    T6(ModItems.T6_CORE, 53144100L, "T6 Core"),
    T1_DEPOSIT(null, 900L, "Deposit");

    private final Item item;
    private final long cost;
    private final String label;

    ResearchCoreTier(Item item, long cost, String label) {
        this.item = item;
        this.cost = cost;
        this.label = label;
    }

    public Item getItem() {
        return item;
    }

    public long getCost() {
        return cost;
    }

    public String getLabel() {
        return label;
    }

    public static ResearchCoreTier fromOrdinal(int ordinal) {
        ResearchCoreTier[] values = values();
        if (ordinal < 0 || ordinal >= values.length) {
            return T1;
        }
        return values[ordinal];
    }
}
