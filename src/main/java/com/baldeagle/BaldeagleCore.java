package com.baldeagle;

import com.baldeagle.bank.GuiHandler;
import com.baldeagle.bank.TileEntityBank;
import com.baldeagle.country.mint.tile.TileEntityCurrencyExchange;
import com.baldeagle.country.mint.tile.TileEntityMint;
import com.baldeagle.country.vault.tile.TileEntityVault;
import com.baldeagle.economy.EconomyTickHandler;
import com.baldeagle.economy.atm.TileEntityAtm;
import com.baldeagle.network.NetworkHandler;
import com.baldeagle.oc.gov.TileEntityGovernmentComputer;
import com.baldeagle.research.tile.TileEntityResearchAssembler;
import com.baldeagle.shop.TileEntityShop;
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

@Mod(
    modid = BaldeagleCore.MODID,
    name = BaldeagleCore.NAME,
    version = BaldeagleCore.VERSION,
    acceptableRemoteVersions = "*"
)
public class BaldeagleCore {

    public static final String MODID = "baldeaglecore";
    public static final String NAME = "BaldEagle Core";
    public static final String VERSION = "1.0";

    @Mod.Instance
    public static BaldeagleCore instance;

    @SidedProxy(
        clientSide = "com.baldeagle.ClientProxy",
        serverSide = "com.baldeagle.ServerProxy"
    )
    public static ServerProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        com.baldeagle.config.BaldeagleConfig.init(event);
        MinecraftForge.EVENT_BUS.register(this);
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
        if (Loader.isModLoaded("opencomputers")) {
            com.baldeagle.oc.OCIntegration.init();
        }
    }

    @Mod.EventHandler
    public void serverLoad(FMLServerStartingEvent event) {
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
        proxy.serverLoad(event);
    }
}
