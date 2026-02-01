package com.baldeagle.network.message;

import com.baldeagle.chunkmap.ChunkOwnershipInfo;
import com.baldeagle.chunkmap.ChunkRelation;
import com.baldeagle.chunkmap.ClientChunkOwnershipCache;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class ChunkOwnershipSnapshotMessage implements IMessage {

    private int dimension;
    private List<ChunkOwnershipInfo> infos;

    public ChunkOwnershipSnapshotMessage() {}

    public ChunkOwnershipSnapshotMessage(
        int dimension,
        List<ChunkOwnershipInfo> infos
    ) {
        this.dimension = dimension;
        this.infos = infos != null ? infos : new ArrayList<>();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(dimension);
        buf.writeInt(infos.size());
        for (ChunkOwnershipInfo info : infos) {
            buf.writeInt(info.chunkX);
            buf.writeInt(info.chunkZ);

            boolean hasOwner = info.ownerCountryId != null;
            buf.writeBoolean(hasOwner);
            if (hasOwner) {
                buf.writeLong(info.ownerCountryId.getMostSignificantBits());
                buf.writeLong(info.ownerCountryId.getLeastSignificantBits());
            }

            buf.writeByte(info.relation.ordinal());
            buf.writeLong(info.incomePerDay);
            ByteBufUtils.writeUTF8String(
                buf,
                info.ownerName != null ? info.ownerName : ""
            );
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        dimension = buf.readInt();
        int count = buf.readInt();
        infos = new ArrayList<>(Math.max(0, count));
        for (int i = 0; i < count; i++) {
            int cx = buf.readInt();
            int cz = buf.readInt();

            UUID ownerId = null;
            boolean hasOwner = buf.readBoolean();
            if (hasOwner) {
                ownerId = new UUID(buf.readLong(), buf.readLong());
            }

            int relOrd = buf.readUnsignedByte();
            ChunkRelation rel =
                relOrd < ChunkRelation.values().length
                    ? ChunkRelation.values()[relOrd]
                    : ChunkRelation.NEUTRAL;

            long incomePerDay = buf.readLong();
            String ownerName = ByteBufUtils.readUTF8String(buf);

            infos.add(
                new ChunkOwnershipInfo(
                    cx,
                    cz,
                    ownerId,
                    ownerName,
                    rel,
                    incomePerDay
                )
            );
        }
    }

    public static class Handler
        implements IMessageHandler<ChunkOwnershipSnapshotMessage, IMessage>
    {

        @Override
        public IMessage onMessage(
            ChunkOwnershipSnapshotMessage message,
            MessageContext ctx
        ) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                if (message == null) {
                    return;
                }
                ClientChunkOwnershipCache.putAll(
                    message.dimension,
                    message.infos
                );
            });
            return null;
        }
    }
}
