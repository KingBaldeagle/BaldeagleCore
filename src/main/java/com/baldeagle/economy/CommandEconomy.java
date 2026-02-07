package com.baldeagle.economy;

import com.baldeagle.util.MoneyFormatUtil;
import java.util.UUID;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

public class CommandEconomy extends CommandBase {

    @Override
    public String getName() {
        return "economy";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/economy <player|country> <get|deposit|withdraw> <target> <amount>";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(new TextComponentString("Usage: " + getUsage(sender)));
            return;
        }

        World world = sender.getEntityWorld();
        String type = args[0];
        String action = args[1];

        try {
            if (type.equalsIgnoreCase("player")) {
                EntityPlayerMP targetPlayer = server.getPlayerList().getPlayerByUsername(args[2]);
                if (targetPlayer == null) {
                    sender.sendMessage(new TextComponentString("Player not found!"));
                    return;
                }
                UUID uuid = targetPlayer.getUniqueID();
                if (action.equalsIgnoreCase("get")) {
                    long balance = EconomyManager.getPlayerBalance(world, uuid);
                    sender.sendMessage(new TextComponentString(targetPlayer.getName() + " balance: " + MoneyFormatUtil.format(balance)));
                } else if (args.length >= 4) {
                    long amount = Long.parseLong(args[3]);
                    if (action.equalsIgnoreCase("deposit")) {
                        EconomyManager.depositPlayer(world, uuid, amount);
                        sender.sendMessage(new TextComponentString("Deposited " + MoneyFormatUtil.format(amount) + " to " + targetPlayer.getName()));
                    } else if (action.equalsIgnoreCase("withdraw")) {
                        if (EconomyManager.withdrawPlayer(world, uuid, amount)) {
                            sender.sendMessage(new TextComponentString("Withdrew " + MoneyFormatUtil.format(amount) + " from " + targetPlayer.getName()));
                        } else {
                            sender.sendMessage(new TextComponentString("Insufficient funds!"));
                        }
                    }
                }
            } else if (type.equalsIgnoreCase("country")) {
                String country = args[2];
                if (action.equalsIgnoreCase("get")) {
                    long balance = EconomyManager.getCountryBalance(world, country);
                    sender.sendMessage(new TextComponentString(country + " balance: " + MoneyFormatUtil.format(balance)));
                } else if (args.length >= 4) {
                    long amount = Long.parseLong(args[3]);
                    if (action.equalsIgnoreCase("deposit")) {
                        EconomyManager.depositCountry(world, country, amount);
                        sender.sendMessage(new TextComponentString("Deposited " + MoneyFormatUtil.format(amount) + " to " + country));
                    } else if (action.equalsIgnoreCase("withdraw")) {
                        if (EconomyManager.withdrawCountry(world, country, amount)) {
                            sender.sendMessage(new TextComponentString("Withdrew " + MoneyFormatUtil.format(amount) + " from " + country));
                        } else {
                            sender.sendMessage(new TextComponentString("Insufficient funds!"));
                        }
                    }
                }
            } else {
                sender.sendMessage(new TextComponentString("Invalid type! Use player or country."));
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(new TextComponentString("Amount must be a number!"));
        }
    }
}
