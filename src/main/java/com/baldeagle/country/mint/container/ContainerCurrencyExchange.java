package com.baldeagle.country.mint.container;

import com.baldeagle.country.currency.CurrencyItemHelper;
import com.baldeagle.country.mint.tile.TileEntityCurrencyExchange;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class ContainerCurrencyExchange extends Container {

    private final TileEntityCurrencyExchange tile;
    private final EntityPlayer player;

    public ContainerCurrencyExchange(
        InventoryPlayer playerInventory,
        TileEntityCurrencyExchange tile
    ) {
        this.tile = tile;
        this.player = playerInventory.player;

        this.addSlotToContainer(
            new Slot(tile, 0, 80, 40) {
                @Override
                public boolean isItemValid(ItemStack stack) {
                    return CurrencyItemHelper.isCurrency(stack);
                }
            }
        );

        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlotToContainer(
                    new Slot(
                        playerInventory,
                        col + row * 9 + 9,
                        8 + col * 18,
                        84 + row * 18
                    )
                );
            }
        }

        for (int hotbar = 0; hotbar < 9; ++hotbar) {
            this.addSlotToContainer(
                new Slot(playerInventory, hotbar, 8 + hotbar * 18, 142)
            );
        }
    }

    public TileEntityCurrencyExchange getTile() {
        return tile;
    }

    @Override
    public void addListener(IContainerListener listener) {
        super.addListener(listener);
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return tile.isUsableByPlayer(playerIn);
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = inventorySlots.get(index);

        if (slot != null && slot.getHasStack()) {
            ItemStack stack = slot.getStack();
            itemstack = stack.copy();

            if (index == 0) {
                if (!mergeItemStack(stack, 1, inventorySlots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (CurrencyItemHelper.isCurrency(stack)) {
                if (!mergeItemStack(stack, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) {
                slot.putStack(ItemStack.EMPTY);
            } else {
                slot.onSlotChanged();
            }
        }

        return itemstack;
    }
}
