package com.baldeagle;

import com.baldeagle.bank.GuiHandler;
import com.baldeagle.bank.TileEntityBank;
import com.baldeagle.country.mint.tile.TileEntityCurrencyExchange;
import com.baldeagle.country.mint.tile.TileEntityMint;
import com.baldeagle.country.vault.tile.TileEntityVault;
import com.baldeagle.economy.EconomyTickHandler;
import com.baldeagle.economy.atm.TileEntityAtm;
import com.baldeagle.network.NetworkHandler;
import com.baldeagle.shop.TileEntityShop;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
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
    public static final String VERSION = "0.4";

    @Mod.Instance
    public static BaldeagleCore instance;

    @SidedProxy(
        clientSide = "com.baldeagle.ClientProxy",
        serverSide = "com.baldeagle.ServerProxy"
    )
    public static ServerProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new EconomyTickHandler());
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
        proxy.preInit();
    }

    @Mod.EventHandler
    public void serverLoad(FMLServerStartingEvent event) {
        event.registerServerCommand(new com.baldeagle.country.CountryCommand());
        com.baldeagle.country.CountryStorage.get(event.getServer().getWorld(0));
        proxy.serverLoad(event);
    }
}
