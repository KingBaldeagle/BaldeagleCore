package com.baldeagle.bank;

import com.baldeagle.country.Country;
import com.baldeagle.country.CountryManager;
import com.baldeagle.country.CountryStorage;
import com.baldeagle.economy.EconomyManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ContainerBank extends Container {

    private enum DepositTarget {
        PLAYER,
        COUNTRY
    }

    private final TileEntityBank tileBank;
    private final EntityPlayer player;

    private int cachedPlayerBalance = Integer.MIN_VALUE;
    private int cachedCountryBalance = Integer.MIN_VALUE;
    private int displayedPlayerBalance = 0;
    private int displayedCountryBalance = 0;

    public ContainerBank(InventoryPlayer playerInventory, TileEntityBank tileBank) {
        this.tileBank = tileBank;
        this.player = playerInventory.player;

        tileBank.openInventory(player);

        this.addSlotToContainer(new CurrencySlot(tileBank, TileEntityBank.SLOT_PLAYER_DEPOSIT, 44, 36));
        this.addSlotToContainer(new CurrencySlot(tileBank, TileEntityBank.SLOT_COUNTRY_DEPOSIT, 116, 36));

        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlotToContainer(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        for (int hotbar = 0; hotbar < 9; ++hotbar) {
            this.addSlotToContainer(new Slot(playerInventory, hotbar, 8 + hotbar * 18, 142));
        }
    }

    @Override
    public void addListener(IContainerListener listener) {
        super.addListener(listener);
        if (!player.world.isRemote) {
            listener.sendWindowProperty(this, 0, getPlayerBalanceInt());
            listener.sendWindowProperty(this, 1, getCountryBalanceInt());
        }
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();

        if (!player.world.isRemote) {
            handleDeposit(TileEntityBank.SLOT_PLAYER_DEPOSIT, DepositTarget.PLAYER);
            handleDeposit(TileEntityBank.SLOT_COUNTRY_DEPOSIT, DepositTarget.COUNTRY);

            int currentPlayerBalance = getPlayerBalanceInt();
            int currentCountryBalance = getCountryBalanceInt();

            if (currentPlayerBalance != cachedPlayerBalance) {
                cachedPlayerBalance = currentPlayerBalance;
                for (IContainerListener listener : listeners) {
                    listener.sendWindowProperty(this, 0, currentPlayerBalance);
                }
            }

            if (currentCountryBalance != cachedCountryBalance) {
                cachedCountryBalance = currentCountryBalance;
                for (IContainerListener listener : listeners) {
                    listener.sendWindowProperty(this, 1, currentCountryBalance);
                }
            }
        }
    }

    private void handleDeposit(int slotIndex, DepositTarget target) {
        ItemStack stack = tileBank.removeStackFromSlot(slotIndex);
        if (stack.isEmpty()) {
            return;
        }

        long value = TileEntityBank.getCurrencyValue(stack);
        if (value <= 0) {
            returnStackToPlayer(stack);
        } else if (target == DepositTarget.PLAYER) {
            EconomyManager.depositPlayer(player.world, player.getUniqueID(), value);
            player.sendStatusMessage(new TextComponentString("Deposited " + value + " to your personal balance."), true);
            tileBank.markDirty();
        } else {
            Country country = CountryManager.getCountryForPlayer(player.world, player.getUniqueID());
            if (country == null) {
                returnStackToPlayer(stack);
                player.sendStatusMessage(new TextComponentString("Join a country to deposit into its balance."), true);
                return;
            }
            if (!country.isAuthorized(player.getUniqueID())) {
                returnStackToPlayer(stack);
                player.sendStatusMessage(new TextComponentString("You are not authorized to deposit to " + country.getName() + "."), true);
                return;
            }

            try {
                country.deposit(player.getUniqueID(), value);
            } catch (IllegalArgumentException e) {
                returnStackToPlayer(stack);
                player.sendStatusMessage(new TextComponentString(e.getMessage()), true);
                return;
            }

            CountryStorage.get(player.world).markDirty();
            EconomyManager.depositCountry(player.world, country.getName(), value);
            player.sendStatusMessage(new TextComponentString("Deposited " + value + " to " + country.getName() + "."), true);
            tileBank.markDirty();
        }
    }

    private void returnStackToPlayer(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        if (!player.inventory.addItemStackToInventory(stack)) {
            player.dropItem(stack, false);
        }
        player.inventory.markDirty();
    }

    private int getPlayerBalanceInt() {
        long balance = EconomyManager.getPlayerBalance(player.world, player.getUniqueID());
        return (int) Math.min(Integer.MAX_VALUE, balance);
    }

    private int getCountryBalanceInt() {
        Country country = CountryManager.getCountryForPlayer(player.world, player.getUniqueID());
        if (country == null) {
            return -1;
        }
        long balance = Math.round(country.getBalance());
        return (int) Math.min(Integer.MAX_VALUE, balance);
    }

    public int getDisplayedPlayerBalance() {
        return displayedPlayerBalance;
    }

    public int getDisplayedCountryBalance() {
        return displayedCountryBalance;
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return tileBank.isUsableByPlayer(playerIn);
    }

    @Override
    public void onContainerClosed(EntityPlayer playerIn) {
        super.onContainerClosed(playerIn);
        tileBank.closeInventory(playerIn);

        if (!playerIn.world.isRemote) {
            returnStackToPlayer(tileBank.removeStackFromSlot(TileEntityBank.SLOT_PLAYER_DEPOSIT));
            returnStackToPlayer(tileBank.removeStackFromSlot(TileEntityBank.SLOT_COUNTRY_DEPOSIT));
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void updateProgressBar(int id, int data) {
        if (id == 0) {
            displayedPlayerBalance = data;
        } else if (id == 1) {
            displayedCountryBalance = data;
        }
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = inventorySlots.get(index);

        if (slot != null && slot.getHasStack()) {
            ItemStack stack = slot.getStack();
            itemstack = stack.copy();

            if (index < TileEntityBank.SLOT_COUNT) {
                if (!mergeItemStack(stack, TileEntityBank.SLOT_COUNT, inventorySlots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (TileEntityBank.isCurrency(stack)) {
                if (!mergeItemStack(stack, 0, TileEntityBank.SLOT_COUNT, false)) {
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

    private static class CurrencySlot extends Slot {
        public CurrencySlot(TileEntityBank tile, int index, int xPosition, int yPosition) {
            super(tile, index, xPosition, yPosition);
        }

        @Override
        public boolean isItemValid(ItemStack stack) {
            return TileEntityBank.isCurrency(stack);
        }
    }
}