package com.baldeagle.blocks.shop;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class ContainerShop extends Container {

    private final TileEntityShop tile;
    private final EntityPlayer player;
    private final boolean ownerView;

    private static final int[] ROW_Y_OFFSETS = new int[] { 0, 2, 4 };
    private static final int SLOT_SPACING_X = 18;

    public ContainerShop(InventoryPlayer playerInventory, TileEntityShop tile) {
        this.tile = tile;
        this.player = playerInventory.player;
        this.ownerView = tile != null && tile.isOwner(player);

        // Shop slots
        for (int row = 0; row < 3; ++row) {
            int yOffset = ROW_Y_OFFSETS[row]; // <-- row-specific offset
            for (int col = 0; col < 3; ++col) {
                int index = col + row * 3;
                this.addSlotToContainer(
                    new Slot(
                        tile,
                        index,
                        62 + col * SLOT_SPACING_X,
                        17 + row * 18 + yOffset
                    ) {
                        @Override
                        public boolean isItemValid(ItemStack stack) {
                            return ownerView;
                        }

                        @Override
                        public boolean canTakeStack(EntityPlayer playerIn) {
                            return ownerView;
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

    public TileEntityShop getTile() {
        return tile;
    }

    public boolean isOwnerView() {
        return ownerView;
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return tile != null && tile.isUsableByPlayer(playerIn);
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = inventorySlots.get(index);

        int shopSlots = TileEntityShop.SLOT_COUNT;
        if (slot != null && slot.getHasStack()) {
            ItemStack stack = slot.getStack();
            itemstack = stack.copy();

            if (index < shopSlots) {
                if (
                    !mergeItemStack(
                        stack,
                        shopSlots,
                        inventorySlots.size(),
                        true
                    )
                ) {
                    return ItemStack.EMPTY;
                }
            } else if (ownerView) {
                if (!mergeItemStack(stack, 0, shopSlots, false)) {
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
