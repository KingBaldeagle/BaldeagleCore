package com.baldeagle.economy;

import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class EconomyTickHandler {

    private static final long INTEREST_INTERVAL_TICKS = 20L * 60L;
    private static final double INTEREST_RATE = 0.01D;

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        World world = event.world;
        if (world == null || world.isRemote) {
            return;
        }
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (world.provider.getDimension() != 0) {
            return;
        }

        EconomyData data = EconomyData.get(world);
        long now = world.getTotalWorldTime();
        long last = data.getLastInterestTime();

        if (last <= 0L) {
            data.setLastInterestTime(now);
            data.markDirty();
            return;
        }

        long elapsed = now - last;
        if (elapsed < INTEREST_INTERVAL_TICKS) {
            return;
        }

        long intervals = elapsed / INTEREST_INTERVAL_TICKS;
        for (long i = 0; i < intervals; i++) {
            EconomyManager.applyInterest(world, INTEREST_RATE);
        }

        data.setLastInterestTime(last + intervals * INTEREST_INTERVAL_TICKS);
        data.markDirty();
    }
}
