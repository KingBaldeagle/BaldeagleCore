package com.baldeagle.blocks.shop;

import java.util.UUID;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagLong;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;

public class TileEntityShop extends TileEntity implements IInventory {

    public static final int SLOT_COUNT = 9;
    private final NonNullList<ItemStack> items = NonNullList.withSize(
        SLOT_COUNT,
        ItemStack.EMPTY
    );
    private final long[] prices = new long[SLOT_COUNT];

    private UUID owner;
    private UUID countryId;
    private long cashStored;

    public UUID getOwner() {
        return owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
        markDirty();
        sync();
    }

    public UUID getCountryId() {
        return countryId;
    }

    public void setCountryId(UUID countryId) {
        this.countryId = countryId;
        markDirty();
        sync();
    }

    public boolean isOwner(EntityPlayer player) {
        return (
            player != null &&
            owner != null &&
            owner.equals(player.getUniqueID())
        );
    }

    public long getCashStored() {
        return cashStored;
    }

    public void addCash(long amount) {
        if (amount <= 0) {
            return;
        }
        cashStored += amount;
        markDirty();
        sync();
    }

    public long withdrawAllCash() {
        long cash = cashStored;
        cashStored = 0;
        markDirty();
        sync();
        return cash;
    }

    public long getPrice(int slot) {
        if (slot < 0 || slot >= prices.length) {
            return 0;
        }
        return prices[slot];
    }

    public void setPrice(int slot, long price) {
        if (slot < 0 || slot >= prices.length) {
            return;
        }
        prices[slot] = Math.max(0, price);
        markDirty();
        sync();
    }

    private void sync() {
        if (world == null || world.isRemote) {
            return;
        }
        world.notifyBlockUpdate(
            pos,
            world.getBlockState(pos),
            world.getBlockState(pos),
            3
        );
    }

    @Override
    public int getSizeInventory() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) return false;
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
            sync();
        }
        return stack;
    }

    @Override
    public ItemStack removeStackFromSlot(int index) {
        ItemStack stack = ItemStackHelper.getAndRemove(items, index);
        if (!stack.isEmpty()) {
            markDirty();
            sync();
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
        sync();
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public boolean isUsableByPlayer(EntityPlayer player) {
        return (
            world != null &&
            world.getTileEntity(pos) == this &&
            player.getDistanceSq(
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5
            ) <=
            64
        );
    }

    @Override
    public void openInventory(EntityPlayer player) {}

    @Override
    public void closeInventory(EntityPlayer player) {}

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        return true;
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
        sync();
    }

    @Override
    public String getName() {
        return "container.baldeagle.shop";
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
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        ItemStackHelper.saveAllItems(compound, items);
        if (owner != null) {
            compound.setString("owner", owner.toString());
        }
        if (countryId != null) {
            compound.setString("country", countryId.toString());
        }
        compound.setLong("cash", cashStored);
        NBTTagList priceList = new NBTTagList();
        for (long price : prices) {
            priceList.appendTag(new NBTTagLong(price));
        }
        compound.setTag("prices", priceList);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        ItemStackHelper.loadAllItems(compound, items);
        owner = null;
        if (compound.hasKey("owner")) {
            try {
                owner = UUID.fromString(compound.getString("owner"));
            } catch (IllegalArgumentException ignored) {
                owner = null;
            }
        }
        countryId = null;
        if (compound.hasKey("country")) {
            try {
                countryId = UUID.fromString(compound.getString("country"));
            } catch (IllegalArgumentException ignored) {
                countryId = null;
            }
        }
        cashStored = compound.getLong("cash");
        if (compound.hasKey("prices")) {
            NBTTagList list = compound.getTagList("prices", 4);
            int len = Math.min(list.tagCount(), prices.length);
            for (int i = 0; i < len; i++) {
                prices[i] = ((NBTTagLong) list.get(i)).getLong();
            }
        }
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        NBTTagCompound tag = new NBTTagCompound();
        writeToNBT(tag);
        return new SPacketUpdateTileEntity(pos, 3, tag);
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        readFromNBT(pkt.getNbtCompound());
    }
}
