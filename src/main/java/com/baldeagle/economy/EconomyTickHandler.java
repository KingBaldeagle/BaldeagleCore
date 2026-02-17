package com.baldeagle.economy;

import com.baldeagle.config.BaldeagleConfig;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class EconomyTickHandler {

    private static final long TICKS_PER_MINUTE = 20L * 60L;

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

        long intervalTicks = BaldeagleConfig.interestIntervalMinutes * TICKS_PER_MINUTE;
        EconomyData data = EconomyData.get(world);
        long now = world.getTotalWorldTime();
        long last = data.getLastInterestTime();

        if (last <= 0L) {
            data.setLastInterestTime(now);
            data.markDirty();
            return;
        }

        long elapsed = now - last;
        if (elapsed < intervalTicks) {
            return;
        }

        long intervals = elapsed / intervalTicks;
        for (long i = 0; i < intervals; i++) {
            EconomyManager.applyInterest(world, BaldeagleConfig.countryInterestRate, BaldeagleConfig.playerInterestRate);
        }

        data.setLastInterestTime(last + intervals * intervalTicks);
        data.markDirty();
    }
}
