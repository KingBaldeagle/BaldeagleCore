package com.baldeagle.network.message;

import com.baldeagle.blocks.research.tile.TileEntityResearchAssembler;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class ResearchAssemblerSyncMessage implements IMessage {

    private BlockPos pos;
    private long storedCredits;
    private int tierOrdinal;
    private String ownerName;
    private String countryName;
    private double exchangeRate;
    private double inflationModifier;
    private long convertedCredits;
    private String status;

    public ResearchAssemblerSyncMessage() {}

    public ResearchAssemblerSyncMessage(
        BlockPos pos,
        long storedCredits,
        int tierOrdinal,
        String ownerName,
        String countryName,
        double exchangeRate,
        double inflationModifier,
        long convertedCredits,
        String status
    ) {
        this.pos = pos;
        this.storedCredits = storedCredits;
        this.tierOrdinal = tierOrdinal;
        this.ownerName = ownerName != null ? ownerName : "";
        this.countryName = countryName != null ? countryName : "";
        this.exchangeRate = exchangeRate;
        this.inflationModifier = inflationModifier;
        this.convertedCredits = convertedCredits;
        this.status = status != null ? status : "";
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(pos.getX());
        buf.writeInt(pos.getY());
        buf.writeInt(pos.getZ());
        buf.writeLong(storedCredits);
        buf.writeInt(tierOrdinal);
        ByteBufUtils.writeUTF8String(buf, ownerName != null ? ownerName : "");
        ByteBufUtils.writeUTF8String(
            buf,
            countryName != null ? countryName : ""
        );
        buf.writeDouble(exchangeRate);
        buf.writeDouble(inflationModifier);
        buf.writeLong(convertedCredits);
        ByteBufUtils.writeUTF8String(buf, status != null ? status : "");
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int x = buf.readInt();
        int y = buf.readInt();
        int z = buf.readInt();
        pos = new BlockPos(x, y, z);
        storedCredits = buf.readLong();
        tierOrdinal = buf.readInt();
        ownerName = ByteBufUtils.readUTF8String(buf);
        countryName = ByteBufUtils.readUTF8String(buf);
        exchangeRate = buf.readDouble();
        inflationModifier = buf.readDouble();
        convertedCredits = buf.readLong();
        status = ByteBufUtils.readUTF8String(buf);
    }

    public static class Handler
        implements IMessageHandler<ResearchAssemblerSyncMessage, IMessage>
    {

        @Override
        public IMessage onMessage(
            ResearchAssemblerSyncMessage message,
            MessageContext ctx
        ) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                if (message == null) {
                    return;
                }
                TileEntity tile = Minecraft.getMinecraft().world.getTileEntity(
                    message.pos
                );
                if (!(tile instanceof TileEntityResearchAssembler)) {
                    return;
                }
                ((TileEntityResearchAssembler) tile).applySync(
                    message.storedCredits,
                    message.tierOrdinal,
                    message.ownerName,
                    message.countryName,
                    message.exchangeRate,
                    message.inflationModifier,
                    message.convertedCredits,
                    message.status
                );
            });
            return null;
        }
    }
}
