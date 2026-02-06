package com.baldeagle.blocks.bank;

import com.baldeagle.country.Country;
import com.baldeagle.country.CountryManager;
import com.baldeagle.country.CountryStorage;
import com.baldeagle.country.currency.CurrencyItemHelper;
import com.baldeagle.economy.EconomyManager;
import com.baldeagle.network.NetworkHandler;
import com.baldeagle.network.message.BankSyncMessage;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentString;

public class ContainerBank extends Container {

    private enum DepositTarget {
        PLAYER,
        COUNTRY,
    }

    private final TileEntityBank tileBank;
    private final EntityPlayer player;

    private long cachedPlayerBalance = Long.MIN_VALUE;
    private long cachedCountryBalance = Long.MIN_VALUE;

    public ContainerBank(
        InventoryPlayer playerInventory,
        TileEntityBank tileBank
    ) {
        this.tileBank = tileBank;
        this.player = playerInventory.player;

        tileBank.openInventory(player);

        this.addSlotToContainer(
            new CurrencySlot(
                tileBank,
                TileEntityBank.SLOT_PLAYER_DEPOSIT,
                44,
                36
            )
        );
        this.addSlotToContainer(
            new CurrencySlot(
                tileBank,
                TileEntityBank.SLOT_COUNTRY_DEPOSIT,
                116,
                36
            )
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

    @Override
    public void addListener(IContainerListener listener) {
        super.addListener(listener);
        if (!player.world.isRemote) {
            syncBalancesToPlayer();
        }
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();

        if (!player.world.isRemote) {
            handleDeposit(
                TileEntityBank.SLOT_PLAYER_DEPOSIT,
                DepositTarget.PLAYER
            );
            handleDeposit(
                TileEntityBank.SLOT_COUNTRY_DEPOSIT,
                DepositTarget.COUNTRY
            );

            syncBalancesToPlayer();
        }
    }

    private void handleDeposit(int slotIndex, DepositTarget target) {
        ItemStack stack = tileBank.removeStackFromSlot(slotIndex);
        if (stack.isEmpty()) {
            return;
        }

        long faceValue = TileEntityBank.getCurrencyValue(stack);
        if (faceValue <= 0) {
            returnStackToPlayer(stack);
            return;
        }

        if (target == DepositTarget.PLAYER) {
            CurrencyItemHelper.removeFromCirculation(player.world, stack);
            EconomyManager.depositPlayer(
                player.world,
                player.getUniqueID(),
                faceValue
            );
            player.sendStatusMessage(
                new TextComponentString(
                    "Deposited " + faceValue + " to your personal balance."
                ),
                true
            );
            tileBank.markDirty();
            return;
        }

        Country country = CountryManager.getCountryForPlayer(
            player.world,
            player.getUniqueID()
        );
        if (country == null) {
            returnStackToPlayer(stack);
            player.sendStatusMessage(
                new TextComponentString(
                    "Join a country to deposit into its balance."
                ),
                true
            );
            return;
        }
        if (!country.isAuthorized(player.getUniqueID())) {
            returnStackToPlayer(stack);
            player.sendStatusMessage(
                new TextComponentString(
                    "You are not authorized to deposit to " +
                        country.getName() +
                        "."
                ),
                true
            );
            return;
        }
        if (
            !CurrencyItemHelper.enforceCountryMatch(
                player.world,
                stack,
                country,
                player
            )
        ) {
            returnStackToPlayer(stack);
            return;
        }

        try {
            country.deposit(player.getUniqueID(), faceValue);
            CountryStorage.get(player.world).markDirty();
            CurrencyItemHelper.removeFromCirculation(player.world, stack);
        } catch (IllegalArgumentException e) {
            returnStackToPlayer(stack);
            player.sendStatusMessage(
                new TextComponentString(e.getMessage()),
                true
            );
            return;
        }

        player.sendStatusMessage(
            new TextComponentString(
                "Deposited " + faceValue + " to " + country.getName() + "."
            ),
            true
        );
        tileBank.markDirty();
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

    public TileEntityBank getTile() {
        return tileBank;
    }

    public long getDisplayedPlayerBalance() {
        return tileBank.getClientPlayerBalance();
    }

    public long getDisplayedCountryBalance() {
        return tileBank.getClientCountryBalance();
    }

    private void syncBalancesToPlayer() {
        if (!(player instanceof EntityPlayerMP)) {
            return;
        }

        long currentPlayerBalance = EconomyManager.getPlayerBalance(
            player.world,
            player.getUniqueID()
        );

        Country country = CountryManager.getCountryForPlayer(
            player.world,
            player.getUniqueID()
        );
        long currentCountryBalance =
            country != null ? country.getBalance() : 0L;

        if (
            currentPlayerBalance == cachedPlayerBalance &&
            currentCountryBalance == cachedCountryBalance
        ) {
            return;
        }

        cachedPlayerBalance = currentPlayerBalance;
        cachedCountryBalance = currentCountryBalance;
        NetworkHandler.INSTANCE.sendTo(
            new BankSyncMessage(
                tileBank.getPos(),
                currentPlayerBalance,
                currentCountryBalance
            ),
            (EntityPlayerMP) player
        );
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
            returnStackToPlayer(
                tileBank.removeStackFromSlot(TileEntityBank.SLOT_PLAYER_DEPOSIT)
            );
            returnStackToPlayer(
                tileBank.removeStackFromSlot(
                    TileEntityBank.SLOT_COUNTRY_DEPOSIT
                )
            );
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
                if (
                    !mergeItemStack(
                        stack,
                        TileEntityBank.SLOT_COUNT,
                        inventorySlots.size(),
                        true
                    )
                ) {
                    return ItemStack.EMPTY;
                }
            } else if (TileEntityBank.isCurrency(stack)) {
                if (
                    !mergeItemStack(stack, 0, TileEntityBank.SLOT_COUNT, false)
                ) {
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

        public CurrencySlot(
            TileEntityBank tile,
            int index,
            int xPosition,
            int yPosition
        ) {
            super(tile, index, xPosition, yPosition);
        }

        @Override
        public boolean isItemValid(ItemStack stack) {
            return TileEntityBank.isCurrency(stack);
        }
    }
}
