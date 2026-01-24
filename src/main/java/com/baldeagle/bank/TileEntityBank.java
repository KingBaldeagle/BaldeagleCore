package com.baldeagle.bank;

import com.baldeagle.country.Country;
import com.baldeagle.country.CountryManager;
import com.baldeagle.country.CountryStorage;
import com.baldeagle.country.currency.CurrencyItemHelper;
import com.baldeagle.country.items.ItemBill;
import com.baldeagle.country.items.ItemCoin;
import com.baldeagle.economy.EconomyManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;

public class TileEntityBank
    extends TileEntity
    implements ITickable, IInventory
{

    public static final int SLOT_PLAYER_DEPOSIT = 0;
    public static final int SLOT_COUNTRY_DEPOSIT = 1;
    public static final int SLOT_COUNT = 2;

    private static final double INTEREST_RATE = 0.01;

    private final NonNullList<ItemStack> items = NonNullList.withSize(
        SLOT_COUNT,
        ItemStack.EMPTY
    );
    private int tickCounter = 0;

    @Override
    public void update() {
        if (world == null || world.isRemote) {
            return;
        }

        tickCounter++;
        if (tickCounter >= 20 * 60) {
            EconomyManager.applyInterest(world, INTEREST_RATE);
            tickCounter = 0;
        }
    }

    public static boolean isCurrency(ItemStack stack) {
        return CurrencyItemHelper.isCurrency(stack);
    }

    public static long getCurrencyValue(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        return CurrencyItemHelper.getFaceValue(stack);
    }

    public static double getCurrencyMonetaryValue(
        TileEntityBank bank,
        ItemStack stack
    ) {
        if (bank == null || bank.world == null) {
            return 0;
        }
        return CurrencyItemHelper.getStackMonetaryValue(bank.world, stack);
    }

    @Override
    public int getSizeInventory() {
        return SLOT_COUNT;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getStackInSlot(int index) {
        return items.get(index);
    }

    @Override
    public ItemStack decrStackSize(int index, int count) {
        ItemStack stack = ItemStackHelper.getAndSplit(items, index, count);
        if (!stack.isEmpty()) {
            markDirty();
        }
        return stack;
    }

    @Override
    public ItemStack removeStackFromSlot(int index) {
        ItemStack stack = ItemStackHelper.getAndRemove(items, index);
        if (!stack.isEmpty()) {
            markDirty();
        }
        return stack;
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        items.set(index, stack);
        if (!stack.isEmpty() && stack.getCount() > getInventoryStackLimit()) {
            stack.setCount(getInventoryStackLimit());
        }
        markDirty();
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public boolean isUsableByPlayer(EntityPlayer player) {
        if (world == null || world.getTileEntity(pos) != this) {
            return false;
        }
        return (
            player.getDistanceSq(
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D
            ) <=
            64.0D
        );
    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        return index >= 0 && index < SLOT_COUNT && isCurrency(stack);
    }

    @Override
    public void openInventory(EntityPlayer player) {
        // No-op; kept for interface compatibility
    }

    @Override
    public void closeInventory(EntityPlayer player) {
        // No-op; kept for interface compatibility
    }

    @Override
    public int getField(int id) {
        return 0;
    }

    @Override
    public void setField(int id, int value) {}

    @Override
    public int getFieldCount() {
        return 0;
    }

    @Override
    public void clear() {
        for (int i = 0; i < items.size(); i++) {
            items.set(i, ItemStack.EMPTY);
        }
        markDirty();
    }

    @Override
    public String getName() {
        return "container.baldeagle.bank";
    }

    @Override
    public boolean hasCustomName() {
        return false;
    }

    @Override
    public ITextComponent getDisplayName() {
        return new TextComponentTranslation(getName());
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setInteger("ticks", tickCounter);
        ItemStackHelper.saveAllItems(tag, items);
        return tag;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        tickCounter = tag.getInteger("ticks");
        ItemStackHelper.loadAllItems(tag, items);
    }
}
