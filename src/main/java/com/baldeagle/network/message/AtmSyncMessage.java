package com.baldeagle.network.message;

import com.baldeagle.economy.atm.TileEntityAtm;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class AtmSyncMessage implements IMessage {

    private BlockPos pos;
    private String countryName;

    public AtmSyncMessage() {}

    public AtmSyncMessage(BlockPos pos, String countryName) {
        this.pos = pos;
        this.countryName = countryName;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(pos.toLong());
        buf.writeBoolean(countryName != null);
        if (countryName != null) {
            ByteBufUtils.writeUTF8String(buf, countryName);
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        pos = BlockPos.fromLong(buf.readLong());
        countryName = null;
        if (buf.readBoolean()) {
            countryName = ByteBufUtils.readUTF8String(buf);
        }
    }

    public static class Handler implements IMessageHandler<AtmSyncMessage, IMessage> {

        @Override
        public IMessage onMessage(AtmSyncMessage message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                TileEntity tile = Minecraft.getMinecraft().world.getTileEntity(message.pos);
                if (tile instanceof TileEntityAtm) {
                    ((TileEntityAtm) tile).applySync(message.countryName);
                }
            });
            return null;
        }
    }
}
