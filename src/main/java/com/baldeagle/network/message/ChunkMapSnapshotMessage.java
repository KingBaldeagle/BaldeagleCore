package com.baldeagle.network.message;

import com.baldeagle.chunkmap.ChunkTerrainSnapshot;
import com.baldeagle.chunkmap.ClientChunkMapCache;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class ChunkMapSnapshotMessage implements IMessage {

    private int dimension;
    private int grid;
    private List<ChunkTerrainSnapshot> snapshots;

    public ChunkMapSnapshotMessage() {}

    public ChunkMapSnapshotMessage(
        int dimension,
        int grid,
        List<ChunkTerrainSnapshot> snapshots
    ) {
        this.dimension = dimension;
        this.grid = grid;
        this.snapshots = snapshots != null ? snapshots : new ArrayList<>();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(dimension);
        buf.writeByte(grid);
        buf.writeInt(snapshots.size());
        for (ChunkTerrainSnapshot s : snapshots) {
            buf.writeInt(s.chunkX);
            buf.writeInt(s.chunkZ);
            int samples = s.grid * s.grid;
            for (int i = 0; i < samples; i++) {
                buf.writeByte(s.heights[i]);
            }
            for (int i = 0; i < samples; i++) {
                buf.writeInt(s.stateIds[i]);
            }
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        dimension = buf.readInt();
        grid = buf.readUnsignedByte();
        int count = buf.readInt();
        snapshots = new ArrayList<>(Math.max(0, count));
        for (int n = 0; n < count; n++) {
            int cx = buf.readInt();
            int cz = buf.readInt();
            int samples = grid * grid;
            byte[] heights = new byte[samples];
            int[] stateIds = new int[samples];
            for (int i = 0; i < samples; i++) {
                heights[i] = buf.readByte();
            }
            for (int i = 0; i < samples; i++) {
                stateIds[i] = buf.readInt();
            }
            snapshots.add(
                new ChunkTerrainSnapshot(cx, cz, grid, heights, stateIds)
            );
        }
    }

    public static class Handler
        implements IMessageHandler<ChunkMapSnapshotMessage, IMessage>
    {

        @Override
        public IMessage onMessage(
            ChunkMapSnapshotMessage message,
            MessageContext ctx
        ) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                if (message == null || message.snapshots == null) {
                    return;
                }
                ClientChunkMapCache.putAll(
                    message.dimension,
                    message.grid,
                    message.snapshots
                );
            });
            return null;
        }
    }
}
