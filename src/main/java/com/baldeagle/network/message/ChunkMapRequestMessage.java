package com.baldeagle.network.message;

import com.baldeagle.chunkmap.ChunkMapConstants;
import com.baldeagle.chunkmap.ChunkTerrainSnapshot;
import com.baldeagle.chunkmap.ChunkTerrainSnapshotCache;
import com.baldeagle.network.NetworkHandler;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class ChunkMapRequestMessage implements IMessage {

    private int centerChunkX;
    private int centerChunkZ;
    private int radius;
    private int grid;

    public ChunkMapRequestMessage() {}

    public ChunkMapRequestMessage(
        int centerChunkX,
        int centerChunkZ,
        int radius,
        int grid
    ) {
        this.centerChunkX = centerChunkX;
        this.centerChunkZ = centerChunkZ;
        this.radius = radius;
        this.grid = grid;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(centerChunkX);
        buf.writeInt(centerChunkZ);
        buf.writeByte(radius);
        buf.writeByte(grid);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        centerChunkX = buf.readInt();
        centerChunkZ = buf.readInt();
        radius = buf.readUnsignedByte();
        grid = buf.readUnsignedByte();
    }

    public static class Handler
        implements IMessageHandler<ChunkMapRequestMessage, IMessage>
    {

        @Override
        public IMessage onMessage(
            ChunkMapRequestMessage message,
            MessageContext ctx
        ) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            if (player == null) {
                return null;
            }
            WorldServer world = player.getServerWorld();
            world.addScheduledTask(() -> handle(player, world, message));
            return null;
        }

        private void handle(
            EntityPlayerMP player,
            WorldServer world,
            ChunkMapRequestMessage message
        ) {
            if (player == null || world == null || message == null) {
                return;
            }

            int radius = Math.max(
                1,
                Math.min(ChunkMapConstants.MAX_RADIUS, message.radius)
            );
            int grid = Math.max(
                1,
                Math.min(ChunkMapConstants.MAX_GRID, message.grid)
            );

            List<ChunkTerrainSnapshot> snapshots = new ArrayList<>();
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    int cx = message.centerChunkX + dx;
                    int cz = message.centerChunkZ + dz;
                    ChunkTerrainSnapshot snap =
                        ChunkTerrainSnapshotCache.getOrSample(
                            world,
                            cx,
                            cz,
                            grid
                        );
                    if (snap != null) {
                        snapshots.add(snap);
                    }
                }
            }

            NetworkHandler.INSTANCE.sendTo(
                new ChunkMapSnapshotMessage(
                    world.provider.getDimension(),
                    grid,
                    snapshots
                ),
                player
            );
        }
    }
}
