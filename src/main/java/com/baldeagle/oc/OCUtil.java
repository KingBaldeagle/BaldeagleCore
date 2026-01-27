package com.baldeagle.oc;

import com.baldeagle.country.Country;
import com.baldeagle.country.CountryManager;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.machine.Machine;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerProfileCache;
import net.minecraft.world.World;

public final class OCUtil {

    private OCUtil() {}

    public static UUID parseUuid(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("Missing UUID");
        }
        try {
            return UUID.fromString(raw.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid UUID: " + raw);
        }
    }

    public static UUID resolveActorUuid(Context context, World world) {
        if (!(context instanceof Machine)) {
            throw new IllegalArgumentException(
                "Unable to resolve machine users."
            );
        }

        MinecraftServer server = world.getMinecraftServer();
        if (server == null) {
            throw new IllegalArgumentException("Server unavailable.");
        }

        PlayerProfileCache cache = server.getPlayerProfileCache();
        if (cache == null) {
            throw new IllegalArgumentException("Profile cache unavailable.");
        }

        String[] users = ((Machine) context).users();
        if (users == null || users.length == 0) {
            throw new IllegalArgumentException(
                "Add at least one user to the computer."
            );
        }

        for (String name : users) {
            if (name == null || name.trim().isEmpty()) {
                continue;
            }
            GameProfile profile = cache.getGameProfileForUsername(name);
            if (profile != null && profile.getId() != null) {
                if (!context.canInteract(name)) {
                    continue;
                }
                return profile.getId();
            }
        }

        throw new IllegalArgumentException(
            "No computer users can be resolved to a UUID."
        );
    }

    public static String resolveActorName(Context context, World world, UUID uuid) {
        MinecraftServer server = world.getMinecraftServer();
        if (server == null) {
            return null;
        }
        net.minecraft.entity.player.EntityPlayerMP online =
            server.getPlayerList().getPlayerByUUID(uuid);
        if (online != null) {
            return online.getName();
        }
        PlayerProfileCache cache = server.getPlayerProfileCache();
        if (cache == null) {
            return null;
        }
        GameProfile profile = cache.getProfileByUUID(uuid);
        return profile != null ? profile.getName() : null;
    }

    public static Country requireActorCountry(World world, UUID actorUuid) {
        Country country = CountryManager.getCountryForPlayer(world, actorUuid);
        if (country == null) {
            throw new IllegalArgumentException("Player is not in a country.");
        }
        return country;
    }

    public static void requireAuthorized(Country country, UUID actorUuid) {
        if (country == null || actorUuid == null) {
            throw new IllegalArgumentException("Unauthorized.");
        }
        if (!country.isHighAuthority(actorUuid)) {
            throw new IllegalArgumentException("Unauthorized.");
        }
    }

    public static void requireCanInteract(Context context, String actorName) {
        if (actorName == null || actorName.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Unable to resolve player name for permission check."
            );
        }
        if (!context.canInteract(actorName)) {
            throw new IllegalArgumentException("Not permitted by computer.");
        }
    }
}
