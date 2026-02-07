package com.baldeagle.integration.ar;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;

public final class AdvancedRocketryIntegration {

    private static final ARStationEventHandler STATION_HANDLER =
        new ARStationEventHandler();

    private AdvancedRocketryIntegration() {}

    public static void init() {
        MinecraftForge.EVENT_BUS.register(STATION_HANDLER);
    }

    public static void onServerLoad(FMLServerStartingEvent event) {
        STATION_HANDLER.onServerLoad(event.getServer());
    }
}
