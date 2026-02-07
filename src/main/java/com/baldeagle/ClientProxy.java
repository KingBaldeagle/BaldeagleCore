package com.baldeagle;

import com.baldeagle.chunkmap.client.InventoryMapButtonHandler;
import net.minecraftforge.common.MinecraftForge;

public class ClientProxy extends ServerProxy {

    @Override
    public void preInit() {
        MinecraftForge.EVENT_BUS.register(new InventoryMapButtonHandler());
        MinecraftForge.EVENT_BUS.register(new CustomSplashHandler());
    }
}
