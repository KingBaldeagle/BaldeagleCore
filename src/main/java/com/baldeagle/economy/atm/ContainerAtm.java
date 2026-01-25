package com.baldeagle.economy.atm;

import com.baldeagle.country.Country;
import com.baldeagle.country.CountryManager;
import com.baldeagle.economy.EconomyManager;
import com.baldeagle.network.NetworkHandler;
import com.baldeagle.network.message.AtmSyncMessage;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class ContainerAtm extends Container {

    private final TileEntityAtm tile;
    private final EntityPlayer player;

    private int cachedPlayerBalance = Integer.MIN_VALUE;
    private int cachedCountryBalance = Integer.MIN_VALUE;
    private int displayedPlayerBalance = 0;
    private int displayedCountryBalance = 0;
    private String cachedCountryName;

    public ContainerAtm(InventoryPlayer playerInventory, TileEntityAtm tile) {
        this.tile = tile;
        this.player = playerInventory.player;

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

    public TileEntityAtm getTile() {
        return tile;
    }

    public int getDisplayedPlayerBalance() {
        return displayedPlayerBalance;
    }

    public int getDisplayedCountryBalance() {
        return displayedCountryBalance;
    }

    @Override
    public void addListener(IContainerListener listener) {
        super.addListener(listener);
        if (!player.world.isRemote) {
            listener.sendWindowProperty(this, 0, getPlayerBalanceInt());
            listener.sendWindowProperty(
                this,
                1,
                Math.max(0, getCountryBalanceInt())
            );
            syncCountryName();
        }
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();

        if (player.world.isRemote) {
            return;
        }

        int currentPlayerBalance = getPlayerBalanceInt();
        int currentCountryBalance = Math.max(0, getCountryBalanceInt());

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

        syncCountryName();
    }

    private int getPlayerBalanceInt() {
        long balance = EconomyManager.getPlayerBalance(
            player.world,
            player.getUniqueID()
        );
        return (int) Math.min(Integer.MAX_VALUE, balance);
    }

    private int getCountryBalanceInt() {
        Country country = CountryManager.getCountryForPlayer(
            player.world,
            player.getUniqueID()
        );
        if (country == null) {
            return -1;
        }
        long balance = Math.round(country.getBalance());
        return (int) Math.min(Integer.MAX_VALUE, balance);
    }

    private void syncCountryName() {
        Country country = CountryManager.getCountryForPlayer(
            player.world,
            player.getUniqueID()
        );
        String name = country != null ? country.getName() : null;
        if (name == null) {
            name = "";
        }
        if (name.equals(cachedCountryName)) {
            return;
        }
        cachedCountryName = name;
        if (player instanceof net.minecraft.entity.player.EntityPlayerMP) {
            NetworkHandler.INSTANCE.sendTo(
                new AtmSyncMessage(tile.getPos(), name.isEmpty() ? null : name),
                (net.minecraft.entity.player.EntityPlayerMP) player
            );
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        if (tile == null || tile.getWorld() == null) {
            return false;
        }
        return (
            tile.getWorld().getTileEntity(tile.getPos()) == tile &&
            playerIn.getDistanceSq(
                tile.getPos().getX() + 0.5,
                tile.getPos().getY() + 0.5,
                tile.getPos().getZ() + 0.5
            ) <=
            64
        );
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    @net.minecraftforge.fml.relauncher.SideOnly(
        net.minecraftforge.fml.relauncher.Side.CLIENT
    )
    public void updateProgressBar(int id, int data) {
        if (id == 0) {
            displayedPlayerBalance = data;
        } else if (id == 1) {
            displayedCountryBalance = Math.max(0, data);
        }
    }
}
