package com.baldeagle.country.vault.container;

import com.baldeagle.country.vault.tile.TileEntityVault;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class ContainerVault extends Container {

    private final TileEntityVault tile;
    private final EntityPlayer player;

    public ContainerVault(
        InventoryPlayer playerInventory,
        TileEntityVault tile
    ) {
        this.tile = tile;
        this.player = playerInventory.player;

        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlotToContainer(
                    new Slot(tile, col + row * 9, 8 + col * 18, 18 + row * 18) {
                        @Override
                        public boolean isItemValid(ItemStack stack) {
                            return tile.isItemValidForSlot(
                                getSlotIndex(),
                                stack
                            );
                        }
                    }
                );
            }
        }

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

    public TileEntityVault getTile() {
        return tile;
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return tile.isUsableByPlayer(playerIn);
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = inventorySlots.get(index);

        int vaultSlots = 27;
        if (slot != null && slot.getHasStack()) {
            ItemStack stack = slot.getStack();
            itemstack = stack.copy();

            if (index < vaultSlots) {
                if (
                    !mergeItemStack(
                        stack,
                        vaultSlots,
                        inventorySlots.size(),
                        true
                    )
                ) {
                    return ItemStack.EMPTY;
                }
            } else if (tile.isItemValidForSlot(0, stack)) {
                if (!mergeItemStack(stack, 0, vaultSlots, false)) {
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
