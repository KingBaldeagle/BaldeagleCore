package com.baldeagle.territory;

import java.util.UUID;
import net.minecraft.world.World;

public final class WarManager {

    private WarManager() {}

    /**
     * Placeholder until a proper war system exists.
     * For now, allow hostile flag capture by any other country.
     */
    public static boolean canCapture(UUID attackerCountryId, UUID defenderCountryId, World world) {
        if (attackerCountryId == null || defenderCountryId == null) {
            return false;
        }
        return !attackerCountryId.equals(defenderCountryId);
    }
}
