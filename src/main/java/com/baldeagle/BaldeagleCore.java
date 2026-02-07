package com.baldeagle;

import com.baldeagle.blocks.bank.TileEntityBank;
import com.baldeagle.blocks.currency_exchange.tile.TileEntityCurrencyExchange;
import com.baldeagle.blocks.mint.tile.TileEntityMint;
import com.baldeagle.blocks.research.tile.TileEntityResearchAssembler;
import com.baldeagle.blocks.shop.TileEntityShop;
import com.baldeagle.blocks.vault.tile.TileEntityVault;
import com.baldeagle.country.BountyEventHandler;
import com.baldeagle.economy.EconomyTickHandler;
import com.baldeagle.economy.atm.TileEntityAtm;
import com.baldeagle.network.NetworkHandler;
import com.baldeagle.oc.gov.TileEntityGovernmentComputer;
import com.baldeagle.territory.TerritoryIncomeTickHandler;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(
    modid = BaldeagleCore.MODID,
    name = BaldeagleCore.NAME,
    version = BaldeagleCore.VERSION,
    acceptableRemoteVersions = "*",
    dependencies = "required-after:forge@[14.23.5.2859,);" +
        "after:ftblib;" +
        "after:ftbutilities;" +
        "after:crafttweaker;" +
        "after:kubejs;" +
        "after:advancedrocketry;" +
        "after:libvulpes"
)
public class BaldeagleCore {

    public static final String MODID = "baldeaglecore";
    public static final String NAME = "BaldEagle Core";
    public static final String VERSION = "1.1";

    @Mod.Instance
    public static BaldeagleCore instance;

    @SidedProxy(
        clientSide = "com.baldeagle.ClientProxy",
        serverSide = "com.baldeagle.ServerProxy"
    )
    public static ServerProxy proxy;

    public static final Logger LOGGER = LogManager.getLogger(MODID);

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER.info("The Eagle is taking off");
        com.baldeagle.config.BaldeagleConfig.init(event);

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new BountyEventHandler());
        MinecraftForge.EVENT_BUS.register(new EconomyTickHandler());
        MinecraftForge.EVENT_BUS.register(new TerritoryIncomeTickHandler());
        NetworkRegistry.INSTANCE.registerGuiHandler(this, new GuiHandler());
        NetworkHandler.register();
        GameRegistry.registerTileEntity(
            TileEntityBank.class,
            new ResourceLocation(MODID, "bank")
        );
        GameRegistry.registerTileEntity(
            TileEntityMint.class,
            new ResourceLocation(MODID, "mint")
        );
        GameRegistry.registerTileEntity(
            TileEntityCurrencyExchange.class,
            new ResourceLocation(MODID, "currency_exchange")
        );
        GameRegistry.registerTileEntity(
            TileEntityVault.class,
            new ResourceLocation(MODID, "vault")
        );
        GameRegistry.registerTileEntity(
            TileEntityAtm.class,
            new ResourceLocation(MODID, "atm")
        );
        GameRegistry.registerTileEntity(
            TileEntityShop.class,
            new ResourceLocation(MODID, "shop")
        );
        GameRegistry.registerTileEntity(
            TileEntityGovernmentComputer.class,
            new ResourceLocation(MODID, "government_computer")
        );
        GameRegistry.registerTileEntity(
            TileEntityResearchAssembler.class,
            new ResourceLocation(MODID, "research_assembler")
        );
        proxy.preInit();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        LOGGER.info("The Eagle is in flight");
        if (Loader.isModLoaded("opencomputers")) {
            com.baldeagle.oc.OCIntegration.init();
        }
        if (Loader.isModLoaded("advancedrocketry")) {
            com.baldeagle.integration.ar.AdvancedRocketryIntegration.init();
        }
    }

    @Mod.EventHandler
    public void postinit(FMLInitializationEvent event) {
        LOGGER.info("The Eagle has landed");
    }

    @Mod.EventHandler
    public void serverLoad(FMLServerStartingEvent event) {
        LOGGER.info("The Eagle has been deployed");
        event.registerServerCommand(new com.baldeagle.country.CountryCommand());
        // Ensure country storage is initialized on the overworld, and migrate any legacy
        // per-dimension country data into the overworld store.
        net.minecraft.world.World overworld = event.getServer().getWorld(0);
        com.baldeagle.country.CountryStorage overworldStorage =
            com.baldeagle.country.CountryStorage.get(overworld);
        boolean migrated = false;
        for (net.minecraft.world.World w : event.getServer().worlds) {
            if (w == null || w.provider.getDimension() == 0) {
                continue;
            }
            com.baldeagle.country.CountryStorage other =
                com.baldeagle.country.CountryStorage.get(w);
            for (java.util.Map.Entry<
                java.util.UUID,
                com.baldeagle.country.Country
            > e : other.getCountriesMap().entrySet()) {
                if (
                    !overworldStorage.getCountriesMap().containsKey(e.getKey())
                ) {
                    overworldStorage
                        .getCountriesMap()
                        .put(e.getKey(), e.getValue());
                    migrated = true;
                }
            }
        }
        if (migrated) {
            overworldStorage.markDirty();
        }
        if (Loader.isModLoaded("advancedrocketry")) {
            com.baldeagle.integration.ar.AdvancedRocketryIntegration.onServerLoad(
                event
            );
        }
        proxy.serverLoad(event);
    }
}
