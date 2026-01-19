package com.baldeagle.country;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

import java.util.UUID;

public class CountryCommand extends CommandBase {

    @Override
    public String getName() {
        return "country";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/country <create|requestjoin|approve|deny|listrequests|balance|deposit|transfer>";
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

            // --- COUNTRY CREATION ---
            case "create": {
                if (args.length < 2) {
                    sender.sendMessage(new TextComponentString("Usage: /country create <name>"));
                    return;
                }
                String name = args[1];
                try {
                    Country country = CountryManager.createCountry(name, playerUUID);
                    storage.markDirty();
                    sender.sendMessage(new TextComponentString("Country '" + name + "' created. You are President."));
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(new TextComponentString(e.getMessage()));
                }
                break;
            }

            // --- JOIN REQUEST ---
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
                storage.markDirty();
                sender.sendMessage(new TextComponentString("Join request sent to " + c.getName()));
                break;
            }

            // --- APPROVE JOIN ---
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
                    storage.markDirty();
                    sender.sendMessage(new TextComponentString("Approved join request for " + applicant));
                } else {
                    sender.sendMessage(new TextComponentString("No pending request for that player"));
                }
                break;
            }

            // --- DENY JOIN ---
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
                storage.markDirty();
                sender.sendMessage(new TextComponentString("Denied join request for " + applicant));
                break;
            }
            
            // --- PROMOTE ---
            case "promote": {
                if (args.length < 3) {
                    sender.sendMessage(new TextComponentString("Usage: /country promote <country> <playerUUID>"));
                    return;
                }
                Country c = CountryManager.getCountryByName(args[1]);
                if (c == null) {
                    sender.sendMessage(new TextComponentString("Country not found"));
                    return;
                }
                if (c.getRole(playerUUID) != Country.Role.PRESIDENT) {
                    sender.sendMessage(new TextComponentString("Only the President can promote members"));
                    return;
                }
                UUID memberUUID;
                try {
                    memberUUID = UUID.fromString(args[2]);
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(new TextComponentString("Invalid UUID"));
                    return;
                }
                if (c.promote(playerUUID, memberUUID)) {
                    CountryStorage.get(world).markDirty();
                    sender.sendMessage(new TextComponentString("Promoted " + memberUUID + " to Minister"));
                } else {
                    sender.sendMessage(new TextComponentString("Promotion failed. Make sure the player is a member"));
                }
                break;
            }


            // --- LIST JOIN REQUESTS ---
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

            // --- BALANCE ---
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

            // --- DEPOSIT ---
            case "deposit": {
                if (args.length < 3) {
                    sender.sendMessage(new TextComponentString("Usage: /country deposit <country> <amount>"));
                    return;
                }
                Country c = CountryManager.getCountryByName(args[1]);
                if (c == null) {
                    sender.sendMessage(new TextComponentString("Country not found"));
                    return;
                }
                double amount;
                try {
                    amount = Double.parseDouble(args[2]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(new TextComponentString("Invalid amount"));
                    return;
                }
                try {
                    c.deposit(playerUUID, amount);
                    storage.markDirty();
                    sender.sendMessage(new TextComponentString("Deposited " + amount + " to " + c.getName()));
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(new TextComponentString(e.getMessage()));
                }
                break;
            }

            // --- TRANSFER ---
            case "transfer": {
                if (args.length < 4) {
                    sender.sendMessage(new TextComponentString("Usage: /country transfer <fromCountry> <toCountry> <amount>"));
                    return;
                }
                Country from = CountryManager.getCountryByName(args[1]);
                Country to = CountryManager.getCountryByName(args[2]);
                if (from == null || to == null) {
                    sender.sendMessage(new TextComponentString("Country not found"));
                    return;
                }
                double amount;
                try {
                    amount = Double.parseDouble(args[3]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(new TextComponentString("Invalid amount"));
                    return;
                }
                try {
                    from.transfer(playerUUID, to, amount);
                    storage.markDirty();
                    sender.sendMessage(new TextComponentString("Transferred " + amount + " from " + from.getName() + " to " + to.getName()));
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(new TextComponentString(e.getMessage()));
                }
                break;
            }

            default:
                sender.sendMessage(new TextComponentString("Unknown subcommand"));
                break;
        }
    }
}
