package com.baldeagle.territory;

import com.baldeagle.country.Country;
import com.baldeagle.country.CountryManager;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

public final class WarManager {

    public enum WarState {
        DECLARED,
        ACTIVE,
        STALEMATE,
        ENDED,
        TRUCE
    }

    public static final class WarEntry {
        public final UUID attackerId;
        public final UUID defenderId;
        public final long declarationTime;
        public WarState state;
        public long lastActivityTime;

        public WarEntry(UUID attackerId, UUID defenderId, long declarationTime) {
            this.attackerId = attackerId;
            this.defenderId = defenderId;
            this.declarationTime = declarationTime;
            this.state = WarState.DECLARED;
            this.lastActivityTime = declarationTime;
        }
    }

    private static final Map<UUID, WarEntry> activeWars = new HashMap<>();

    private WarManager() {}

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
        return isWarActive(attackerCountryId, defenderCountryId);
    }

    public static boolean declareWar(
        World world,
        UUID attackerId,
        UUID defenderId
    ) {
        if (attackerId == null || defenderId == null) {
            return false;
        }
        if (attackerId.equals(defenderId)) {
            return false;
        }

        Country attacker = CountryManager.getCountry(world, attackerId);
        Country defender = CountryManager.getCountry(world, defenderId);

        if (attacker == null || defender == null) {
            return false;
        }

        if (attacker.isAlliedWith(defenderId)) {
            return false;
        }

        if (attacker.isAtWarWith(defenderId)) {
            return false;
        }

        long currentTime = world.getTotalWorldTime();
        UUID warKey = createWarKey(attackerId, defenderId);

        activeWars.put(warKey, new WarEntry(attackerId, defenderId, currentTime));

        attacker.addWar(defenderId);
        defender.addWar(attackerId);

        broadcastWarDeclaration(world, attacker, defender);

        return true;
    }

    public static boolean endWar(
        World world,
        UUID attackerId,
        UUID defenderId
    ) {
        UUID warKey = createWarKey(attackerId, defenderId);
        WarEntry war = activeWars.get(warKey);

        if (war == null) {
            return false;
        }

        Country attacker = CountryManager.getCountry(world, attackerId);
        Country defender = CountryManager.getCountry(world, defenderId);

        if (attacker != null) {
            attacker.removeWar(defenderId);
        }
        if (defender != null) {
            defender.removeWar(attackerId);
        }

        war.state = WarState.ENDED;

        broadcastWarEnd(world, attacker, defender);

        activeWars.remove(warKey);

        return true;
    }

    public static boolean isWarActive(UUID country1, UUID country2) {
        UUID warKey = createWarKey(country1, country2);
        WarEntry war = activeWars.get(warKey);
        return war != null && (war.state == WarState.ACTIVE || war.state == WarState.DECLARED);
    }

    public static boolean isWarActive(UUID country1, UUID country2, WarState state) {
        UUID warKey = createWarKey(country1, country2);
        WarEntry war = activeWars.get(warKey);
        return war != null && war.state == state;
    }

    public static WarState getWarState(UUID country1, UUID country2) {
        UUID warKey = createWarKey(country1, country2);
        WarEntry war = activeWars.get(warKey);
        return war != null ? war.state : null;
    }

    public static void setWarState(UUID country1, UUID country2, WarState state) {
        UUID warKey = createWarKey(country1, country2);
        WarEntry war = activeWars.get(warKey);
        if (war != null) {
            war.state = state;
            war.lastActivityTime = System.currentTimeMillis();
        }
    }

    public static void updateWarActivity(World world, UUID attackerId, UUID defenderId) {
        UUID warKey = createWarKey(attackerId, defenderId);
        WarEntry war = activeWars.get(warKey);
        if (war != null) {
            war.lastActivityTime = System.currentTimeMillis();
            if (war.state == WarState.DECLARED) {
                war.state = WarState.ACTIVE;
            }
            if (war.state == WarState.STALEMATE) {
                war.state = WarState.ACTIVE;
            }
        }
    }

    public static boolean checkStalemate(UUID country1, UUID country2, long stalemateThresholdMs) {
        UUID warKey = createWarKey(country1, country2);
        WarEntry war = activeWars.get(warKey);
        if (war == null) {
            return false;
        }
        
        long inactiveTime = System.currentTimeMillis() - war.lastActivityTime;
        if (inactiveTime > stalemateThresholdMs) {
            war.state = WarState.STALEMATE;
            return true;
        }
        return false;
    }

    public static boolean proposeTruce(World world, UUID proposerId, UUID targetId) {
        UUID warKey = createWarKey(proposerId, targetId);
        WarEntry war = activeWars.get(warKey);
        
        if (war == null) {
            return false;
        }

        war.state = WarState.TRUCE;
        return true;
    }

    public static boolean acceptTruce(World world, UUID acceptorId, UUID otherId) {
        return endWar(world, acceptorId, otherId);
    }

    private static UUID createWarKey(UUID id1, UUID id2) {
        if (id1.compareTo(id2) < 0) {
            return id1;
        }
        return id2;
    }

    private static void broadcastWarDeclaration(World world, Country attacker, Country defender) {
        String message = String.format("[WAR] %s has declared war on %s!",
            attacker.getName(), defender.getName());
        world.getMinecraftServer().getPlayerList().sendMessage(
            new TextComponentString(message)
        );
    }

    private static void broadcastWarEnd(World world, Country attacker, Country defender) {
        String message = String.format("[WAR] The war between %s and %s has ended.",
            attacker.getName(), defender.getName());
        world.getMinecraftServer().getPlayerList().sendMessage(
            new TextComponentString(message)
        );
    }

    public static Map<UUID, WarEntry> getActiveWars() {
        return activeWars;
    }
}
