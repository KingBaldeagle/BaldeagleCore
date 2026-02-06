package com.baldeagle.network.message;

import com.baldeagle.blocks.bank.TileEntityBank;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class BankSyncMessage implements IMessage {

    private BlockPos pos;
    private long playerBalance;
    private long countryBalance;

    public BankSyncMessage() {}

    public BankSyncMessage(BlockPos pos, long playerBalance, long countryBalance) {
        this.pos = pos;
        this.playerBalance = playerBalance;
        this.countryBalance = countryBalance;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(pos.toLong());
        buf.writeLong(playerBalance);
        buf.writeLong(countryBalance);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        pos = BlockPos.fromLong(buf.readLong());
        playerBalance = buf.readLong();
        countryBalance = buf.readLong();
    }

    public static class Handler implements IMessageHandler<BankSyncMessage, IMessage> {

        @Override
        public IMessage onMessage(BankSyncMessage message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                TileEntity tile = Minecraft.getMinecraft().world.getTileEntity(message.pos);
                if (tile instanceof TileEntityBank) {
                    ((TileEntityBank) tile).applyClientBalanceSync(
                        message.playerBalance,
                        message.countryBalance
                    );
                }
            });
            return null;
        }
    }
}
