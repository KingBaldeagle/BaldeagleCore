package com.baldeagle.country;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

import java.util.UUID;

public class CountryMoneyCommand extends CommandBase {

    @Override
    public String getName() { return "countrymoney"; }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/countrymoney <deposit|withdraw|transfer> <args>";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
        if (!(sender instanceof EntityPlayerMP)) {
            sender.sendMessage(new net.minecraft.util.text.TextComponentString("Must be a player"));
            return;
        }

        EntityPlayerMP player = (EntityPlayerMP) sender;
        UUID playerUUID = player.getUniqueID();

        if (args.length < 1) {
            sender.sendMessage(new net.minecraft.util.text.TextComponentString("Invalid command"));
            return;
        }

        String sub = args[0];

        switch (sub.toLowerCase()) {

            case "deposit": {
                if (args.length < 3) {
                    sender.sendMessage(new net.minecraft.util.text.TextComponentString("Usage: deposit <country> <amount>"));
                    return;
                }
                Country c = CountryManager.getCountryByName(args[1]);
                if (c == null) {
                    sender.sendMessage(new net.minecraft.util.text.TextComponentString("Country not found"));
                    return;
                }
                double amount = Double.parseDouble(args[2]);
                try {
                    c.deposit(playerUUID, amount);
                    sender.sendMessage(new net.minecraft.util.text.TextComponentString(
                            "Deposited " + amount + " to " + c.getName()));
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(new net.minecraft.util.text.TextComponentString(e.getMessage()));
                }
                break;
            }

            case "transfer": {
                if (args.length < 4) {
                    sender.sendMessage(new net.minecraft.util.text.TextComponentString("Usage: transfer <fromCountry> <toCountry> <amount>"));
                    return;
                }
                Country from = CountryManager.getCountryByName(args[1]);
                Country to = CountryManager.getCountryByName(args[2]);
                double amount = Double.parseDouble(args[3]);
                if (from == null || to == null) {
                    sender.sendMessage(new net.minecraft.util.text.TextComponentString("Country not found"));
                    return;
                }
                try {
                    from.transfer(playerUUID, to, amount);
                    sender.sendMessage(new net.minecraft.util.text.TextComponentString(
                            "Transferred " + amount + " from " + from.getName() + " to " + to.getName()));
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(new net.minecraft.util.text.TextComponentString(e.getMessage()));
                }
                break;
            }
        }
    }
}
