package com.baldeagle.country;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

import java.util.UUID;

public class CountryJoinCommand extends CommandBase {

    @Override
    public String getName() { return "country"; }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/country <create|requestjoin|approve|deny|listrequests|balance>";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
        if (!(sender instanceof EntityPlayerMP)) {
            sender.sendMessage(new TextComponentString("Must be a player"));
            return;
        }

        EntityPlayerMP player = (EntityPlayerMP) sender;
        UUID playerUUID = player.getUniqueID();
        World world = player.world;
        CountryStorage storage = CountryStorage.get(world);

        if (args.length < 1) {
            sender.sendMessage(new TextComponentString("Invalid command"));
            return;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {

            case "create": {
                if (args.length < 2) {
                    sender.sendMessage(new TextComponentString("Usage: /country create <name>"));
                    return;
                }
                String name = args[1];
                try {
                    Country country = CountryManager.createCountry(name, playerUUID);
                    storage.markDirty(); // auto-save
                    sender.sendMessage(new TextComponentString("Country '" + name + "' created. You are President."));
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(new TextComponentString(e.getMessage()));
                }
                break;
            }

            case "requestjoin": {
                if (args.length < 2) {
                    sender.sendMessage(new TextComponentString("Usage: /country requestjoin <country>"));
                    return;
                }
                Country c = CountryManager.getCountryByName(args[1]);
                if (c == null) {
                    sender.sendMessage(new TextComponentString("Country not found"));
                    return;
                }
                c.requestJoin(playerUUID);
                storage.markDirty(); // auto-save
                sender.sendMessage(new TextComponentString("Join request sent to " + c.getName()));
                break;
            }

            case "approve": {
                if (args.length < 3) {
                    sender.sendMessage(new TextComponentString("Usage: /country approve <country> <playerUUID>"));
                    return;
                }
                Country c = CountryManager.getCountryByName(args[1]);
                if (c == null) {
                    sender.sendMessage(new TextComponentString("Country not found"));
                    return;
                }
                if (!c.isAuthorized(playerUUID)) {
                    sender.sendMessage(new TextComponentString("You are not authorized to approve requests"));
                    return;
                }
                UUID applicant = UUID.fromString(args[2]);
                if (c.approveJoin(playerUUID, applicant)) {
                    storage.markDirty(); // auto-save
                    sender.sendMessage(new TextComponentString("Approved join request for " + applicant));
                } else {
                    sender.sendMessage(new TextComponentString("No pending request for that player"));
                }
                break;
            }

            case "deny": {
                if (args.length < 3) {
                    sender.sendMessage(new TextComponentString("Usage: /country deny <country> <playerUUID>"));
                    return;
                }
                Country c = CountryManager.getCountryByName(args[1]);
                if (c == null) {
                    sender.sendMessage(new TextComponentString("Country not found"));
                    return;
                }
                if (!c.isAuthorized(playerUUID)) {
                    sender.sendMessage(new TextComponentString("You are not authorized to deny requests"));
                    return;
                }
                UUID applicant = UUID.fromString(args[2]);
                c.denyJoin(playerUUID, applicant);
                storage.markDirty(); // auto-save
                sender.sendMessage(new TextComponentString("Denied join request for " + applicant));
                break;
            }

            case "listrequests": {
                if (args.length < 2) {
                    sender.sendMessage(new TextComponentString("Usage: /country listrequests <country>"));
                    return;
                }
                Country c = CountryManager.getCountryByName(args[1]);
                if (c == null) {
                    sender.sendMessage(new TextComponentString("Country not found"));
                    return;
                }
                if (!c.isAuthorized(playerUUID)) {
                    sender.sendMessage(new TextComponentString("You are not authorized to view requests"));
                    return;
                }
                StringBuilder sb = new StringBuilder("Pending requests: ");
                for (UUID u : c.getJoinRequests()) sb.append(u.toString()).append(" ");
                sender.sendMessage(new TextComponentString(sb.toString()));
                break;
            }

            case "balance": {
                if (args.length < 2) {
                    sender.sendMessage(new TextComponentString("Usage: /country balance <country>"));
                    return;
                }
                Country c = CountryManager.getCountryByName(args[1]);
                if (c == null) {
                    sender.sendMessage(new TextComponentString("Country not found"));
                    return;
                }
                sender.sendMessage(new TextComponentString(c.getName() + " balance: " + c.getBalance()));
                break;
            }

            default:
                sender.sendMessage(new TextComponentString("Unknown subcommand"));
                break;
        }
    }
}
