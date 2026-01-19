package com.baldeagle;

import com.baldeagle.economy.CommandEconomy;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;

public class ServerProxy {
    public void preInit() {}

    public void serverLoad(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandEconomy());
    }

}
