package com.baldeagle.network.message;

import com.baldeagle.blocks.mint.tile.TileEntityMint;
import com.baldeagle.country.currency.CurrencyDenomination;
import com.baldeagle.country.currency.CurrencyType;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.*;

public class MintSyncMessage implements IMessage {

    private BlockPos pos;
    private CurrencyType type;
    private CurrencyDenomination denomination;
    private int amount;
    private double inflation;
    private long circulation;

    public MintSyncMessage() {}

    public MintSyncMessage(
        BlockPos pos,
        CurrencyType type,
        CurrencyDenomination denomination,
        int amount,
        double inflation,
        long circulation
    ) {
        this.pos = pos;
        this.type = type;
        this.denomination = denomination;
        this.amount = amount;
        this.inflation = inflation;
        this.circulation = circulation;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(pos.toLong());
        buf.writeByte(type.ordinal());
        buf.writeByte(denomination.ordinal());
        buf.writeInt(amount);
        buf.writeDouble(inflation);
        buf.writeLong(circulation);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        pos = BlockPos.fromLong(buf.readLong());
        type = CurrencyType.values()[buf.readByte()];
        denomination = CurrencyDenomination.values()[buf.readByte()];
        amount = buf.readInt();
        inflation = buf.readDouble();
        circulation = buf.readLong();
    }

    public static class Handler
        implements IMessageHandler<MintSyncMessage, IMessage>
    {

        @Override
        public IMessage onMessage(MintSyncMessage message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                TileEntity tile = Minecraft.getMinecraft().world.getTileEntity(
                    message.pos
                );
                if (tile instanceof TileEntityMint) {
                    ((TileEntityMint) tile).applySync(
                        message.type,
                        message.denomination,
                        message.amount,
                        message.inflation,
                        message.circulation
                    );
                }
            });
            return null;
        }
    }
}
