package com.baldeagle.blocks.mint.tile;

import com.baldeagle.blocks.mint.MintingConstants;
import com.baldeagle.blocks.vault.VaultManager;
import com.baldeagle.country.Country;
import com.baldeagle.country.CountryManager;
import com.baldeagle.country.CountryStorage;
import com.baldeagle.country.currency.CurrencyDenomination;
import com.baldeagle.country.currency.CurrencyItemHelper;
import com.baldeagle.country.currency.CurrencyType;
import com.baldeagle.network.NetworkHandler;
import com.baldeagle.network.message.MintSyncMessage;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;

public class TileEntityMint
    extends TileEntity
    implements ITickable, IInventory
{

    private static final int SLOT_GOLD = 0;
    private final NonNullList<ItemStack> items = NonNullList.withSize(
        1,
        ItemStack.EMPTY
    );

    private UUID countryId;
    private CurrencyType selectedType = CurrencyType.COIN;
    private CurrencyDenomination selectedDenomination =
        CurrencyDenomination.COIN_1;
    private int amount = 1;
    private double projectedInflation = 0;
    private long projectedCirculation = 0;

    @Override
    public void update() {
        if (world != null && world.isRemote) {
            return;
        }
    }

    public boolean ensureCountry(World world, EntityPlayer player) {
        if (countryId != null) {
            return true;
        }
        Country country = CountryManager.getCountryForPlayer(
            world,
            player.getUniqueID()
        );
        if (country == null) {
            player.sendStatusMessage(
                new net.minecraft.util.text.TextComponentString(
                    "Join a country first."
                ),
                true
            );
            return false;
        }
        setCountryId(country.getId());
        markDirty();
        sync();
        return true;
    }

    public void setCountryId(UUID countryId) {
        this.countryId = countryId;
        recalc();
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
        return country != null && country.isTreasurerAuthority(player.getUniqueID());
    }

    public CurrencyType getSelectedType() {
        return selectedType;
    }

    public CurrencyDenomination getSelectedDenomination() {
        return selectedDenomination;
    }

    public int getAmount() {
        return amount;
    }

    public double getProjectedInflation() {
        return projectedInflation;
    }

    public long getProjectedCirculation() {
        return projectedCirculation;
    }

    public void toggleType(EntityPlayer player) {
        CurrencyType nextType =
            selectedDenomination.getType() == CurrencyType.COIN
                ? CurrencyType.BILL
                : CurrencyType.COIN;
        selectedDenomination = CurrencyDenomination.firstOfType(nextType);
        selectedType = selectedDenomination.getType();
        clampAmount();
        recalc();
        syncToClient(player);
    }

    public void cycleDenomination(boolean forward, EntityPlayer player) {
        selectedDenomination = CurrencyDenomination.nextAny(
            selectedDenomination,
            forward
        );
        selectedType = selectedDenomination.getType();
        clampAmount();
        recalc();
        syncToClient(player);
    }

    public void changeAmount(int delta, EntityPlayer player) {
        amount += delta;
        clampAmount();
        recalc();
        syncToClient(player);
    }

    private void clampAmount() {
        CurrencyType type =
            selectedDenomination != null
                ? selectedDenomination.getType()
                : CurrencyType.COIN;
        int max = type == CurrencyType.COIN ? 64 : 16;
        if (amount < 1) amount = 1;
        if (amount > max) amount = max;
    }

    private void recalc() {
        Country country = getCountry();
        if (country == null || selectedDenomination == null) {
            projectedInflation = 0;
            projectedCirculation = 0;
            return;
        }
        long mintedValue = (long) selectedDenomination.getValue() * amount;
        projectedInflation = country.calculateInflationImpact(
            mintedValue,
            MintingConstants.MINT_INFLATION_FACTOR
        );
        projectedCirculation = country.getMoneyInCirculation() + mintedValue;
    }

    public void performMint(EntityPlayer player) {
        if (world == null || world.isRemote) {
            return;
        }
        Country country = getCountry();
        if (country == null) {
            player.sendStatusMessage(
                new net.minecraft.util.text.TextComponentString(
                    "This mint is not assigned to a country."
                ),
                true
            );
            return;
        }
        if (!country.isHighAuthority(player.getUniqueID())) {
            player.sendStatusMessage(
                new net.minecraft.util.text.TextComponentString(
                    "You are not authorized."
                ),
                true
            );
            return;
        }
        long mintedValue = (long) selectedDenomination.getValue() * amount;
        if (mintedValue <= 0) {
            return;
        }
        int needed = amount;
        ItemStack gold = items.get(SLOT_GOLD);
        int inSlot =
            !gold.isEmpty() && gold.getItem() == Items.GOLD_INGOT
                ? gold.getCount()
                : 0;
        int fromVault = Math.max(0, needed - inSlot);

        if (fromVault > 0) {
            int availableFromVault = VaultManager.getAvailableGold(
                world,
                country.getId()
            );
            if (availableFromVault < fromVault) {
                player.sendStatusMessage(
                    new net.minecraft.util.text.TextComponentString(
                        "Not enough gold ingots. Need " +
                            needed +
                            " (missing " +
                            (fromVault - availableFromVault) +
                            ")."
                    ),
                    true
                );
                return;
            }
        } else if (inSlot < needed) {
            player.sendStatusMessage(
                new net.minecraft.util.text.TextComponentString(
                    "Not enough gold ingots. Need " + needed + "."
                ),
                true
            );
            return;
        }

        ItemStack currency = CurrencyItemHelper.createCurrencyStack(
            country,
            selectedDenomination,
            amount
        );
        if (currency.isEmpty()) {
            player.sendStatusMessage(
                new net.minecraft.util.text.TextComponentString(
                    "Failed to mint currency."
                ),
                true
            );
            return;
        }

        int toConsumeFromSlot = Math.min(needed, inSlot);
        if (toConsumeFromSlot > 0) {
            gold.shrink(toConsumeFromSlot);
            if (gold.isEmpty()) {
                items.set(SLOT_GOLD, ItemStack.EMPTY);
            }
        }
        if (fromVault > 0) {
            VaultManager.consumeGold(world, country.getId(), fromVault);
        }
        country.applyMinting(
            mintedValue,
            MintingConstants.MINT_INFLATION_FACTOR
        );
        CountryStorage.get(world).markDirty();

        if (!player.inventory.addItemStackToInventory(currency)) {
            player.dropItem(currency, false);
        }

        recalc();
        markDirty();
        syncToClient(player);
        player.sendStatusMessage(
            new net.minecraft.util.text.TextComponentString(
                "Minted " + amount + " x " + selectedDenomination.getValue()
            ),
            true
        );
    }

    public boolean performMintByValue(EntityPlayerMP player, long faceValue) {
        if (world == null || world.isRemote) {
            return false;
        }
        if (player == null || faceValue <= 0L) {
            return false;
        }
        Country country = getCountry();
        if (country == null) {
            return false;
        }
        if (!country.isHighAuthority(player.getUniqueID())) {
            return false;
        }

        List<CurrencyDenomination> denominations = Arrays.stream(
            CurrencyDenomination.values()
        )
            .sorted(
                Comparator.comparingInt(
                    CurrencyDenomination::getValue
                ).reversed()
            )
            .collect(Collectors.toList());

        long remaining = faceValue;
        long piecesNeeded = 0L;
        for (CurrencyDenomination denom : denominations) {
            if (denom == null) continue;
            long count = remaining / denom.getValue();
            if (count > 0L) {
                piecesNeeded += count;
                remaining = remaining % denom.getValue();
            }
            if (remaining <= 0L) break;
        }
        if (remaining > 0L) {
            return false;
        }
        if (piecesNeeded > (long) Integer.MAX_VALUE) {
            player.sendStatusMessage(
                new net.minecraft.util.text.TextComponentString(
                    "Mint amount is too large (too many items)."
                ),
                true
            );
            return false;
        }

        int needed = (int) piecesNeeded;
        ItemStack gold = items.get(SLOT_GOLD);
        int inSlot =
            !gold.isEmpty() && gold.getItem() == Items.GOLD_INGOT
                ? gold.getCount()
                : 0;
        int fromVault = Math.max(0, needed - inSlot);

        if (fromVault > 0) {
            int availableFromVault = VaultManager.getAvailableGold(
                world,
                country.getId()
            );
            if (availableFromVault < fromVault) {
                player.sendStatusMessage(
                    new net.minecraft.util.text.TextComponentString(
                        "Not enough gold ingots. Need " +
                            needed +
                            " (missing " +
                            (fromVault - availableFromVault) +
                            ")."
                    ),
                    true
                );
                return false;
            }
        } else if (inSlot < needed) {
            player.sendStatusMessage(
                new net.minecraft.util.text.TextComponentString(
                    "Not enough gold ingots. Need " + needed + "."
                ),
                true
            );
            return false;
        }

        int toConsumeFromSlot = Math.min(needed, inSlot);
        if (toConsumeFromSlot > 0) {
            gold.shrink(toConsumeFromSlot);
            if (gold.isEmpty()) {
                items.set(SLOT_GOLD, ItemStack.EMPTY);
            }
        }
        if (fromVault > 0) {
            VaultManager.consumeGold(world, country.getId(), fromVault);
        }

        remaining = faceValue;
        for (CurrencyDenomination denom : denominations) {
            if (denom == null) continue;
            long count = remaining / denom.getValue();
            if (count <= 0L) continue;

            int maxStack = denom.getType() == CurrencyType.COIN ? 64 : 16;
            while (count > 0L) {
                int give = (int) Math.min(count, (long) maxStack);
                ItemStack currency = CurrencyItemHelper.createCurrencyStack(
                    country,
                    denom,
                    give
                );
                if (!currency.isEmpty()) {
                    if (!player.inventory.addItemStackToInventory(currency)) {
                        player.dropItem(currency, false);
                    }
                }
                count -= give;
            }

            remaining = remaining % denom.getValue();
            if (remaining <= 0L) break;
        }

        country.applyMinting(faceValue, MintingConstants.MINT_INFLATION_FACTOR);
        CountryStorage.get(world).markDirty();

        recalc();
        markDirty();
        syncToClient(player);
        return true;
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
        markDirty();
    }

    private void syncToClient(EntityPlayer player) {
        if (
            world == null ||
            world.isRemote ||
            !(player instanceof EntityPlayerMP)
        ) {
            return;
        }
        NetworkHandler.INSTANCE.sendTo(
            new MintSyncMessage(
                pos,
                selectedType,
                selectedDenomination,
                amount,
                projectedInflation,
                projectedCirculation
            ),
            (EntityPlayerMP) player
        );
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        NBTTagCompound tag = new NBTTagCompound();
        writeToNBT(tag);
        return new SPacketUpdateTileEntity(pos, 0, tag);
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        readFromNBT(pkt.getNbtCompound());
    }

    public void applySync(
        CurrencyType type,
        CurrencyDenomination denomination,
        int amount,
        double inflation,
        long circulation
    ) {
        this.selectedDenomination = denomination;
        this.selectedType =
            denomination != null ? denomination.getType() : CurrencyType.COIN;
        this.amount = amount;
        this.projectedInflation = inflation;
        this.projectedCirculation = circulation;
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
            recalc();
            sync();
        }
        return stack;
    }

    @Override
    public ItemStack removeStackFromSlot(int index) {
        ItemStack stack = ItemStackHelper.getAndRemove(items, index);
        if (!stack.isEmpty()) {
            markDirty();
            recalc();
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
        recalc();
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
        return index == SLOT_GOLD && stack.getItem() == Items.GOLD_INGOT;
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
    }

    @Override
    public String getName() {
        return "container.baldeagle.mint";
    }

    @Override
    public boolean hasCustomName() {
        return false;
    }

    @Override
    public net.minecraft.util.text.ITextComponent getDisplayName() {
        return new net.minecraft.util.text.TextComponentTranslation(getName());
    }

    public Container createContainer(EntityPlayer player) {
        return new com.baldeagle.blocks.mint.container.ContainerMint(
            player.inventory,
            this
        );
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        if (countryId != null) {
            compound.setString("country", countryId.toString());
        }
        compound.setString("type", selectedType.getNbtKey());
        compound.setString("denom", selectedDenomination.getId());
        compound.setInteger("amount", amount);
        compound.setDouble("inflation", projectedInflation);
        compound.setLong("circulation", projectedCirculation);
        ItemStackHelper.saveAllItems(compound, items);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        if (compound.hasKey("country")) {
            try {
                countryId = UUID.fromString(compound.getString("country"));
            } catch (IllegalArgumentException ignored) {
                countryId = null;
            }
        }
        CurrencyDenomination denom = CurrencyDenomination.fromId(
            compound.getString("denom")
        );
        selectedDenomination =
            denom != null ? denom : CurrencyDenomination.COIN_1;
        selectedType = selectedDenomination.getType();
        amount = Math.max(1, compound.getInteger("amount"));
        projectedInflation = compound.getDouble("inflation");
        projectedCirculation = compound.getLong("circulation");
        ItemStackHelper.loadAllItems(compound, items);
    }
}
