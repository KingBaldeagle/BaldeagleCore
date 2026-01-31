package com.baldeagle;

import com.baldeagle.chunkmap.client.ChunkMapKeyHandler;
import com.baldeagle.chunkmap.client.ChunkMapKeybinds;
import com.baldeagle.chunkmap.client.InventoryMapButtonHandler;
import net.minecraftforge.common.MinecraftForge;

public class ClientProxy extends ServerProxy {

    @Override
    public void preInit() {
        ChunkMapKeybinds.register();
        MinecraftForge.EVENT_BUS.register(new ChunkMapKeyHandler());
        MinecraftForge.EVENT_BUS.register(new InventoryMapButtonHandler());
    }
}
