package com.baldeagle.integration.ar;

import com.baldeagle.BaldeagleCore;
import com.baldeagle.country.Country;
import com.baldeagle.country.CountryManager;
import com.baldeagle.country.CountryStorage;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import zmaster587.advancedRocketry.api.AdvancedRocketryItems;
import zmaster587.advancedRocketry.api.stations.ISpaceObject;
import zmaster587.advancedRocketry.item.ItemStationChip;
import zmaster587.advancedRocketry.stations.SpaceObjectManager;
import zmaster587.advancedRocketry.stations.SpaceStationObject;
import zmaster587.advancedRocketry.tile.TileStationAssembler;

public class ARStationEventHandler {

    private static final String DENIED_MESSAGE =
        "Your country is not authorized to create a space station. " +
        "Complete the Orbital Authorization quest to unlock one.";

    private static final Logger LOGGER = LogManager.getLogger(
        BaldeagleCore.MODID + "-advancedrocketry"
    );

    private final Map<AssemblerKey, UUID> assemblerUsers = new HashMap<>();
    private final Set<Integer> knownStations = new HashSet<>();

    public void onServerLoad(MinecraftServer server) {
        if (server == null) {
            return;
        }
        knownStations.clear();
        knownStations.addAll(getCurrentStationIds());
        validateStations(server);
    }

    @SubscribeEvent
    public void onAssemblerInteract(PlayerInteractEvent.RightClickBlock event) {
        if (event.getWorld().isRemote || event.getHand() != EnumHand.MAIN_HAND) {
            return;
        }
        TileEntity tile = event.getWorld().getTileEntity(event.getPos());
        if (!(tile instanceof TileStationAssembler)) {
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) event.getEntityPlayer();
        assemblerUsers.put(
            new AssemblerKey(event.getWorld(), event.getPos()),
            player.getUniqueID()
        );

        Country country = CountryManager.getCountryForPlayer(
            event.getWorld(),
            player.getUniqueID()
        );
        if (country == null || !country.canCreateStation()) {
            player.sendMessage(new TextComponentString(DENIED_MESSAGE));
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        MinecraftServer server = FMLCommonHandler
            .instance()
            .getMinecraftServerInstance();
        if (server == null) {
            return;
        }
        Set<Integer> currentStationIds = getCurrentStationIds();
        Set<Integer> removed = new HashSet<>(knownStations);
        removed.removeAll(currentStationIds);
        for (Integer stationId : removed) {
            handleStationRemoved(server, stationId);
            knownStations.remove(stationId);
        }

        for (Integer stationId : currentStationIds) {
            if (knownStations.contains(stationId)) {
                continue;
            }
            handleStationCreated(server, stationId);
            knownStations.add(stationId);
        }
    }

    private Set<Integer> getCurrentStationIds() {
        Set<Integer> ids = new HashSet<>();
        for (ISpaceObject spaceObject : SpaceObjectManager
            .getSpaceManager()
            .getSpaceObjects()) {
            if (spaceObject instanceof SpaceStationObject) {
                ids.add(spaceObject.getId());
            }
        }
        return ids;
    }

    private void handleStationCreated(MinecraftServer server, int stationId) {
        TileStationAssembler assembler = findAssemblerForStation(
            server,
            stationId
        );
        if (assembler == null) {
            LOGGER.warn(
                "Detected station {} creation, but no assembler could be matched.",
                stationId
            );
            return;
        }
        AssemblerKey key = new AssemblerKey(
            assembler.getWorld(),
            assembler.getPos()
        );
        UUID creator = assemblerUsers.get(key);
        if (creator == null) {
            LOGGER.warn(
                "Station {} creation has no tracked creator. Skipping authorization.",
                stationId
            );
            return;
        }
        World countryWorld = server.getWorld(0);
        Country country = CountryManager.getCountryForPlayer(
            countryWorld,
            creator
        );
        if (country == null || !country.canCreateStation()) {
            denyStationCreation(server, assembler, creator, stationId);
            return;
        }
        CountryStorage storage = CountryStorage.get(countryWorld);
        country.onStationCreated();
        storage.getStationOwners().put(stationId, country.getId());
        storage.markDirty();
    }

    private void denyStationCreation(
        MinecraftServer server,
        TileStationAssembler assembler,
        UUID creator,
        int stationId
    ) {
        SpaceObjectManager.getSpaceManager().unregisterSpaceObject(stationId);
        if (assembler instanceof IInventory) {
            IInventory inv = (IInventory) assembler;
            inv.setInventorySlotContents(2, ItemStack.EMPTY);
            inv.setInventorySlotContents(3, ItemStack.EMPTY);
        }
        assembler.markDirty();
        World world = assembler.getWorld();
        world.notifyBlockUpdate(
            assembler.getPos(),
            world.getBlockState(assembler.getPos()),
            world.getBlockState(assembler.getPos()),
            3
        );
        EntityPlayerMP player = server
            .getPlayerList()
            .getPlayerByUUID(creator);
        if (player != null) {
            player.sendMessage(new TextComponentString(DENIED_MESSAGE));
        }
    }

    private void handleStationRemoved(MinecraftServer server, int stationId) {
        World countryWorld = server.getWorld(0);
        CountryStorage storage = CountryStorage.get(countryWorld);
        UUID owner = storage.getStationOwners().remove(stationId);
        if (owner == null) {
            return;
        }
        Country country = storage.getCountriesMap().get(owner);
        if (country != null) {
            country.onStationRemoved();
            storage.markDirty();
        }
    }

    private TileStationAssembler findAssemblerForStation(
        MinecraftServer server,
        int stationId
    ) {
        for (World world : server.worlds) {
            if (world == null || world.isRemote) {
                continue;
            }
            for (TileEntity tile : world.loadedTileEntityList) {
                if (!(tile instanceof TileStationAssembler)) {
                    continue;
                }
                IInventory inv = (IInventory) tile;
                ItemStack output = inv.getStackInSlot(2);
                if (output.isEmpty()) {
                    continue;
                }
                if (output.getItem() != AdvancedRocketryItems.itemSpaceStation) {
                    continue;
                }
                int id = ItemStationChip.getUUID(output);
                if (id == stationId) {
                    return (TileStationAssembler) tile;
                }
            }
        }
        return null;
    }

    private void validateStations(MinecraftServer server) {
        World countryWorld = server.getWorld(0);
        CountryStorage storage = CountryStorage.get(countryWorld);
        Set<Integer> currentStations = getCurrentStationIds();
        storage
            .getStationOwners()
            .entrySet()
            .removeIf(entry -> !currentStations.contains(entry.getKey()));

        Map<UUID, Integer> counts = new HashMap<>();
        for (UUID owner : storage.getStationOwners().values()) {
            counts.put(owner, counts.getOrDefault(owner, 0) + 1);
        }
        for (Country country : storage.getCountriesMap().values()) {
            int built = counts.getOrDefault(country.getId(), 0);
            country.setStationsBuilt(built);
            if (built > country.getStationCap()) {
                LOGGER.warn(
                    "Country {} exceeds station cap ({} of {}).",
                    country.getName(),
                    built,
                    country.getStationCap()
                );
            }
        }

        for (Integer stationId : currentStations) {
            if (!storage.getStationOwners().containsKey(stationId)) {
                LOGGER.warn(
                    "Station {} has no country owner mapping.",
                    stationId
                );
            }
        }
        storage.markDirty();
    }

    private static final class AssemblerKey {

        private final int dimensionId;
        private final long position;

        private AssemblerKey(World world, net.minecraft.util.math.BlockPos pos) {
            this.dimensionId = world.provider.getDimension();
            this.position = pos.toLong();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            AssemblerKey other = (AssemblerKey) obj;
            return (
                dimensionId == other.dimensionId && position == other.position
            );
        }

        @Override
        public int hashCode() {
            int result = Integer.hashCode(dimensionId);
            result = 31 * result + Long.hashCode(position);
            return result;
        }
    }
}
