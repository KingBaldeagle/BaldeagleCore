package com.baldeagle.territory;

import com.baldeagle.country.Country;
import com.baldeagle.country.CountryManager;
import java.util.UUID;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

public final class RelationshipSystem {

    public enum RelationshipState {
        NEUTRAL,
        ALLY,
        ENEMY,
        WAR,
        TRUCE
    }

    private RelationshipSystem() {}

    public static RelationshipState getRelationship(
        World world,
        UUID country1Id,
        UUID country2Id
    ) {
        if (country1Id == null || country2Id == null) {
            return RelationshipState.NEUTRAL;
        }
        if (country1Id.equals(country2Id)) {
            return RelationshipState.NEUTRAL;
        }

        Country country1 = CountryManager.getCountry(world, country1Id);
        Country country2 = CountryManager.getCountry(world, country2Id);

        if (country1 == null || country2 == null) {
            return RelationshipState.NEUTRAL;
        }

        if (country1.isAlliedWith(country2Id)) {
            return RelationshipState.ALLY;
        }

        if (country1.isAtWarWith(country2Id)) {
            WarManager.WarState warState = WarManager.getWarState(country1Id, country2Id);
            if (warState == WarManager.WarState.TRUCE) {
                return RelationshipState.TRUCE;
            }
            return RelationshipState.WAR;
        }

        return RelationshipState.NEUTRAL;
    }

    public static boolean canDeclareWar(
        World world,
        UUID requesterId,
        UUID targetId
    ) {
        if (requesterId == null || targetId == null) {
            return false;
        }
        if (requesterId.equals(targetId)) {
            return false;
        }

        Country requester = CountryManager.getCountry(world, requesterId);
        Country target = CountryManager.getCountry(world, targetId);

        if (requester == null || target == null) {
            return false;
        }

        if (requester.isAlliedWith(targetId)) {
            return false;
        }

        if (requester.isAtWarWith(targetId)) {
            return false;
        }

        return true;
    }

    public static boolean proposeAlliance(
        World world,
        UUID requesterId,
        UUID targetId
    ) {
        if (requesterId == null || targetId == null) {
            return false;
        }
        if (requesterId.equals(targetId)) {
            return false;
        }

        Country requester = CountryManager.getCountry(world, requesterId);
        Country target = CountryManager.getCountry(world, targetId);

        if (requester == null || target == null) {
            return false;
        }

        if (requester.isAtWarWith(targetId)) {
            return false;
        }

        if (target.hasIncomingAllianceRequest(requesterId)) {
            return false;
        }

        target.addIncomingAllianceRequest(requesterId);

        String message = String.format("[DIPLOMACY] %s has proposed an alliance with %s",
            requester.getName(), target.getName());
        world.getMinecraftServer().getPlayerList().sendMessage(
            new TextComponentString(message)
        );

        return true;
    }

    public static boolean acceptAlliance(
        World world,
        UUID acceptorId,
        UUID requesterId
    ) {
        if (acceptorId == null || requesterId == null) {
            return false;
        }

        Country acceptor = CountryManager.getCountry(world, acceptorId);
        Country requester = CountryManager.getCountry(world, requesterId);

        if (acceptor == null || requester == null) {
            return false;
        }

        if (!acceptor.hasIncomingAllianceRequest(requesterId)) {
            return false;
        }

        acceptor.removeIncomingAllianceRequest(requesterId);
        acceptor.addAlly(requesterId);
        requester.addAlly(acceptorId);

        String message = String.format("[DIPLOMACY] %s and %s are now allies!",
            acceptor.getName(), requester.getName());
        world.getMinecraftServer().getPlayerList().sendMessage(
            new TextComponentString(message)
        );

        return true;
    }

    public static boolean denyAlliance(
        World world,
        UUID denierId,
        UUID requesterId
    ) {
        if (denierId == null || requesterId == null) {
            return false;
        }

        Country denier = CountryManager.getCountry(world, denierId);
        Country requester = CountryManager.getCountry(world, requesterId);

        if (denier == null || requester == null) {
            return false;
        }

        if (!denier.hasIncomingAllianceRequest(requesterId)) {
            return false;
        }

        denier.removeIncomingAllianceRequest(requesterId);

        String message = String.format("[DIPLOMACY] %s has declined the alliance request from %s",
            denier.getName(), requester.getName());
        world.getMinecraftServer().getPlayerList().sendMessage(
            new TextComponentString(message)
        );

        return true;
    }

    public static boolean breakAlliance(
        World world,
        UUID breakerId,
        UUID targetId
    ) {
        if (breakerId == null || targetId == null) {
            return false;
        }

        Country breaker = CountryManager.getCountry(world, breakerId);
        Country target = CountryManager.getCountry(world, targetId);

        if (breaker == null || target == null) {
            return false;
        }

        if (!breaker.isAlliedWith(targetId)) {
            return false;
        }

        breaker.removeAlly(targetId);
        target.removeAlly(breakerId);

        String message = String.format("[DIPLOMACY] %s has ended their alliance with %s",
            breaker.getName(), target.getName());
        world.getMinecraftServer().getPlayerList().sendMessage(
            new TextComponentString(message)
        );

        return true;
    }

    public static String getRelationshipColor(RelationshipState state) {
        switch (state) {
            case ALLY:
                return "§a";
            case NEUTRAL:
                return "§e";
            case ENEMY:
                return "§c";
            case WAR:
                return "§4";
            case TRUCE:
                return "§5";
            default:
                return "§f";
        }
    }
}
