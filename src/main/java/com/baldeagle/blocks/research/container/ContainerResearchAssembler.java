package com.baldeagle.blocks.research.container;

import com.baldeagle.blocks.research.tile.TileEntityResearchAssembler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class ContainerResearchAssembler extends Container {

    private final TileEntityResearchAssembler tileEntity;
    private final EntityPlayer player;

    public ContainerResearchAssembler(
        InventoryPlayer inventory,
        TileEntityResearchAssembler tile
    ) {
        this.tileEntity = tile;
        this.player = inventory.player;

        this.addSlotToContainer(
            new Slot(tile, 0, 128, 33) {
                @Override
                public boolean isItemValid(ItemStack stack) {
                    return tile.isInputValid(stack);
                }
            }
        );

        this.addSlotToContainer(
            new Slot(tile, 1, 128, 60) {
                @Override
                public boolean isItemValid(ItemStack stack) {
                    return false;
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

    public TileEntityResearchAssembler getTileEntity() {
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
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.inventorySlots.get(index);

        if (slot != null && slot.getHasStack()) {
            ItemStack stack = slot.getStack();
            result = stack.copy();

            // From tile entity slots → player inventory
            if (index == 0 || index == 1) {
                if (
                    !this.mergeItemStack(
                        stack,
                        2,
                        this.inventorySlots.size(),
                        true
                    )
                ) {
                    return ItemStack.EMPTY;
                }
                slot.onSlotChange(stack, result);
            }
            // From player inventory → input slot
            else if (tileEntity.isInputValid(stack)) {
                if (!this.mergeItemStack(stack, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            }
            // Anything else: deny
            else {
                return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) {
                slot.putStack(ItemStack.EMPTY);
            } else {
                slot.onSlotChanged();
            }

            if (stack.getCount() == result.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(playerIn, stack);
        }

        return result;
    }
}
