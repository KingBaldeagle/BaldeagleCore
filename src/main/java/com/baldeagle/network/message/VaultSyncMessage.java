package com.baldeagle.network.message;

import com.baldeagle.country.vault.tile.TileEntityVault;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class VaultSyncMessage implements IMessage {

    private BlockPos pos;
    private long countryReserves;

    public VaultSyncMessage() {}

    public VaultSyncMessage(BlockPos pos, long countryReserves) {
        this.pos = pos;
        this.countryReserves = countryReserves;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(pos.toLong());
        buf.writeLong(countryReserves);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        pos = BlockPos.fromLong(buf.readLong());
        countryReserves = buf.readLong();
    }

    public static class Handler implements IMessageHandler<VaultSyncMessage, IMessage> {

        @Override
        public IMessage onMessage(VaultSyncMessage message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                TileEntity tile = Minecraft.getMinecraft().world.getTileEntity(message.pos);
                if (tile instanceof TileEntityVault) {
                    ((TileEntityVault) tile).applyCountryReserveSync(
                        message.countryReserves
                    );
                }
            });
            return null;
        }
    }
}
