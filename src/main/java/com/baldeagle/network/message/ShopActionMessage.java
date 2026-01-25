package com.baldeagle.network.message;

import com.baldeagle.country.Country;
import com.baldeagle.country.CountryManager;
import com.baldeagle.country.CountryStorage;
import com.baldeagle.country.currency.CurrencyDenomination;
import com.baldeagle.country.currency.CurrencyItemHelper;
import com.baldeagle.economy.EconomyManager;
import com.baldeagle.shop.TileEntityShop;
import io.netty.buffer.ByteBuf;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class ShopActionMessage implements IMessage {

    public enum Action {
        BUY,
        SET_PRICE,
        WITHDRAW,
    }

    private BlockPos pos;
    private Action action;
    private int slot;
    private long payload;

    public ShopActionMessage() {}

    public ShopActionMessage(BlockPos pos, Action action, int slot, long payload) {
        this.pos = pos;
        this.action = action;
        this.slot = slot;
        this.payload = payload;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(pos.toLong());
        buf.writeByte(action.ordinal());
        buf.writeInt(slot);
        buf.writeLong(payload);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        pos = BlockPos.fromLong(buf.readLong());
        action = Action.values()[buf.readByte()];
        slot = buf.readInt();
        payload = buf.readLong();
    }

    public static class Handler implements IMessageHandler<ShopActionMessage, IMessage> {

        private static final double TAX_RATE = 0.08D;

        @Override
        public IMessage onMessage(ShopActionMessage message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                TileEntity tile = player.world.getTileEntity(message.pos);
                if (!(tile instanceof TileEntityShop)) {
                    return;
                }
                TileEntityShop shop = (TileEntityShop) tile;
                switch (message.action) {
                    case SET_PRICE:
                        handleSetPrice(player, shop, message.slot, message.payload);
                        break;
                    case WITHDRAW:
                        handleWithdraw(player, shop);
                        break;
                    case BUY:
                        handleBuy(player, shop, message.slot);
                        break;
                }
            });
            return null;
        }

        private void handleSetPrice(
            EntityPlayerMP player,
            TileEntityShop shop,
            int slot,
            long price
        ) {
            if (!shop.isOwner(player)) {
                return;
            }
            shop.setPrice(slot, Math.max(0, price));
        }

        private void handleWithdraw(EntityPlayerMP player, TileEntityShop shop) {
            if (!shop.isOwner(player)) {
                return;
            }
            Country country = shop.getCountryId() != null
                ? CountryManager.getCountry(player.world, shop.getCountryId())
                : null;
            if (country == null) {
                player.sendStatusMessage(
                    new net.minecraft.util.text.TextComponentString("Shop has no country."),
                    true
                );
                return;
            }

            long cash = shop.withdrawAllCash();
            if (cash <= 0) {
                return;
            }

            spawnCurrency(player, country, cash);
            player.sendStatusMessage(
                new net.minecraft.util.text.TextComponentString("Withdrew " + cash + "."),
                true
            );
        }

        private void handleBuy(EntityPlayerMP buyer, TileEntityShop shop, int slot) {
            if (slot < 0 || slot >= TileEntityShop.SLOT_COUNT) {
                return;
            }

            synchronized (shop) {
                ItemStack forSale = shop.getStackInSlot(slot);
                long price = shop.getPrice(slot);
                if (forSale.isEmpty() || price <= 0) {
                    return;
                }

                Country shopCountry = shop.getCountryId() != null
                    ? CountryManager.getCountry(buyer.world, shop.getCountryId())
                    : null;
                if (shopCountry == null) {
                    buyer.sendStatusMessage(
                        new net.minecraft.util.text.TextComponentString("Shop has no country."),
                        true
                    );
                    return;
                }

                Country buyerCountry = CountryManager.getCountryForPlayer(
                    buyer.world,
                    buyer.getUniqueID()
                );
                if (buyerCountry == null || !shopCountry.getId().equals(buyerCountry.getId())) {
                    buyer.sendStatusMessage(
                        new net.minecraft.util.text.TextComponentString(
                            "You must use " + shopCountry.getName() + " currency."
                        ),
                        true
                    );
                    return;
                }

                boolean paid = EconomyManager.withdrawPlayer(
                    buyer.world,
                    buyer.getUniqueID(),
                    price
                );
                if (!paid) {
                    buyer.sendStatusMessage(
                        new net.minecraft.util.text.TextComponentString("Insufficient funds."),
                        true
                    );
                    return;
                }

                long tax = (long) Math.floor(price * TAX_RATE);
                long ownerReceives = Math.max(0, price - tax);

                shop.addCash(ownerReceives);
                if (tax > 0) {
                    EconomyManager.depositCountry(buyer.world, shopCountry.getName(), tax);
                    shopCountry.setBalance(shopCountry.getBalance() + tax);
                    CountryStorage.get(buyer.world).markDirty();
                }

                ItemStack purchased = forSale.copy();
                shop.setInventorySlotContents(slot, ItemStack.EMPTY);

                if (!buyer.inventory.addItemStackToInventory(purchased)) {
                    buyer.dropItem(purchased, false);
                }
                buyer.inventory.markDirty();

                buyer.sendStatusMessage(
                    new net.minecraft.util.text.TextComponentString(
                        "Purchased for " + price + " (tax " + tax + ")."
                    ),
                    true
                );
            }
        }

        private void spawnCurrency(EntityPlayerMP player, Country country, long amount) {
            long remaining = amount;
            List<CurrencyDenomination> denominations = Arrays.stream(
                CurrencyDenomination.values()
            )
                .sorted(Comparator.comparingInt(CurrencyDenomination::getValue).reversed())
                .collect(Collectors.toList());

            for (CurrencyDenomination denom : denominations) {
                long count = remaining / denom.getValue();
                if (count <= 0) continue;

                int maxStack =
                    denom.getType() == com.baldeagle.country.currency.CurrencyType.COIN
                        ? 64
                        : 16;
                while (count > 0) {
                    int give = (int) Math.min(count, (long) maxStack);
                    ItemStack stack = CurrencyItemHelper.createCurrencyStack(
                        country,
                        denom,
                        give
                    );
                    if (!stack.isEmpty()) {
                        if (!player.inventory.addItemStackToInventory(stack)) {
                            player.dropItem(stack, false);
                        }
                    }
                    count -= give;
                }

                remaining = remaining % denom.getValue();
                if (remaining <= 0) break;
            }
        }
    }
}
