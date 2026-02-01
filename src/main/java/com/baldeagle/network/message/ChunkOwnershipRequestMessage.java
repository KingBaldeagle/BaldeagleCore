package com.baldeagle.network.message;

import com.baldeagle.chunkmap.ChunkMapConstants;
import com.baldeagle.chunkmap.ChunkOwnershipInfo;
import com.baldeagle.chunkmap.ChunkRelation;
import com.baldeagle.country.Country;
import com.baldeagle.country.CountryManager;
import com.baldeagle.network.NetworkHandler;
import com.baldeagle.territory.TerritoryData;
import com.baldeagle.territory.TerritoryEconomy;
import com.baldeagle.territory.TerritoryManager;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class ChunkOwnershipRequestMessage implements IMessage {

    private int centerChunkX;
    private int centerChunkZ;
    private int radius;

    public ChunkOwnershipRequestMessage() {}

    public ChunkOwnershipRequestMessage(
        int centerChunkX,
        int centerChunkZ,
        int radius
    ) {
        this.centerChunkX = centerChunkX;
        this.centerChunkZ = centerChunkZ;
        this.radius = radius;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(centerChunkX);
        buf.writeInt(centerChunkZ);
        buf.writeByte(radius);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        centerChunkX = buf.readInt();
        centerChunkZ = buf.readInt();
        radius = buf.readUnsignedByte();
    }

    public static class Handler
        implements IMessageHandler<ChunkOwnershipRequestMessage, IMessage>
    {

        @Override
        public IMessage onMessage(
            ChunkOwnershipRequestMessage message,
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
            ChunkOwnershipRequestMessage message
        ) {
            if (player == null || world == null || message == null) {
                return;
            }

            int radius = Math.max(
                1,
                Math.min(ChunkMapConstants.MAX_RADIUS, message.radius)
            );

            Country playerCountry = CountryManager.getCountryForPlayer(
                world,
                player.getUniqueID()
            );

            // Used for tooltip income numbers.
            Map<UUID, Integer> claimCounts = TerritoryManager.getClaimCounts(
                world.getMinecraftServer()
            );

            List<ChunkOwnershipInfo> infos = new ArrayList<>();

            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    int cx = message.centerChunkX + dx;
                    int cz = message.centerChunkZ + dz;
                    ChunkPos chunkPos = new ChunkPos(cx, cz);
                    TerritoryData.ClaimEntry claim = TerritoryManager.getClaim(
                        world,
                        chunkPos
                    );

                    if (claim == null) {
                        infos.add(
                            new ChunkOwnershipInfo(
                                cx,
                                cz,
                                null,
                                "",
                                ChunkRelation.NEUTRAL,
                                0L
                            )
                        );
                        continue;
                    }

                    UUID ownerId = claim.countryId;
                    Country owner = CountryManager.getCountry(world, ownerId);

                    ChunkRelation relation;
                    if (playerCountry == null) {
                        relation = ChunkRelation.NEUTRAL;
                    } else if (ownerId.equals(playerCountry.getId())) {
                        relation = ChunkRelation.OWNED;
                    } else if (playerCountry.isAlliedWith(ownerId)) {
                        relation = ChunkRelation.ALLIED;
                    } else if (playerCountry.isAtWarWith(ownerId)) {
                        relation = ChunkRelation.HOSTILE;
                    } else {
                        relation = ChunkRelation.NEUTRAL;
                    }

                    long incomePerDay = 0L;
                    Integer count = claimCounts.get(ownerId);
                    if (count != null && count > 0) {
                        long total = TerritoryEconomy.calculateIncome(count);
                        incomePerDay =
                            total > 0 ? Math.max(0L, total / count) : 0L;
                    }

                    infos.add(
                        new ChunkOwnershipInfo(
                            cx,
                            cz,
                            ownerId,
                            owner != null ? owner.getName() : "",
                            relation,
                            incomePerDay
                        )
                    );
                }
            }

            NetworkHandler.INSTANCE.sendTo(
                new ChunkOwnershipSnapshotMessage(
                    world.provider.getDimension(),
                    infos
                ),
                player
            );
        }
    }
}
