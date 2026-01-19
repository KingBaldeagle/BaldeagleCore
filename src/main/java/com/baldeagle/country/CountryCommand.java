package com.baldeagle.country;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

import java.util.Map;
import java.util.UUID;

public class CountryCommand extends CommandBase {
    @Override
    public String getName() {
        return "country";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/country <create|info|list|requestjoin|approve|deny|listrequests|deposit|transfer|promote> [...]";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (!(sender instanceof EntityPlayerMP)) {
            sender.sendMessage(new TextComponentString("Only players can use this command."));
            return;
        }

        EntityPlayerMP player = (EntityPlayerMP) sender;
        UUID playerUUID = player.getUniqueID();
        World world = player.getEntityWorld();

        if (args.length < 1) {
            sender.sendMessage(new TextComponentString(getUsage(sender)));
            return;
        }

        String sub = args[0].toLowerCase();
        Map<UUID, Country> countries = CountryManager.getAllCountries(world);

        switch (sub) {

            // --- CREATE COUNTRY ---
            case "create": {
                if (args.length < 2) {
                    sender.sendMessage(new TextComponentString("Usage: /country create <name>"));
                    return;
                }
                String name = args[1];
                try {
                    Country country = CountryManager.createCountry(world, name, playerUUID);
                    sender.sendMessage(new TextComponentString("Country '" + name + "' created! You are President."));
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(new TextComponentString(e.getMessage()));
                }
                break;
            }

            // --- INFO ---
            case "info": {
                Country found = null;
                for (Country c : CountryManager.getAllCountries(world).values()) {
                    if (c.isMember(playerUUID)) {
                        found = c;
                        break;
                    }
                }

                if (found == null) {
                    sender.sendMessage(new TextComponentString("You are not part of any country."));
                } else {
                    Country.Role role = found.getRole(playerUUID);

                    StringBuilder membersStr = new StringBuilder();
                    for (UUID uuid : found.getMembers().keySet()) {
                        EntityPlayerMP member = server.getPlayerList().getPlayerByUUID(uuid);
                        String name = member != null ? member.getName() : uuid.toString();
                        membersStr.append(name).append(" (").append(found.getRole(uuid).name()).append("), ");
                    }
                    if (membersStr.length() > 2) membersStr.setLength(membersStr.length() - 2);

                    sender.sendMessage(new TextComponentString(
                            "Country: " + found.getName() +
                                    " | Role: " + role.name() +
                                    " | Balance: " + found.getBalance() +
                                    " | Members: " + membersStr
                    ));
                }
                break;
            }

            // --- LIST COUNTRIES ---
            case "list": {
                if (countries.isEmpty()) {
                    sender.sendMessage(new TextComponentString("No countries exist in this world."));
                } else {
                    sender.sendMessage(new TextComponentString("=== Countries ==="));
                    for (Country c : countries.values()) {
                        sender.sendMessage(new TextComponentString(
                                c.getName() + " | Members: " + c.getMembers().size() + " | Balance: " + c.getBalance()
                        ));
                    }
                }
                break;
            }

            // --- REQUEST JOIN ---
            case "requestjoin": {
                if (args.length < 2) {
                    sender.sendMessage(new TextComponentString("Usage: /country requestjoin <countryName>"));
                    return;
                }
                Country c = CountryManager.getCountryByName(world, args[1]);
                if (c == null) {
                    sender.sendMessage(new TextComponentString("Country not found."));
                    return;
                }
                if (c.isMember(playerUUID)) {
                    sender.sendMessage(new TextComponentString("You are already a member."));
                    return;
                }
                c.requestJoin(playerUUID);
                CountryStorage.get(world).markDirty();
                sender.sendMessage(new TextComponentString("Join request sent to " + c.getName()));
                break;
            }

            // --- APPROVE ---
            case "approve": {
                if (args.length < 3) {
                    sender.sendMessage(new TextComponentString("Usage: /country approve <countryName> <playerUUID>"));
                    return;
                }
                Country c = CountryManager.getCountryByName(world, args[1]);
                if (c == null) {
                    sender.sendMessage(new TextComponentString("Country not found."));
                    return;
                }
                if (!c.isAuthorized(playerUUID)) {
                    sender.sendMessage(new TextComponentString("You are not authorized to approve members."));
                    return;
                }
                UUID applicant;
                try {
                    applicant = UUID.fromString(args[2]);
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(new TextComponentString("Invalid UUID."));
                    return;
                }
                if (c.approveJoin(playerUUID, applicant)) {
                    CountryStorage.get(world).markDirty();
                    sender.sendMessage(new TextComponentString("Approved " + applicant + " to join " + c.getName()));
                } else {
                    sender.sendMessage(new TextComponentString("Approval failed."));
                }
                break;
            }

            // --- DENY ---
            case "deny": {
                if (args.length < 3) {
                    sender.sendMessage(new TextComponentString("Usage: /country deny <countryName> <playerUUID>"));
                    return;
                }
                Country c = CountryManager.getCountryByName(world, args[1]);
                if (c == null) {
                    sender.sendMessage(new TextComponentString("Country not found."));
                    return;
                }
                if (!c.isAuthorized(playerUUID)) {
                    sender.sendMessage(new TextComponentString("You are not authorized to deny members."));
                    return;
                }
                UUID applicant;
                try {
                    applicant = UUID.fromString(args[2]);
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(new TextComponentString("Invalid UUID."));
                    return;
                }
                c.denyJoin(playerUUID, applicant);
                CountryStorage.get(world).markDirty();
                sender.sendMessage(new TextComponentString("Denied " + applicant + " from joining " + c.getName()));
                break;
            }

            // --- LIST JOIN REQUESTS ---
            case "listrequests": {
                if (args.length < 2) {
                    sender.sendMessage(new TextComponentString("Usage: /country listrequests <countryName>"));
                    return;
                }
                Country c = CountryManager.getCountryByName(world, args[1]);
                if (c == null) {
                    sender.sendMessage(new TextComponentString("Country not found."));
                    return;
                }
                if (!c.isAuthorized(playerUUID)) {
                    sender.sendMessage(new TextComponentString("You are not authorized to view join requests."));
                    return;
                }
                if (c.getJoinRequests().isEmpty()) {
                    sender.sendMessage(new TextComponentString("No pending join requests."));
                } else {
                    sender.sendMessage(new TextComponentString("Pending join requests:"));
                    for (UUID u : c.getJoinRequests()) {
                        EntityPlayerMP applicant = server.getPlayerList().getPlayerByUUID(u);
                        String name = applicant != null ? applicant.getName() : u.toString();
                        sender.sendMessage(new TextComponentString(name));
                    }
                }

                break;
            }

            // --- DEPOSIT ---
            case "deposit": {
                if (args.length < 3) {
                    sender.sendMessage(new TextComponentString("Usage: /country deposit <countryName> <amount>"));
                    return;
                }
                Country c = CountryManager.getCountryByName(world, args[1]);
                if (c == null) {
                    sender.sendMessage(new TextComponentString("Country not found."));
                    return;
                }
                if (!c.isAuthorized(playerUUID)) {
                    sender.sendMessage(new TextComponentString("You are not authorized to deposit."));
                    return;
                }
                double amount;
                try { amount = Double.parseDouble(args[2]); }
                catch (NumberFormatException e) { sender.sendMessage(new TextComponentString("Invalid number.")); return; }
                c.deposit(playerUUID, amount);
                CountryStorage.get(world).markDirty();
                sender.sendMessage(new TextComponentString("Deposited " + amount + " to " + c.getName()));
                break;
            }

            // --- TRANSFER ---
            case "transfer": {
                if (args.length < 4) {
                    sender.sendMessage(new TextComponentString("Usage: /country transfer <fromCountry> <toCountry> <amount>"));
                    return;
                }
                Country from = CountryManager.getCountryByName(world, args[1]);
                Country to = CountryManager.getCountryByName(world, args[2]);
                if (from == null || to == null) {
                    sender.sendMessage(new TextComponentString("One of the countries does not exist."));
                    return;
                }
                if (!from.isAuthorized(playerUUID)) {
                    sender.sendMessage(new TextComponentString("You are not authorized to transfer."));
                    return;
                }
                double amount;
                try { amount = Double.parseDouble(args[3]); }
                catch (NumberFormatException e) { sender.sendMessage(new TextComponentString("Invalid number.")); return; }
                try {
                    from.transfer(playerUUID, to, amount);
                    CountryStorage.get(world).markDirty();
                    sender.sendMessage(new TextComponentString("Transferred " + amount + " from " + from.getName() + " to " + to.getName()));
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(new TextComponentString(e.getMessage()));
                }
                break;
            }

            // --- PROMOTE ---
            case "promote": {
                if (args.length < 3) {
                    sender.sendMessage(new TextComponentString("Usage: /country promote <countryName> <playerUUID>"));
                    return;
                }
                Country c = CountryManager.getCountryByName(world, args[1]);
                if (c == null) {
                    sender.sendMessage(new TextComponentString("Country not found."));
                    return;
                }
                if (c.getRole(playerUUID) != Country.Role.PRESIDENT) {
                    sender.sendMessage(new TextComponentString("Only the President can promote members."));
                    return;
                }
                UUID member;
                try { member = UUID.fromString(args[2]); }
                catch (IllegalArgumentException e) { sender.sendMessage(new TextComponentString("Invalid UUID.")); return; }
                if (c.promote(playerUUID, member)) {
                    CountryStorage.get(world).markDirty();
                    sender.sendMessage(new TextComponentString("Promoted " + member + " to Minister in " + c.getName()));
                } else {
                    sender.sendMessage(new TextComponentString("Promotion failed."));
                }
                break;
            }

            default:
                sender.sendMessage(new TextComponentString("Unknown subcommand. Usage: " + getUsage(sender)));
        }
    }
}
