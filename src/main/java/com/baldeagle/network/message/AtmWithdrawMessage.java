package com.baldeagle.network.message;

import com.baldeagle.country.Country;
import com.baldeagle.country.CountryManager;
import com.baldeagle.country.CountryStorage;
import com.baldeagle.country.currency.CurrencyDenomination;
import com.baldeagle.country.currency.CurrencyItemHelper;
import com.baldeagle.country.mint.MintingConstants;
import com.baldeagle.economy.EconomyManager;
import com.baldeagle.economy.atm.TileEntityAtm;
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

public class AtmWithdrawMessage implements IMessage {

    public enum Source {
        PLAYER,
        COUNTRY,
    }

    private BlockPos pos;
    private Source source;
    private long amount;

    public AtmWithdrawMessage() {}

    public AtmWithdrawMessage(BlockPos pos, Source source, long amount) {
        this.pos = pos;
        this.source = source;
        this.amount = amount;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(pos.toLong());
        buf.writeByte(source.ordinal());
        buf.writeLong(amount);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        pos = BlockPos.fromLong(buf.readLong());
        source = Source.values()[buf.readByte()];
        amount = buf.readLong();
    }

    public static class Handler
        implements IMessageHandler<AtmWithdrawMessage, IMessage>
    {

        @Override
        public IMessage onMessage(
            AtmWithdrawMessage message,
            MessageContext ctx
        ) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player
                .getServerWorld()
                .addScheduledTask(() -> {
                    TileEntity tile = player.world.getTileEntity(message.pos);
                    if (!(tile instanceof TileEntityAtm)) {
                        return;
                    }
                    handleWithdraw(player, message.source, message.amount);
                });
            return null;
        }

        private void handleWithdraw(
            EntityPlayerMP player,
            Source source,
            long amount
        ) {
            if (player == null || player.world == null) {
                return;
            }
            if (amount <= 0) {
                return;
            }

            Country country = CountryManager.getCountryForPlayer(
                player.world,
                player.getUniqueID()
            );
            if (country == null) {
                player.sendStatusMessage(
                    new net.minecraft.util.text.TextComponentString(
                        "Join a country first."
                    ),
                    true
                );
                return;
            }

            boolean success;
            if (source == Source.PLAYER) {
                success = EconomyManager.withdrawPlayer(
                    player.world,
                    player.getUniqueID(),
                    amount
                );
            } else {
                if (!country.isAuthorized(player.getUniqueID())) {
                    player.sendStatusMessage(
                        new net.minecraft.util.text.TextComponentString(
                            "You are not authorized."
                        ),
                        true
                    );
                    return;
                }
                success = EconomyManager.withdrawCountry(
                    player.world,
                    country.getName(),
                    amount
                );
            }

            if (!success) {
                player.sendStatusMessage(
                    new net.minecraft.util.text.TextComponentString(
                        "Insufficient funds."
                    ),
                    true
                );
                return;
            }

            long remaining = amount;
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

            // Withdrawal creates physical currency again (adds back into circulation).
            country.applyMinting(
                amount,
                MintingConstants.MINT_INFLATION_FACTOR
            );
            CountryStorage.get(player.world).markDirty();

            player.sendStatusMessage(
                new net.minecraft.util.text.TextComponentString(
                    "Withdrew " +
                        amount +
                        " from " +
                        (source == Source.PLAYER ? "personal" : "country") +
                        " balance."
                ),
                true
            );
        }
    }
}
