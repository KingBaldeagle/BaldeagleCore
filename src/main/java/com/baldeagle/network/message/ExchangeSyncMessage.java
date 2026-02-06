package com.baldeagle.network.message;

import com.baldeagle.blocks.currency_exchange.tile.TileEntityCurrencyExchange;
import io.netty.buffer.ByteBuf;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.*;

public class ExchangeSyncMessage implements IMessage {

    private BlockPos pos;
    private UUID target;
    private String targetName;
    private double rate;
    private int output;

    public ExchangeSyncMessage() {}

    public ExchangeSyncMessage(
        BlockPos pos,
        UUID target,
        String targetName,
        double rate,
        int output
    ) {
        this.pos = pos;
        this.target = target;
        this.targetName = targetName;
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
        buf.writeBoolean(targetName != null);
        if (targetName != null) {
            ByteBufUtils.writeUTF8String(buf, targetName);
        }
        buf.writeDouble(rate);
        buf.writeInt(output);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        pos = BlockPos.fromLong(buf.readLong());
        target = null;
        if (buf.readBoolean()) {
            target = new UUID(buf.readLong(), buf.readLong());
        }
        targetName = null;
        if (buf.readBoolean()) {
            targetName = ByteBufUtils.readUTF8String(buf);
        }
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
                        message.targetName,
                        message.rate,
                        message.output
                    );
                }
            });
            return null;
        }
    }
}
