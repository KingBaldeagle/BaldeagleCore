package com.baldeagle.blocks.currency_exchange.tile;

import com.baldeagle.blocks.mint.CurrencyMath;
import com.baldeagle.blocks.mint.MintingConstants;
import com.baldeagle.country.Country;
import com.baldeagle.country.CountryManager;
import com.baldeagle.country.CountryStorage;
import com.baldeagle.country.currency.CurrencyDenomination;
import com.baldeagle.country.currency.CurrencyItemHelper;
import com.baldeagle.economy.EconomyManager;
import com.baldeagle.network.NetworkHandler;
import com.baldeagle.network.message.ExchangeSyncMessage;
import java.util.*;
import java.util.stream.Collectors;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.NonNullList;

public class TileEntityCurrencyExchange
    extends TileEntity
    implements ITickable, IInventory
{

    private static final int SLOT_INPUT = 0;
    private final NonNullList<ItemStack> items = NonNullList.withSize(
        1,
        ItemStack.EMPTY
    );

    private UUID targetCountryId;
    private String targetCountryName;
    private double projectedRate = 0;
    private int projectedOutput = 0;

    @Override
    public void update() {
        if (world != null && !world.isRemote) {
            recalc();
        }
    }

    public void handleCycleTarget(boolean forward) {
        List<Country> countries = getCountriesSorted();
        if (countries.isEmpty()) {
            targetCountryId = null;
            targetCountryName = null;
            sync();
            return;
        }
        if (targetCountryId == null) {
            targetCountryId = countries.get(0).getId();
        } else {
            int index = -1;
            for (int i = 0; i < countries.size(); i++) {
                if (countries.get(i).getId().equals(targetCountryId)) {
                    index = i;
                    break;
                }
            }
            if (index == -1) {
                targetCountryId = countries.get(0).getId();
            } else {
                int nextIndex =
                    (index + (forward ? 1 : -1) + countries.size()) %
                    countries.size();
                targetCountryId = countries.get(nextIndex).getId();
            }
        }
        recalc();
        sync();
    }

    public void executeExchange(EntityPlayer player) {
        if (world == null || world.isRemote) {
            return;
        }
        ItemStack input = items.get(SLOT_INPUT);
        if (input.isEmpty()) {
            player.sendStatusMessage(
                new net.minecraft.util.text.TextComponentString(
                    "Insert currency first."
                ),
                true
            );
            return;
        }
        CurrencyDenomination sourceDenom = CurrencyItemHelper.getDenomination(
            input
        );
        Country sourceCountry = CurrencyItemHelper.getCountry(world, input);
        Country targetCountry = getTargetCountry();

        if (
            sourceDenom == null ||
            sourceCountry == null ||
            targetCountry == null
        ) {
            player.sendStatusMessage(
                new net.minecraft.util.text.TextComponentString(
                    "Invalid exchange setup."
                ),
                true
            );
            return;
        }
        if (
            sourceCountry.getId().equals(targetCountry.getId()) &&
            !com.baldeagle.config.BaldeagleConfig.allowReverseConversion
        ) {
            player.sendStatusMessage(
                new net.minecraft.util.text.TextComponentString(
                    "Reverse conversion is disabled."
                ),
                true
            );
            return;
        }

        double rate = CurrencyMath.computeExchangeRate(
            sourceCountry,
            targetCountry
        );
        if (rate <= 0) {
            player.sendStatusMessage(
                new net.minecraft.util.text.TextComponentString(
                    "Exchange rate unavailable."
                ),
                true
            );
            return;
        }

        long faceValue = CurrencyItemHelper.getFaceValue(input);
        if (faceValue <= 0) {
            player.sendStatusMessage(
                new net.minecraft.util.text.TextComponentString(
                    "No value to exchange."
                ),
                true
            );
            return;
        }

        double liquidityMultiplier = CurrencyMath.computeLiquidityMultiplier(
            sourceCountry,
            faceValue
        );
        double pressuredRate = rate * liquidityMultiplier;

        long grossTargetFaceValue = (long) Math.floor(
            faceValue * pressuredRate
        );
        if (grossTargetFaceValue <= 0) {
            player.sendStatusMessage(
                new net.minecraft.util.text.TextComponentString(
                    "No value after conversion."
                ),
                true
            );
            return;
        }

        double fee = sourceCountry.getExchangeFee();
        double conversionTax =
            com.baldeagle.config.BaldeagleConfig.currencyConversionTax;
        double conversionInterest =
            com.baldeagle.config.BaldeagleConfig.currencyConversionInterest;
        int interestThreshold =
            com.baldeagle.config.BaldeagleConfig.currencyConversionInterestThreshold;
        double extraFee =
            faceValue >= interestThreshold ? conversionInterest : 0.0D;
        double totalFeeRate = fee + conversionTax + extraFee;
        double safeFeeRate = Math.max(0.0D, Math.min(0.99D, totalFeeRate));
        long feeFaceValue = (long) Math.floor(
            grossTargetFaceValue * safeFeeRate
        );
        long targetFaceValue = Math.max(0, grossTargetFaceValue - feeFaceValue);
        if (targetFaceValue <= 0) {
            player.sendStatusMessage(
                new net.minecraft.util.text.TextComponentString(
                    "No value after fee."
                ),
                true
            );
            return;
        }

        double inflationImpact = targetCountry.calculateInflationImpact(
            targetFaceValue,
            MintingConstants.EXCHANGE_INFLATION_FACTOR
        );

        if (inflationImpact > MintingConstants.MAX_EXCHANGE_INFLATION_DELTA) {
            player.sendStatusMessage(
                new net.minecraft.util.text.TextComponentString(
                    "Exchange would destabilize " + targetCountry.getName()
                ),
                true
            );
            return;
        }

        items.set(SLOT_INPUT, ItemStack.EMPTY);
        markDirty();

        sourceCountry.removeFromCirculation(faceValue);
        sourceCountry.applyExchangePressure(faceValue);
        targetCountry.applyMinting(
            targetFaceValue,
            MintingConstants.EXCHANGE_INFLATION_FACTOR
        );
        if (feeFaceValue > 0) {
            EconomyManager.depositCountry(
                world,
                targetCountry.getName(),
                feeFaceValue
            );
        }
        CountryStorage.get(world).markDirty();

        long remaining = targetFaceValue;
        List<CurrencyDenomination> denominations = Arrays.stream(
            CurrencyDenomination.values()
        )
            .sorted(
                Comparator.comparingInt(
                    CurrencyDenomination::getValue
                ).reversed()
            )
            .collect(Collectors.toList());

        for (CurrencyDenomination denom : denominations) {
            if (denom == null) continue;
            long count = remaining / denom.getValue();
            if (count <= 0) continue;

            int maxStack =
                denom.getType() ==
                com.baldeagle.country.currency.CurrencyType.COIN
                    ? 64
                    : 16;
            while (count > 0) {
                int give = (int) Math.min(count, (long) maxStack);
                ItemStack output = CurrencyItemHelper.createCurrencyStack(
                    targetCountry,
                    denom,
                    give
                );
                if (!output.isEmpty()) {
                    if (!player.inventory.addItemStackToInventory(output)) {
                        player.dropItem(output, false);
                    }
                }
                count -= give;
            }

            remaining = remaining % denom.getValue();
            if (remaining <= 0) break;
        }

        recalc();
        sync();
        double feeMultiplier = 1.0D - safeFeeRate;
        player.sendStatusMessage(
            new net.minecraft.util.text.TextComponentString(
                "Exchange completed at rate " +
                    String.format("%.3f", pressuredRate * feeMultiplier)
            ),
            true
        );
    }

    private List<Country> getCountriesSorted() {
        if (world == null) {
            return Collections.emptyList();
        }
        return CountryStorage.get(world)
            .getCountriesMap()
            .values()
            .stream()
            .sorted(
                Comparator.comparing(
                    Country::getName,
                    String.CASE_INSENSITIVE_ORDER
                )
            )
            .collect(Collectors.toList());
    }

    private Country getTargetCountry() {
        if (world == null || targetCountryId == null) {
            return null;
        }
        return CountryManager.getCountry(world, targetCountryId);
    }

    public double getProjectedRate() {
        return projectedRate;
    }

    public int getProjectedOutput() {
        return projectedOutput;
    }

    public UUID getTargetCountryId() {
        return targetCountryId;
    }

    public String getTargetCountryName() {
        return targetCountryName;
    }

    public void applySync(
        UUID countryId,
        String countryName,
        double rate,
        int output
    ) {
        this.targetCountryId = countryId;
        this.targetCountryName = countryName;
        this.projectedRate = rate;
        this.projectedOutput = output;
    }

    private void recalc() {
        if (world == null || world.isRemote) {
            return;
        }
        ItemStack input = items.get(SLOT_INPUT);
        CurrencyDenomination sourceDenom = CurrencyItemHelper.getDenomination(
            input
        );
        Country sourceCountry = CurrencyItemHelper.getCountry(world, input);
        Country targetCountry = getTargetCountry();

        if (
            input.isEmpty() ||
            sourceDenom == null ||
            sourceCountry == null ||
            targetCountry == null
        ) {
            projectedRate = 0;
            projectedOutput = 0;
            return;
        }
        if (
            sourceCountry.getId().equals(targetCountry.getId()) &&
            !com.baldeagle.config.BaldeagleConfig.allowReverseConversion
        ) {
            projectedRate = 0;
            projectedOutput = 0;
            return;
        }

        double rate = CurrencyMath.computeExchangeRate(
            sourceCountry,
            targetCountry
        );
        if (rate <= 0) {
            projectedRate = 0;
            projectedOutput = 0;
            return;
        }

        long faceValue = CurrencyItemHelper.getFaceValue(input);
        double liquidityMultiplier = CurrencyMath.computeLiquidityMultiplier(
            sourceCountry,
            faceValue
        );
        double fee = sourceCountry.getExchangeFee();
        double conversionTax =
            com.baldeagle.config.BaldeagleConfig.currencyConversionTax;
        double conversionInterest =
            com.baldeagle.config.BaldeagleConfig.currencyConversionInterest;
        int interestThreshold =
            com.baldeagle.config.BaldeagleConfig.currencyConversionInterestThreshold;
        double extraFee =
            faceValue >= interestThreshold ? conversionInterest : 0.0D;
        double totalFeeRate = fee + conversionTax + extraFee;
        double safeFeeRate = Math.max(0.0D, Math.min(0.99D, totalFeeRate));
        double feeMultiplier = 1.0D - safeFeeRate;
        double finalRate = rate * liquidityMultiplier * feeMultiplier;
        projectedRate = finalRate;
        projectedOutput = (int) Math.max(0, Math.floor(faceValue * finalRate));
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
        Country targetCountry = getTargetCountry();
        NetworkHandler.sendToAllAround(
            new ExchangeSyncMessage(
                pos,
                targetCountryId,
                targetCountry != null ? targetCountry.getName() : null,
                projectedRate,
                projectedOutput
            ),
            world.provider.getDimension(),
            pos
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
        return index == SLOT_INPUT && CurrencyItemHelper.isCurrency(stack);
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
        return "container.baldeagle.currency_exchange";
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
        ItemStackHelper.saveAllItems(compound, items);
        if (targetCountryId != null) {
            compound.setString("target", targetCountryId.toString());
        }
        compound.setDouble("rate", projectedRate);
        compound.setInteger("output", projectedOutput);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        ItemStackHelper.loadAllItems(compound, items);
        if (compound.hasKey("target")) {
            try {
                targetCountryId = UUID.fromString(compound.getString("target"));
            } catch (IllegalArgumentException ignored) {
                targetCountryId = null;
            }
        }
        projectedRate = compound.getDouble("rate");
        projectedOutput = compound.getInteger("output");
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        NBTTagCompound tag = new NBTTagCompound();
        writeToNBT(tag);
        return new SPacketUpdateTileEntity(pos, 1, tag);
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        readFromNBT(pkt.getNbtCompound());
    }
}
