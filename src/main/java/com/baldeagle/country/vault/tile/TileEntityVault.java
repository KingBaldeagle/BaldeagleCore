package com.baldeagle.country.vault.tile;

import com.baldeagle.country.Country;
import com.baldeagle.country.CountryManager;
import com.baldeagle.country.CountryStorage;
import java.util.UUID;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;

public class TileEntityVault
    extends TileEntity
    implements IInventory, ISidedInventory
{

    public static final int SLOT_COUNT = 27;

    private static final long GOLD_VALUE = 1;
    private static final long DIAMOND_VALUE = 4;
    private static final long EMERALD_VALUE = 2;
    private static final int BLOCK_MULTIPLIER = 9;

    private static final int[] NO_SLOTS = new int[0];

    private final NonNullList<ItemStack> items = NonNullList.withSize(
        SLOT_COUNT,
        ItemStack.EMPTY
    );

    private UUID countryId;
    private long trackedReserveUnits = 0;
    private long countryReserveUnits = 0;
    private boolean suppressReconcile = false;

    public boolean ensureCountry(EntityPlayer player) {
        if (countryId != null) {
            return true;
        }
        if (world == null || player == null) {
            return false;
        }
        Country country = CountryManager.getCountryForPlayer(
            world,
            player.getUniqueID()
        );
        if (country == null) {
            return false;
        }
        setCountryId(country.getId());
        return true;
    }

    public void setCountryId(UUID countryId) {
        this.countryId = countryId;
        reconcileReserves();
        updateCountryReserveCache();
        markDirty();
        sync();
    }

    public UUID getCountryId() {
        return countryId;
    }

    public Country getCountry() {
        if (world == null || countryId == null) {
            return null;
        }
        return CountryManager.getCountry(world, countryId);
    }

    public boolean isAuthorized(EntityPlayer player) {
        Country country = getCountry();
        return (
            country != null &&
            player != null &&
            country.isHighAuthority(player.getUniqueID())
        );
    }

    public long getReserveUnits() {
        return computeReserveUnits();
    }

    public long getCountryReserveUnits() {
        return countryReserveUnits;
    }

    public void applyCountryReserveSync(long reserveUnits) {
        this.countryReserveUnits = Math.max(0, reserveUnits);
    }

    public int getGoldCount() {
        int total = 0;
        for (ItemStack stack : items) {
            if (stack.isEmpty()) continue;
            if (stack.getItem() == Items.GOLD_INGOT) {
                total += stack.getCount();
            } else if (
                stack.getItem() ==
                net.minecraft.item.Item.getItemFromBlock(Blocks.GOLD_BLOCK)
            ) {
                total += stack.getCount() * BLOCK_MULTIPLIER;
            }
        }
        return total;
    }

    public int consumeGold(int amount) {
        if (amount <= 0) {
            return 0;
        }
        if (world != null && world.isRemote) {
            return 0;
        }

        int remaining = amount;

        // Prefer ingots first
        for (int i = 0; i < items.size() && remaining > 0; i++) {
            ItemStack stack = items.get(i);
            if (stack.isEmpty() || stack.getItem() != Items.GOLD_INGOT) {
                continue;
            }
            int take = Math.min(remaining, stack.getCount());
            stack.shrink(take);
            if (stack.isEmpty()) {
                items.set(i, ItemStack.EMPTY);
            }
            remaining -= take;
        }

        // Then break blocks as needed and return change as ingots
        net.minecraft.item.Item goldBlockItem =
            net.minecraft.item.Item.getItemFromBlock(Blocks.GOLD_BLOCK);
        for (int i = 0; i < items.size() && remaining > 0; i++) {
            ItemStack stack = items.get(i);
            if (stack.isEmpty() || stack.getItem() != goldBlockItem) {
                continue;
            }
            while (!stack.isEmpty() && remaining > 0) {
                stack.shrink(1);
                if (stack.isEmpty()) {
                    items.set(i, ItemStack.EMPTY);
                }

                int fromThisBlock = Math.min(remaining, BLOCK_MULTIPLIER);
                remaining -= fromThisBlock;

                int change = BLOCK_MULTIPLIER - fromThisBlock;
                if (change > 0) {
                    insertReserveItem(Items.GOLD_INGOT, change);
                }
            }
        }

        int consumed = amount - remaining;
        if (consumed > 0) {
            markDirty();
            reconcileReserves();
            sync();
        }
        return consumed;
    }

    private long computeReserveUnits() {
        long total = 0;
        net.minecraft.item.Item goldBlockItem =
            net.minecraft.item.Item.getItemFromBlock(Blocks.GOLD_BLOCK);
        net.minecraft.item.Item diamondBlockItem =
            net.minecraft.item.Item.getItemFromBlock(Blocks.DIAMOND_BLOCK);
        net.minecraft.item.Item emeraldBlockItem =
            net.minecraft.item.Item.getItemFromBlock(Blocks.EMERALD_BLOCK);
        for (ItemStack stack : items) {
            if (stack.isEmpty()) continue;
            if (stack.getItem() == Items.GOLD_INGOT) {
                total += (long) stack.getCount() * GOLD_VALUE;
            } else if (stack.getItem() == goldBlockItem) {
                total +=
                    (long) stack.getCount() * BLOCK_MULTIPLIER * GOLD_VALUE;
            } else if (stack.getItem() == Items.DIAMOND) {
                total += (long) stack.getCount() * DIAMOND_VALUE;
            } else if (stack.getItem() == diamondBlockItem) {
                total +=
                    (long) stack.getCount() * BLOCK_MULTIPLIER * DIAMOND_VALUE;
            } else if (stack.getItem() == Items.EMERALD) {
                total += (long) stack.getCount() * EMERALD_VALUE;
            } else if (stack.getItem() == emeraldBlockItem) {
                total +=
                    (long) stack.getCount() * BLOCK_MULTIPLIER * EMERALD_VALUE;
            }
        }
        return total;
    }

    private void insertReserveItem(net.minecraft.item.Item item, int count) {
        if (count <= 0) {
            return;
        }
        int remaining = count;

        for (int i = 0; i < items.size() && remaining > 0; i++) {
            ItemStack existing = items.get(i);
            if (existing.isEmpty() || existing.getItem() != item) {
                continue;
            }
            int space =
                Math.min(getInventoryStackLimit(), existing.getMaxStackSize()) -
                existing.getCount();
            if (space <= 0) continue;
            int add = Math.min(space, remaining);
            existing.grow(add);
            remaining -= add;
        }

        for (int i = 0; i < items.size() && remaining > 0; i++) {
            ItemStack existing = items.get(i);
            if (!existing.isEmpty()) continue;
            int add = Math.min(getInventoryStackLimit(), remaining);
            items.set(i, new ItemStack(item, add));
            remaining -= add;
        }
    }

    private void reconcileReserves() {
        if (world == null || world.isRemote) {
            trackedReserveUnits = computeReserveUnits();
            return;
        }
        if (suppressReconcile) {
            trackedReserveUnits = computeReserveUnits();
            updateCountryReserveCache();
            return;
        }

        long actual = computeReserveUnits();
        long delta = actual - trackedReserveUnits;
        trackedReserveUnits = actual;

        if (delta == 0) {
            return;
        }

        Country country = getCountry();
        if (country == null) {
            return;
        }
        country.adjustTreasury(delta);
        updateCountryReserveCache();
        CountryStorage.get(world).markDirty();
    }

    public void prepareForDropAndUntrack() {
        if (world == null || world.isRemote) {
            return;
        }
        if (suppressReconcile) {
            return;
        }
        suppressReconcile = true;

        long actual = computeReserveUnits();
        if (actual <= 0) {
            trackedReserveUnits = 0;
            updateCountryReserveCache();
            markDirty();
            return;
        }

        Country country = getCountry();
        if (country != null) {
            country.adjustTreasury(-actual);
            updateCountryReserveCache();
            CountryStorage.get(world).markDirty();
        }
        trackedReserveUnits = 0;
        markDirty();
    }

    private void updateCountryReserveCache() {
        Country country = getCountry();
        countryReserveUnits =
            country != null ? Math.max(0, country.getTreasury()) : 0;
    }

    private void sync() {
        if (world == null || world.isRemote) {
            return;
        }
        updateCountryReserveCache();
        world.notifyBlockUpdate(
            pos,
            world.getBlockState(pos),
            world.getBlockState(pos),
            3
        );
    }

    // Inventory implementation

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
            reconcileReserves();
            sync();
        }
        return stack;
    }

    @Override
    public ItemStack removeStackFromSlot(int index) {
        ItemStack stack = ItemStackHelper.getAndRemove(items, index);
        if (!stack.isEmpty()) {
            markDirty();
            reconcileReserves();
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
        reconcileReserves();
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
        if (stack.isEmpty()) {
            return false;
        }
        net.minecraft.item.Item item = stack.getItem();
        return (
            item == Items.GOLD_INGOT ||
            item == Items.DIAMOND ||
            item == Items.EMERALD ||
            item ==
            net.minecraft.item.Item.getItemFromBlock(Blocks.GOLD_BLOCK) ||
            item ==
            net.minecraft.item.Item.getItemFromBlock(Blocks.DIAMOND_BLOCK) ||
            item ==
            net.minecraft.item.Item.getItemFromBlock(Blocks.EMERALD_BLOCK)
        );
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
        reconcileReserves();
        sync();
    }

    @Override
    public String getName() {
        return "container.baldeagle.vault";
    }

    @Override
    public boolean hasCustomName() {
        return false;
    }

    @Override
    public ITextComponent getDisplayName() {
        return new TextComponentTranslation(getName());
    }

    // ISidedInventory: deny automation access

    @Override
    public int[] getSlotsForFace(EnumFacing side) {
        return NO_SLOTS;
    }

    @Override
    public boolean canInsertItem(
        int index,
        ItemStack itemStackIn,
        EnumFacing direction
    ) {
        return false;
    }

    @Override
    public boolean canExtractItem(
        int index,
        ItemStack stack,
        EnumFacing direction
    ) {
        return false;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        ItemStackHelper.saveAllItems(compound, items);
        if (countryId != null) {
            compound.setString("country", countryId.toString());
        }
        compound.setLong("trackedReserves", trackedReserveUnits);
        compound.setLong("countryReserves", countryReserveUnits);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        ItemStackHelper.loadAllItems(compound, items);
        if (compound.hasKey("country")) {
            try {
                countryId = UUID.fromString(compound.getString("country"));
            } catch (IllegalArgumentException ignored) {
                countryId = null;
            }
        }
        trackedReserveUnits = compound.getLong("trackedReserves");
        countryReserveUnits = compound.getLong("countryReserves");
        // Reconcile later when world is available
        if (world != null && !world.isRemote) {
            reconcileReserves();
        } else {
            trackedReserveUnits = computeReserveUnits();
        }
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        NBTTagCompound tag = new NBTTagCompound();
        writeToNBT(tag);
        return new SPacketUpdateTileEntity(pos, 2, tag);
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        readFromNBT(pkt.getNbtCompound());
    }
}
