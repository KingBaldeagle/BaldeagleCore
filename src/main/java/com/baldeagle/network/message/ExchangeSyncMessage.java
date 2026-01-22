package com.baldeagle.network.message;

import com.baldeagle.country.currency.CurrencyDenomination;
import com.baldeagle.country.mint.tile.TileEntityCurrencyExchange;
import io.netty.buffer.ByteBuf;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.*;

public class ExchangeSyncMessage implements IMessage {

    private BlockPos pos;
    private UUID target;
    private CurrencyDenomination denomination;
    private double rate;
    private int output;

    public ExchangeSyncMessage() {}

    public ExchangeSyncMessage(
        BlockPos pos,
        UUID target,
        CurrencyDenomination denomination,
        double rate,
        int output
    ) {
        this.pos = pos;
        this.target = target;
        this.denomination = denomination;
        this.rate = rate;
        this.output = output;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(pos.toLong());
        buf.writeBoolean(target != null);
        if (target != null) {
            buf.writeLong(target.getMostSignificantBits());
            buf.writeLong(target.getLeastSignificantBits());
        }
        buf.writeByte(denomination.ordinal());
        buf.writeDouble(rate);
        buf.writeInt(output);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        pos = BlockPos.fromLong(buf.readLong());
        if (buf.readBoolean()) {
            target = new UUID(buf.readLong(), buf.readLong());
        }
        denomination = CurrencyDenomination.values()[buf.readByte()];
        rate = buf.readDouble();
        output = buf.readInt();
    }

    public static class Handler
        implements IMessageHandler<ExchangeSyncMessage, IMessage>
    {

        @Override
        public IMessage onMessage(
            ExchangeSyncMessage message,
            MessageContext ctx
        ) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                TileEntity tile = Minecraft.getMinecraft().world.getTileEntity(
                    message.pos
                );
                if (tile instanceof TileEntityCurrencyExchange) {
                    ((TileEntityCurrencyExchange) tile).applySync(
                        message.target,
                        message.denomination,
                        message.rate,
                        message.output
                    );
                }
            });
            return null;
        }
    }
}
