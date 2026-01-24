package com.baldeagle.country.mint.container;

import com.baldeagle.country.mint.tile.TileEntityMint;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class ContainerMint extends Container {

    private final TileEntityMint tileEntity;
    private final EntityPlayer player;

    public ContainerMint(InventoryPlayer inventory, TileEntityMint tile) {
        this.tileEntity = tile;
        this.player = inventory.player;

        this.addSlotToContainer(
            new Slot(tile, 0, 128, 33) {
                @Override
                public boolean isItemValid(ItemStack stack) {
                    return (
                        !stack.isEmpty() && stack.getItem() == Items.GOLD_INGOT
                    );
                }
            }
        );

        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlotToContainer(
                    new Slot(
                        inventory,
                        col + row * 9 + 9,
                        8 + col * 18,
                        84 + row * 18
                    )
                );
            }
        }

        for (int hotbar = 0; hotbar < 9; ++hotbar) {
            this.addSlotToContainer(
                new Slot(inventory, hotbar, 8 + hotbar * 18, 142)
            );
        }
    }

    public TileEntityMint getTileEntity() {
        return tileEntity;
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return (
            tileEntity != null &&
            playerIn.getDistanceSq(
                tileEntity.getPos().getX() + 0.5,
                tileEntity.getPos().getY() + 0.5,
                tileEntity.getPos().getZ() + 0.5
            ) <=
            64
        );
    }

    public EntityPlayer getPlayer() {
        return player;
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
            } else if (stack.getItem() == Items.GOLD_INGOT) {
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
