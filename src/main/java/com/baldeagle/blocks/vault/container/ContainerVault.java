package com.baldeagle.blocks.vault.container;

import com.baldeagle.blocks.vault.tile.TileEntityVault;
import com.baldeagle.country.Country;
import com.baldeagle.country.CountryManager;
import com.baldeagle.network.NetworkHandler;
import com.baldeagle.network.message.VaultSyncMessage;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class ContainerVault extends Container {

    private final TileEntityVault tile;
    private final EntityPlayer player;
    private long cachedCountryReserves = Long.MIN_VALUE;

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
    public void addListener(IContainerListener listener) {
        super.addListener(listener);
        syncCountryReserves();
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        syncCountryReserves();
    }

    private void syncCountryReserves() {
        if (player.world.isRemote) {
            return;
        }
        if (!(player instanceof net.minecraft.entity.player.EntityPlayerMP)) {
            return;
        }
        if (tile == null || tile.getCountryId() == null) {
            return;
        }
        Country country = CountryManager.getCountry(
            player.world,
            tile.getCountryId()
        );
        if (country == null) {
            return;
        }
        long reserves = country.getTreasury();
        // Also check tile's computed value to catch desyncs
        long tileReserves = tile.getReserveUnits();
        if (reserves != tileReserves) {
            // Force update if tile and country are out of sync
            country.setTreasury(tileReserves);
            reserves = tileReserves;
        }
        if (reserves == cachedCountryReserves) {
            return;
        }
        cachedCountryReserves = reserves;
        NetworkHandler.INSTANCE.sendTo(
            new VaultSyncMessage(tile.getPos(), reserves),
            (net.minecraft.entity.player.EntityPlayerMP) player
        );
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
            
            // Force sync when vault contents change
            if (index < vaultSlots || (index >= vaultSlots && tile.isItemValidForSlot(0, itemstack))) {
                cachedCountryReserves = Long.MIN_VALUE;
                syncCountryReserves();
            }
        }

        return itemstack;
    }
}
