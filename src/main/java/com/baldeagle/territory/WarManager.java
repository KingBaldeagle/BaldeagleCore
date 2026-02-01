package com.baldeagle.territory;

import com.baldeagle.country.Country;
import com.baldeagle.country.CountryManager;
import java.util.UUID;
import net.minecraft.world.World;

public final class WarManager {

    private WarManager() {}

    /**
     * Placeholder until a proper war system exists.
     * Flag capture is only allowed while at war, and never against allies.
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
        if (defender == null) {
            return false;
        }
        if (defender.isAlliedWith(attackerCountryId)) {
            return false;
        }
        return defender.isAtWarWith(attackerCountryId);
    }
}
