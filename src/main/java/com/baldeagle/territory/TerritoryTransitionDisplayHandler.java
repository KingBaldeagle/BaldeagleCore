package com.baldeagle.territory;

import com.baldeagle.country.Country;
import com.baldeagle.country.CountryManager;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

@Mod.EventBusSubscriber(modid = "baldeaglecore")
public class TerritoryTransitionDisplayHandler {

    private static final Map<UUID, LastKnownState> LAST_KNOWN = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        EntityPlayer player = event.player;
        if (!(player instanceof EntityPlayerMP)) {
            return;
        }

        World world = player.world;
        if (world == null || world.isRemote) {
            return;
        }

        int chunkX = player.chunkCoordX;
        int chunkZ = player.chunkCoordZ;
        int dimension = world.provider.getDimension();
        UUID playerId = player.getUniqueID();

        LastKnownState last = LAST_KNOWN.get(playerId);
        if (
            last != null &&
            last.dimension == dimension &&
            last.chunkX == chunkX &&
            last.chunkZ == chunkZ
        ) {
            return;
        }

        TerritoryView territory = resolveTerritory(world, playerId, chunkX, chunkZ);
        if (last == null || !last.territoryKey.equals(territory.key)) {
            player.sendStatusMessage(
                new TextComponentString(
                    TextFormatting.WHITE +
                    "Entering " +
                    territory.formattedName +
                    TextFormatting.RESET
                ),
                true
            );
        }

        LAST_KNOWN.put(
            playerId,
            new LastKnownState(dimension, chunkX, chunkZ, territory.key)
        );
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.player != null) {
            LAST_KNOWN.remove(event.player.getUniqueID());
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(
        PlayerEvent.PlayerChangedDimensionEvent event
    ) {
        if (event.player != null) {
            LAST_KNOWN.remove(event.player.getUniqueID());
        }
    }

    private static TerritoryView resolveTerritory(
        World world,
        UUID playerId,
        int chunkX,
        int chunkZ
    ) {
        ChunkPos chunk = new ChunkPos(chunkX, chunkZ);

        if (TerritoryManager.isChunkInSpawnProtection(world, chunk)) {
            return new TerritoryView("spawn", TextFormatting.GOLD + "Spawn");
        }

        TerritoryData.ClaimEntry claim = TerritoryManager.getClaim(world, chunk);
        if (claim != null) {
            Country owner = CountryManager.getCountry(world, claim.countryId);
            if (owner != null) {
                return new TerritoryView(
                    "country:" + owner.getId(),
                    getCountryColor(world, playerId, owner) + owner.getName()
                );
            }
        }

        return new TerritoryView(
            "wilderness",
            TextFormatting.GRAY + "Wilderness"
        );
    }

    private static TextFormatting getCountryColor(
        World world,
        UUID playerId,
        Country owner
    ) {
        Country playerCountry = CountryManager.getCountryForPlayer(world, playerId);

        if (playerCountry != null) {
            if (Objects.equals(playerCountry.getId(), owner.getId())) {
                return TextFormatting.GREEN;
            }
            if (owner.isAlliedWith(playerCountry.getId())) {
                return TextFormatting.BLUE;
            }
            if (owner.isAtWarWith(playerCountry.getId())) {
                return TextFormatting.RED;
            }
        }

        return TextFormatting.WHITE;
    }

    private static final class LastKnownState {

        private final int dimension;
        private final int chunkX;
        private final int chunkZ;
        private final String territoryKey;

        private LastKnownState(
            int dimension,
            int chunkX,
            int chunkZ,
            String territoryKey
        ) {
            this.dimension = dimension;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.territoryKey = territoryKey;
        }
    }

    private static final class TerritoryView {

        private final String key;
        private final String formattedName;

        private TerritoryView(String key, String formattedName) {
            this.key = key;
            this.formattedName = formattedName;
        }
    }
}
