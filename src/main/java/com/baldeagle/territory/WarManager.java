package com.baldeagle.territory;

import com.baldeagle.country.Country;
import com.baldeagle.country.CountryManager;
import java.util.UUID;
import net.minecraft.world.World;

public final class WarManager {

    private WarManager() {}

    /**
     * Placeholder until a proper war system exists.
     * For now, allow hostile flag capture by any other country.
     */
    public static boolean canCapture(
        UUID attackerCountryId,
        UUID defenderCountryId,
        World world
    ) {
        if (attackerCountryId == null || defenderCountryId == null) {
            return false;
        }
        if (attackerCountryId.equals(defenderCountryId)) {
            return false;
        }
        Country defender = CountryManager.getCountry(world, defenderCountryId);
        if (defender != null && defender.isAlliedWith(attackerCountryId)) {
            return false;
        }
        return true;
    }
}
