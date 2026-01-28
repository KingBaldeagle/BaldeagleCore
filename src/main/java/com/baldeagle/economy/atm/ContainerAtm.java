package com.baldeagle.economy.atm;

import com.baldeagle.country.Country;
import com.baldeagle.country.CountryManager;
import com.baldeagle.economy.EconomyManager;
import com.baldeagle.network.NetworkHandler;
import com.baldeagle.network.message.AtmBalanceSyncMessage;
import com.baldeagle.network.message.AtmSyncMessage;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class ContainerAtm extends Container {

    private final TileEntityAtm tile;
    private final EntityPlayer player;

    private long cachedPlayerBalance = Long.MIN_VALUE;
    private long cachedCountryBalance = Long.MIN_VALUE;
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

    public long getDisplayedPlayerBalance() {
        return tile.getClientPlayerBalance();
    }

    public long getDisplayedCountryBalance() {
        return tile.getClientCountryBalance();
    }

    @Override
    public void addListener(IContainerListener listener) {
        super.addListener(listener);
        if (!player.world.isRemote) {
            syncBalancesToPlayer();
            syncCountryName();
        }
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();

        if (player.world.isRemote) {
            return;
        }

        syncBalancesToPlayer();
        syncCountryName();
    }

    private long getCountryBalanceLong() {
        Country country = CountryManager.getCountryForPlayer(
            player.world,
            player.getUniqueID()
        );
        if (country == null) {
            return 0L;
        }
        return country.getBalance();
    }

    private void syncBalancesToPlayer() {
        if (!(player instanceof EntityPlayerMP)) {
            return;
        }

        long currentPlayerBalance = EconomyManager.getPlayerBalance(
            player.world,
            player.getUniqueID()
        );
        long currentCountryBalance = getCountryBalanceLong();

        if (
            currentPlayerBalance == cachedPlayerBalance &&
            currentCountryBalance == cachedCountryBalance
        ) {
            return;
        }

        cachedPlayerBalance = currentPlayerBalance;
        cachedCountryBalance = currentCountryBalance;
        NetworkHandler.INSTANCE.sendTo(
            new AtmBalanceSyncMessage(
                tile.getPos(),
                currentPlayerBalance,
                currentCountryBalance
            ),
            (EntityPlayerMP) player
        );
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
}
