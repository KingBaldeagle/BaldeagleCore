package com.baldeagle.blocks.research.tile;

import com.baldeagle.country.Country;
import com.baldeagle.country.CountryManager;
import com.baldeagle.country.CountryStorage;
import com.baldeagle.country.currency.CurrencyItemHelper;
import com.baldeagle.items.ModItems;
import com.baldeagle.network.NetworkHandler;
import com.baldeagle.network.message.ResearchAssemblerSyncMessage;
import com.baldeagle.blocks.research.ResearchCoreTier;
import java.util.UUID;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.NonNullList;

public class TileEntityResearchAssembler
    extends TileEntity
    implements ITickable, IInventory
{

    private static final int SLOT_INPUT = 0;
    private static final int SLOT_OUTPUT = 1;
    private static final ResearchCoreTier[] TIER_ORDER = new ResearchCoreTier[] {
        ResearchCoreTier.T1,
        ResearchCoreTier.T1_DEPOSIT,
        ResearchCoreTier.T2,
        ResearchCoreTier.T3,
    };

    private final NonNullList<ItemStack> items = NonNullList.withSize(
        2,
        ItemStack.EMPTY
    );

    private long storedResearchCredits = 0L;
    private ResearchCoreTier selectedTier = ResearchCoreTier.T1;
    private String ownerCountryName = "";
    private UUID ownerCountryId;
    private boolean creditsMigrated = false;
    private long legacyStoredCredits = 0L;

    private String lastCountryName = "";
    private double lastExchangeRate = 0.0D;
    private double lastInflationModifier = 0.0D;
    private long lastConvertedCredits = 0L;
    private String lastStatus = "";

    @Override
    public void update() {
        if (world == null || world.isRemote) {
            return;
        }

        boolean changed = false;
        String prevStatus = lastStatus;
        lastStatus = "";

        Country ownerCountry = getBoundCountry();
        if (ownerCountry != null) {
            if (!creditsMigrated && legacyStoredCredits > 0L) {
                ownerCountry.addResearchCredits(legacyStoredCredits);
                legacyStoredCredits = 0L;
                creditsMigrated = true;
                CountryStorage.get(world).markDirty();
                changed = true;
            }
            storedResearchCredits = ownerCountry.getResearchCredits();
        } else if (selectedTier == ResearchCoreTier.T1) {
            lastStatus = "Join a country";
        }

        if (
            selectedTier == ResearchCoreTier.T1 ||
            selectedTier == ResearchCoreTier.T1_DEPOSIT
        ) {
            if (handleCurrencyInput()) {
                changed = true;
            }
            if (selectedTier == ResearchCoreTier.T1) {
                if (tryOutputCore()) {
                    changed = true;
                }
            }
        } else if (selectedTier == ResearchCoreTier.T2) {
            if (handleCoreUpgrade(ModItems.T1_CORE, ModItems.T2_CORE)) {
                changed = true;
            }
        } else if (selectedTier == ResearchCoreTier.T3) {
            if (handleCoreUpgrade(ModItems.T2_CORE, ModItems.T3_CORE)) {
                changed = true;
            }
        }

        if (!changed && !prevStatus.equals(lastStatus)) {
            changed = true;
        }

        if (changed) {
            markDirty();
            sync();
        }
    }

    private Country getBoundCountry() {
        if (world == null) {
            return null;
        }
        if (ownerCountryId != null) {
            return CountryManager.getCountry(world, ownerCountryId);
        }
        if (ownerCountryName == null || ownerCountryName.trim().isEmpty()) {
            return null;
        }
        for (Country country : CountryStorage.get(world)
            .getCountriesMap()
            .values()) {
            if (
                country != null &&
                ownerCountryName.equalsIgnoreCase(country.getName())
            ) {
                ownerCountryId = country.getId();
                return country;
            }
        }
        return null;
    }

    private boolean handleCurrencyInput() {
        ItemStack input = items.get(SLOT_INPUT);
        if (input.isEmpty()) {
            return false;
        }
        if (!CurrencyItemHelper.isCurrency(input)) {
            lastStatus = "Insert currency";
            return false;
        }
        if (selectedTier == ResearchCoreTier.T1) {
            if (!canAcceptOutput(selectedTier.getItem(), 1)) {
                lastStatus = "Output full";
                return false;
            }
        }

        Country ownerCountry = getBoundCountry();
        if (ownerCountry == null) {
            lastStatus = "Wrong Currency";
            return false;
        }
        Country currencyCountry = CurrencyItemHelper.getCountry(world, input);
        if (!isCurrencyAllowed(ownerCountry, currencyCountry)) {
            lastStatus = "Wrong currency";
            return false;
        }

        long credits = convertCurrencyToCredits(input);
        if (credits <= 0) {
            lastConvertedCredits = 0L;
            return false;
        }
        ownerCountry.addResearchCredits(credits);
        storedResearchCredits = ownerCountry.getResearchCredits();
        CountryStorage.get(world).markDirty();
        CurrencyItemHelper.removeFromCirculation(world, input);
        items.set(SLOT_INPUT, ItemStack.EMPTY);
        return true;
    }

    private boolean handleCoreUpgrade(
        net.minecraft.item.Item inputItem,
        net.minecraft.item.Item outputItem
    ) {
        ItemStack input = items.get(SLOT_INPUT);
        if (input.isEmpty()) {
            return false;
        }
        if (input.getItem() != inputItem) {
            lastStatus = "Insert lower cores";
            return false;
        }
        if (!canAcceptOutput(outputItem, 1)) {
            lastStatus = "Output full";
            return false;
        }

        int crafts = input.getCount() / 9;
        if (crafts <= 0) {
            lastStatus = "Need 9 cores";
            return false;
        }
        int maxCrafts = Math.min(crafts, getMaxOutputFit(outputItem));
        if (maxCrafts <= 0) {
            return false;
        }
        input.shrink(maxCrafts * 9);
        if (input.isEmpty()) {
            items.set(SLOT_INPUT, ItemStack.EMPTY);
        }
        addOutput(outputItem, maxCrafts);
        return true;
    }

    private long convertCurrencyToCredits(ItemStack stack) {
        Country country = CurrencyItemHelper.getCountry(world, stack);
        com.baldeagle.country.currency.CurrencyDenomination denom =
            CurrencyItemHelper.getDenomination(stack);
        long faceValue = CurrencyItemHelper.getFaceValue(stack);
        if (country == null || faceValue <= 0 || denom == null) {
            lastCountryName = "";
            lastExchangeRate = 0.0D;
            lastInflationModifier = 0.0D;
            lastConvertedCredits = 0L;
            return 0;
        }

        double exchangeRate = country.getExchangeValue();
        double inflation = country.getInflation();
        double inflationModifier = inflation > 0 ? (1.0D / inflation) : 0.0D;
        double creditsRaw =
            faceValue *
            exchangeRate *
            inflationModifier *
            denom.getResearchMultiplier();
        long credits = creditsRaw > 0 ? (long) Math.floor(creditsRaw) : 0L;

        lastCountryName = country.getName();
        lastExchangeRate = exchangeRate;
        lastInflationModifier = inflationModifier;
        lastConvertedCredits = credits;

        if (credits > 0) {
            CountryStorage.get(world).markDirty();
        }

        return credits;
    }

    private boolean isCurrencyAllowed(
        Country ownerCountry,
        Country currencyCountry
    ) {
        if (currencyCountry == null || ownerCountry == null) {
            return false;
        }
        return ownerCountry.getId().equals(currencyCountry.getId());
    }

    private boolean canAcceptOutput(Item item, int count) {
        if (item == null || count <= 0) {
            return false;
        }
        ItemStack output = items.get(SLOT_OUTPUT);
        if (output.isEmpty()) {
            return count <= item.getItemStackLimit();
        }
        if (output.getItem() != item) {
            return false;
        }
        return output.getCount() + count <= output.getMaxStackSize();
    }

    private int getMaxOutputFit(Item item) {
        if (item == null) {
            return 0;
        }
        ItemStack output = items.get(SLOT_OUTPUT);
        if (output.isEmpty()) {
            return item.getItemStackLimit();
        }
        if (output.getItem() != item) {
            return 0;
        }
        return Math.max(0, output.getMaxStackSize() - output.getCount());
    }

    private void addOutput(Item item, int count) {
        if (item == null || count <= 0) {
            return;
        }
        ItemStack output = items.get(SLOT_OUTPUT);
        if (output.isEmpty()) {
            items.set(SLOT_OUTPUT, new ItemStack(item, count));
        } else {
            output.grow(count);
        }
    }

    private boolean tryOutputCore() {
        Country ownerCountry = getBoundCountry();
        if (ownerCountry == null) {
            return false;
        }
        long cost = selectedTier.getCost();
        storedResearchCredits = ownerCountry.getResearchCredits();
        if (cost <= 0 || storedResearchCredits < cost) {
            return false;
        }

        Item coreItem = selectedTier.getItem();
        if (coreItem == null) {
            return false;
        }

        if (!canAcceptOutput(coreItem, 1)) {
            return false;
        }

        addOutput(coreItem, 1);
        if (!ownerCountry.consumeResearchCredits(cost)) {
            return false;
        }
        storedResearchCredits = ownerCountry.getResearchCredits();
        CountryStorage.get(world).markDirty();
        return true;
    }

    public void cycleTier(boolean forward) {
        int index = 0;
        for (int i = 0; i < TIER_ORDER.length; i++) {
            if (TIER_ORDER[i] == selectedTier) {
                index = i;
                break;
            }
        }
        int next =
            (index + (forward ? 1 : -1) + TIER_ORDER.length) %
            TIER_ORDER.length;
        selectedTier = TIER_ORDER[next];
        sync();
        markDirty();
    }

    public long getStoredResearchCredits() {
        return storedResearchCredits;
    }

    public ResearchCoreTier getSelectedTier() {
        return selectedTier;
    }

    public String getOwnerCountryName() {
        return ownerCountryName;
    }

    public String getLastCountryName() {
        return lastCountryName;
    }

    public double getLastExchangeRate() {
        return lastExchangeRate;
    }

    public double getLastInflationModifier() {
        return lastInflationModifier;
    }

    public long getLastConvertedCredits() {
        return lastConvertedCredits;
    }

    public String getLastStatus() {
        return lastStatus;
    }

    public void setOwnerCountry(Country country) {
        if (country == null) {
            ownerCountryId = null;
            ownerCountryName = "";
        } else {
            ownerCountryId = country.getId();
            ownerCountryName = country.getName();
            storedResearchCredits = country.getResearchCredits();
        }
        sync();
        markDirty();
    }

    private void sync() {
        if (world == null || world.isRemote) {
            return;
        }
        Country ownerCountry = getBoundCountry();
        storedResearchCredits =
            ownerCountry != null ? ownerCountry.getResearchCredits() : 0L;
        world.notifyBlockUpdate(
            pos,
            world.getBlockState(pos),
            world.getBlockState(pos),
            3
        );
        NetworkHandler.sendToAllAround(
            new ResearchAssemblerSyncMessage(
                pos,
                storedResearchCredits,
                selectedTier.ordinal(),
                ownerCountryName,
                lastCountryName,
                lastExchangeRate,
                lastInflationModifier,
                lastConvertedCredits,
                lastStatus
            ),
            world.provider.getDimension(),
            pos
        );
    }

    public void applySync(
        long storedCredits,
        int tierOrdinal,
        String ownerName,
        String countryName,
        double exchangeRate,
        double inflationModifier,
        long convertedCredits,
        String status
    ) {
        this.storedResearchCredits = Math.max(0L, storedCredits);
        this.selectedTier = ResearchCoreTier.fromOrdinal(tierOrdinal);
        this.ownerCountryName = ownerName != null ? ownerName : "";
        this.lastCountryName = countryName != null ? countryName : "";
        this.lastExchangeRate = exchangeRate;
        this.lastInflationModifier = inflationModifier;
        this.lastConvertedCredits = Math.max(0L, convertedCredits);
        this.lastStatus = status != null ? status : "";
    }

    public Container createContainer(EntityPlayer player) {
        return new com.baldeagle.blocks.research.container.ContainerResearchAssembler(
            player.inventory,
            this
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

    public boolean isInputValid(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        switch (selectedTier) {
            case T1:
            case T1_DEPOSIT:
                return CurrencyItemHelper.isCurrency(stack);
            case T2:
                return stack.getItem() == ModItems.T1_CORE;
            case T3:
                return stack.getItem() == ModItems.T2_CORE;
            default:
                return false;
        }
    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        return index == SLOT_INPUT && isInputValid(stack);
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
        return "container.baldeagle.research_assembler";
    }

    @Override
    public boolean hasCustomName() {
        return false;
    }

    @Override
    public net.minecraft.util.text.ITextComponent getDisplayName() {
        return new net.minecraft.util.text.TextComponentTranslation(getName());
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setLong("storedCredits", storedResearchCredits);
        compound.setInteger("tier", selectedTier.ordinal());
        compound.setBoolean("creditsMigrated", creditsMigrated);
        compound.setLong("legacyStoredCredits", legacyStoredCredits);
        if (ownerCountryId != null) {
            compound.setString("ownerId", ownerCountryId.toString());
        }
        compound.setString("ownerName", ownerCountryName);
        compound.setString("lastCountry", lastCountryName);
        compound.setDouble("lastRate", lastExchangeRate);
        compound.setDouble("lastInflation", lastInflationModifier);
        compound.setLong("lastCredits", lastConvertedCredits);
        compound.setString("lastStatus", lastStatus);
        ItemStackHelper.saveAllItems(compound, items);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        storedResearchCredits = Math.max(0L, compound.getLong("storedCredits"));
        selectedTier = ResearchCoreTier.fromOrdinal(
            compound.getInteger("tier")
        );
        creditsMigrated = compound.getBoolean("creditsMigrated");
        if (compound.hasKey("legacyStoredCredits")) {
            legacyStoredCredits = Math.max(
                0L,
                compound.getLong("legacyStoredCredits")
            );
        } else if (!creditsMigrated) {
            legacyStoredCredits = storedResearchCredits;
        } else {
            legacyStoredCredits = 0L;
        }
        ownerCountryName = compound.getString("ownerName");
        if (compound.hasKey("ownerId")) {
            try {
                ownerCountryId = UUID.fromString(compound.getString("ownerId"));
            } catch (IllegalArgumentException ignored) {
                ownerCountryId = null;
            }
        }
        if (compound.hasKey("autoCreateCores")) {
            boolean autoCreateCores = compound.getBoolean("autoCreateCores");
            if (!autoCreateCores && selectedTier == ResearchCoreTier.T1) {
                selectedTier = ResearchCoreTier.T1_DEPOSIT;
            }
        }
        lastCountryName = compound.getString("lastCountry");
        lastExchangeRate = compound.getDouble("lastRate");
        lastInflationModifier = compound.getDouble("lastInflation");
        lastConvertedCredits = compound.getLong("lastCredits");
        lastStatus = compound.getString("lastStatus");
        ItemStackHelper.loadAllItems(compound, items);
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
