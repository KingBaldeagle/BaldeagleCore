package com.baldeagle;

import com.baldeagle.country.CountryCommand;
import com.baldeagle.country.CountryJoinCommand;
import com.baldeagle.country.CountryMoneyCommand;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;


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

    @SidedProxy(clientSide = "com.baldeagle.ClientProxy",
            serverSide = "com.baldeagle.ServerProxy")
    public static ServerProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
        proxy.preInit();
    }



    @Mod.EventHandler
    public void serverLoad(FMLServerStartingEvent event) {
        proxy.serverLoad(event);
        event.registerServerCommand(new CountryJoinCommand());
        event.registerServerCommand(new CountryMoneyCommand());
        event.registerServerCommand(new CountryCommand());
    }
}
