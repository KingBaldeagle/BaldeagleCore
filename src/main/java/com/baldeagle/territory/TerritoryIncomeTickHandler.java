package com.baldeagle.territory;

import com.baldeagle.country.Country;
import com.baldeagle.country.CountryStorage;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class TerritoryIncomeTickHandler {

    // "Daily" payout: once per Minecraft day (20 minutes / 24000 ticks).
    private static final long PAYOUT_INTERVAL_TICKS = 24000L;

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

        TerritoryData data = TerritoryData.get(world);
        long now = world.getTotalWorldTime();
        long last = data.getLastPayoutTime();

        if (last <= 0L) {
            data.setLastPayoutTime(now);
            data.markDirty();
            return;
        }

        long elapsed = now - last;
        if (elapsed < PAYOUT_INTERVAL_TICKS) {
            return;
        }

        long intervals = elapsed / PAYOUT_INTERVAL_TICKS;
        MinecraftServer server = world.getMinecraftServer();
        if (server != null) {
            for (long i = 0; i < intervals; i++) {
                payout(server);
            }
        }

        data.setLastPayoutTime(last + intervals * PAYOUT_INTERVAL_TICKS);
        data.markDirty();
    }

    private void payout(MinecraftServer server) {
        Map<UUID, Integer> counts = TerritoryManager.getClaimCounts(server);
        if (counts.isEmpty()) {
            return;
        }

        World overworld = server.getWorld(0);
        if (overworld == null) {
            return;
        }

        CountryStorage storage = CountryStorage.get(overworld);
        boolean changed = false;
        for (Map.Entry<UUID, Integer> entry : counts.entrySet()) {
            Country country = storage.getCountriesMap().get(entry.getKey());
            if (country == null) {
                continue;
            }
            long income = TerritoryEconomy.calculateIncome(entry.getValue());
            if (income <= 0) {
                continue;
            }
            country.setBalance(country.getBalance() + income);
            changed = true;
        }

        if (changed) {
            storage.markDirty();
        }
    }
}
