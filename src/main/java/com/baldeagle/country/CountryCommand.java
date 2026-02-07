package com.baldeagle.country;

import com.baldeagle.territory.TerritoryManager;
import com.baldeagle.util.MoneyFormatUtil;
import java.util.Map;
import java.util.UUID;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

public class CountryCommand extends CommandBase {

    @Override
    public String getName() {
        return "country";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/country <create|info|list|requestjoin|approve|deny|listrequests|deposit|transfer|promote|ally|war|bounty|station> [...]";
    }

    @Override
    public void execute(
        MinecraftServer server,
        ICommandSender sender,
        String[] args
    ) throws CommandException {
        if (!(sender instanceof EntityPlayerMP)) {
            sender.sendMessage(
                new TextComponentString("Only players can use this command")
            );
            return;
        }

        EntityPlayerMP player = (EntityPlayerMP) sender;
        UUID playerUUID = player.getUniqueID();
        World world = player.getEntityWorld();
        World countryWorld =
            world.getMinecraftServer() != null
                ? world.getMinecraftServer().getWorld(0)
                : world;

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
                    sender.sendMessage(
                        new TextComponentString("Usage: /country create <name>")
                    );
                    return;
                }
                String name = args[1];
                try {
                    Country country = CountryManager.createCountry(
                        world,
                        name,
                        playerUUID
                    );
                    sender.sendMessage(
                        new TextComponentString(
                            "Country '" + name + "' created! You are President"
                        )
                    );
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(new TextComponentString(e.getMessage()));
                }
                break;
            }
            // --- INFO ---
            case "info": {
                Country found = null;
                for (Country c : CountryManager.getAllCountries(
                    world
                ).values()) {
                    if (c.isMember(playerUUID)) {
                        found = c;
                        break;
                    }
                }

                if (found == null) {
                    sender.sendMessage(
                        new TextComponentString(
                            "You are not part of any country"
                        )
                    );
                } else {
                    Country.Role role = found.getRole(playerUUID);

                    StringBuilder membersStr = new StringBuilder();
                    for (UUID uuid : found.getMembers().keySet()) {
                        EntityPlayerMP member = server
                            .getPlayerList()
                            .getPlayerByUUID(uuid);
                        String name =
                            member != null ? member.getName() : uuid.toString();
                        membersStr
                            .append(name)
                            .append(" (")
                            .append(found.getRole(uuid).name())
                            .append("), ");
                    }
                    if (membersStr.length() > 2) membersStr.setLength(
                        membersStr.length() - 2
                    );

                    sender.sendMessage(
                        new TextComponentString(
                            "Country: " +
                                found.getName() +
                                " | Role: " +
                                role.name() +
                                " | Balance: " +
                                MoneyFormatUtil.format(found.getBalance()) +
                                " | Members: " +
                                membersStr
                        )
                    );
                }
                break;
            }
            // --- LIST COUNTRIES ---
            case "list": {
                if (countries.isEmpty()) {
                    sender.sendMessage(
                        new TextComponentString(
                            "No countries exist in this world"
                        )
                    );
                } else {
                    Map<UUID, Integer> claimCounts =
                        TerritoryManager.getClaimCounts(server);
                    sender.sendMessage(
                        new TextComponentString("=== Countries ===")
                    );
                    for (Country c : countries.values()) {
                        sender.sendMessage(
                            new TextComponentString(
                                c.getName() +
                                    " | Members: " +
                                    c.getMembers().size() +
                                    " | Chunks: " +
                                    claimCounts.getOrDefault(c.getId(), 0) +
                                    " | Balance: " +
                                    MoneyFormatUtil.format(c.getBalance())
                            )
                        );
                    }
                }
                break;
            }
            // --- REQUEST JOIN ---
            case "requestjoin": {
                if (args.length < 2) {
                    sender.sendMessage(
                        new TextComponentString(
                            "Usage: /country requestjoin <countryName>"
                        )
                    );
                    return;
                }
                Country c = CountryManager.getCountryByName(world, args[1]);
                if (c == null) {
                    sender.sendMessage(
                        new TextComponentString("Country not found.")
                    );
                    return;
                }
                if (c.isMember(playerUUID)) {
                    sender.sendMessage(
                        new TextComponentString("You are already a member.")
                    );
                    return;
                }
                c.requestJoin(playerUUID);
                CountryStorage.get(countryWorld).markDirty();
                sender.sendMessage(
                    new TextComponentString(
                        "Join request sent to " + c.getName()
                    )
                );
                break;
            }
            // --- APPROVE ---
            case "approve": {
                if (args.length < 3) {
                    sender.sendMessage(
                        new TextComponentString(
                            "Usage: /country approve <countryName> <playerUUID>"
                        )
                    );
                    return;
                }
                Country c = CountryManager.getCountryByName(world, args[1]);
                if (c == null) {
                    sender.sendMessage(
                        new TextComponentString("Country not found")
                    );
                    return;
                }
                if (!c.isAuthorized(playerUUID)) {
                    sender.sendMessage(
                        new TextComponentString(
                            "You are not authorized to approve members"
                        )
                    );
                    return;
                }
                UUID applicant;
                try {
                    applicant = UUID.fromString(args[2]);
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(new TextComponentString("Invalid UUID"));
                    return;
                }
                if (c.approveJoin(playerUUID, applicant)) {
                    CountryStorage.get(countryWorld).markDirty();
                    sender.sendMessage(
                        new TextComponentString(
                            "Approved " + applicant + " to join " + c.getName()
                        )
                    );
                } else {
                    sender.sendMessage(
                        new TextComponentString("Approval failed")
                    );
                }
                break;
            }
            // --- DENY ---
            case "deny": {
                if (args.length < 3) {
                    sender.sendMessage(
                        new TextComponentString(
                            "Usage: /country deny <countryName> <playerUUID>"
                        )
                    );
                    return;
                }
                Country c = CountryManager.getCountryByName(world, args[1]);
                if (c == null) {
                    sender.sendMessage(
                        new TextComponentString("Country not found")
                    );
                    return;
                }
                if (!c.isAuthorized(playerUUID)) {
                    sender.sendMessage(
                        new TextComponentString(
                            "You are not authorized to deny members"
                        )
                    );
                    return;
                }
                UUID applicant;
                try {
                    applicant = UUID.fromString(args[2]);
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(new TextComponentString("Invalid UUID"));
                    return;
                }
                c.denyJoin(playerUUID, applicant);
                CountryStorage.get(countryWorld).markDirty();
                sender.sendMessage(
                    new TextComponentString(
                        "Denied " + applicant + " from joining " + c.getName()
                    )
                );
                break;
            }
            // --- LIST JOIN REQUESTS ---
            case "listrequests": {
                if (args.length < 2) {
                    sender.sendMessage(
                        new TextComponentString(
                            "Usage: /country listrequests <countryName>"
                        )
                    );
                    return;
                }
                Country c = CountryManager.getCountryByName(world, args[1]);
                if (c == null) {
                    sender.sendMessage(
                        new TextComponentString("Country not found")
                    );
                    return;
                }
                if (!c.isAuthorized(playerUUID)) {
                    sender.sendMessage(
                        new TextComponentString(
                            "You are not authorized to view join requests"
                        )
                    );
                    return;
                }
                if (c.getJoinRequests().isEmpty()) {
                    sender.sendMessage(
                        new TextComponentString("No pending join requests")
                    );
                } else {
                    sender.sendMessage(
                        new TextComponentString("Pending join requests:")
                    );
                    for (UUID u : c.getJoinRequests()) {
                        EntityPlayerMP applicant = server
                            .getPlayerList()
                            .getPlayerByUUID(u);
                        String name =
                            applicant != null
                                ? applicant.getName()
                                : u.toString();
                        sender.sendMessage(new TextComponentString(name));
                    }
                }

                break;
            }
            // --- DEPOSIT ---
            case "deposit": {
                if (args.length < 3) {
                    sender.sendMessage(
                        new TextComponentString(
                            "Usage: /country deposit <countryName> <amount>"
                        )
                    );
                    return;
                }
                Country c = CountryManager.getCountryByName(world, args[1]);
                if (c == null) {
                    sender.sendMessage(
                        new TextComponentString("Country not found")
                    );
                    return;
                }
                if (!c.isAuthorized(playerUUID)) {
                    sender.sendMessage(
                        new TextComponentString(
                            "You are not authorized to deposit"
                        )
                    );
                    return;
                }
                long amount;
                try {
                    amount = Long.parseLong(args[2]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(
                        new TextComponentString("Invalid amount")
                    );
                    return;
                }
                try {
                    c.deposit(playerUUID, amount);
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(new TextComponentString(e.getMessage()));
                    return;
                }
                CountryStorage.get(countryWorld).markDirty();
                sender.sendMessage(
                    new TextComponentString(
                        "Deposited " +
                            MoneyFormatUtil.format(amount) +
                            " to " +
                            c.getName()
                    )
                );
                break;
            }
            // --- TRANSFER ---
            case "transfer": {
                if (args.length < 4) {
                    sender.sendMessage(
                        new TextComponentString(
                            "Usage: /country transfer <fromCountry> <toCountry> <amount>"
                        )
                    );
                    return;
                }
                Country from = CountryManager.getCountryByName(world, args[1]);
                Country to = CountryManager.getCountryByName(world, args[2]);
                if (from == null || to == null) {
                    sender.sendMessage(
                        new TextComponentString(
                            "One of the countries does not exist"
                        )
                    );
                    return;
                }
                if (!from.isAuthorized(playerUUID)) {
                    sender.sendMessage(
                        new TextComponentString(
                            "You are not authorized to transfer"
                        )
                    );
                    return;
                }
                long amount;
                try {
                    amount = Long.parseLong(args[3]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(
                        new TextComponentString("Invalid amount")
                    );
                    return;
                }
                try {
                    from.transfer(playerUUID, to, amount);
                    CountryStorage.get(countryWorld).markDirty();
                    sender.sendMessage(
                        new TextComponentString(
                            "Transferred " +
                                MoneyFormatUtil.format(amount) +
                                " from " +
                                from.getName() +
                                " to " +
                                to.getName()
                        )
                    );
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(new TextComponentString(e.getMessage()));
                }
                break;
            }
            // --- STATION UPGRADES ---
            case "station": {
                if (args.length < 3) {
                    sender.sendMessage(
                        new TextComponentString(
                            "Usage: /country station upgrade <1|2>"
                        )
                    );
                    return;
                }
                String action = args[1].toLowerCase();
                if (!"upgrade".equals(action)) {
                    sender.sendMessage(
                        new TextComponentString(
                            "Usage: /country station upgrade <1|2>"
                        )
                    );
                    return;
                }
                int tier;
                try {
                    tier = Integer.parseInt(args[2]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(
                        new TextComponentString("Invalid upgrade tier")
                    );
                    return;
                }
                if (tier != 1 && tier != 2) {
                    sender.sendMessage(
                        new TextComponentString(
                            "Invalid upgrade tier. Use 1 or 2"
                        )
                    );
                    return;
                }
                Country country = CountryManager.getCountryForPlayer(
                    world,
                    playerUUID
                );
                if (country == null) {
                    sender.sendMessage(
                        new TextComponentString(
                            "You are not part of any country"
                        )
                    );
                    return;
                }
                if (!country.isAuthorized(playerUUID)) {
                    sender.sendMessage(
                        new TextComponentString(
                            "You are not authorized to upgrade station capacity"
                        )
                    );
                    return;
                }
                int currentCap = country.getStationCap();
                long upgradeCost = tier == 1 ? 100_000L : 70_000L;
                long availableCredits = country.getResearchCredits();
                if (availableCredits < upgradeCost) {
                    sender.sendMessage(
                        new TextComponentString(
                            "Not enough research credits. Need " +
                                upgradeCost +
                                " RC"
                        )
                    );
                    return;
                }
                if (tier == 1) {
                    if (currentCap >= 1) {
                        sender.sendMessage(
                            new TextComponentString(
                                "Station cap tier 1 is already unlocked"
                            )
                        );
                        return;
                    }
                    if (currentCap != 0) {
                        sender.sendMessage(
                            new TextComponentString(
                                "Station cap tiers must be unlocked in order"
                            )
                        );
                        return;
                    }
                    country.setResearchCredits(availableCredits - upgradeCost);
                    country.setStationCap(1);
                } else {
                    if (currentCap >= 2) {
                        sender.sendMessage(
                            new TextComponentString(
                                "Station cap tier 2 is already unlocked"
                            )
                        );
                        return;
                    }
                    if (currentCap != 1) {
                        sender.sendMessage(
                            new TextComponentString(
                                "Station cap tier 1 must be unlocked first"
                            )
                        );
                        return;
                    }
                    country.setResearchCredits(availableCredits - upgradeCost);
                    country.setStationCap(2);
                }
                CountryStorage.get(countryWorld).markDirty();
                sender.sendMessage(
                    new TextComponentString(
                        "Station cap upgraded to " +
                            country.getStationCap() +
                            " for " +
                            country.getName() +
                            ". Spent " +
                            upgradeCost +
                            " RC"
                    )
                );
                break;
            }
            // --- PROMOTE ---
            case "promote": {
                if (args.length < 3) {
                    sender.sendMessage(
                        new TextComponentString(
                            "Usage: /country promote <countryName> <playerUUID>"
                        )
                    );
                    return;
                }
                Country c = CountryManager.getCountryByName(world, args[1]);
                if (c == null) {
                    sender.sendMessage(
                        new TextComponentString("Country not found")
                    );
                    return;
                }
                if (c.getRole(playerUUID) != Country.Role.PRESIDENT) {
                    sender.sendMessage(
                        new TextComponentString(
                            "Only the President can promote members"
                        )
                    );
                    return;
                }
                UUID member;
                try {
                    member = UUID.fromString(args[2]);
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(new TextComponentString("Invalid UUID"));
                    return;
                }
                if (c.promote(playerUUID, member)) {
                    CountryStorage.get(countryWorld).markDirty();
                    sender.sendMessage(
                        new TextComponentString(
                            "Promoted " +
                                member +
                                " to Minister in " +
                                c.getName()
                        )
                    );
                } else {
                    sender.sendMessage(
                        new TextComponentString("Promotion failed")
                    );
                }
                break;
            }
            // --- ALLIANCES ---
            case "ally": {
                if (args.length < 3) {
                    sender.sendMessage(
                        new TextComponentString(
                            "Usage: /country ally <request|accept|deny|remove> <countryName>"
                        )
                    );
                    return;
                }

                String action = args[1].toLowerCase();
                String targetName = args[2];

                Country self = CountryManager.getCountryForPlayer(
                    world,
                    playerUUID
                );
                if (self == null) {
                    sender.sendMessage(
                        new TextComponentString(
                            "You are not part of any country"
                        )
                    );
                    return;
                }
                if (!self.isPresident(playerUUID)) {
                    sender.sendMessage(
                        new TextComponentString(
                            "Only the President can manage alliances"
                        )
                    );
                    return;
                }

                Country target = CountryManager.getCountryByName(
                    world,
                    targetName
                );
                if (target == null) {
                    sender.sendMessage(
                        new TextComponentString("Country not found")
                    );
                    return;
                }
                if (target.getId().equals(self.getId())) {
                    sender.sendMessage(
                        new TextComponentString("You cannot ally with yourself")
                    );
                    return;
                }

                switch (action) {
                    case "request": {
                        if (self.isAlliedWith(target.getId())) {
                            sender.sendMessage(
                                new TextComponentString(
                                    "You are already allied with " +
                                        target.getName()
                                )
                            );
                            return;
                        }
                        if (target.hasIncomingAllianceRequest(self.getId())) {
                            sender.sendMessage(
                                new TextComponentString(
                                    "An alliance request is already pending"
                                )
                            );
                            return;
                        }
                        if (self.hasIncomingAllianceRequest(target.getId())) {
                            sender.sendMessage(
                                new TextComponentString(
                                    "You already have an incoming request from " +
                                        target.getName() +
                                        ". Use /country ally accept " +
                                        target.getName()
                                )
                            );
                            return;
                        }

                        target.addIncomingAllianceRequest(self.getId());
                        CountryStorage.get(countryWorld).markDirty();
                        sender.sendMessage(
                            new TextComponentString(
                                "Alliance request sent to " + target.getName()
                            )
                        );

                        UUID targetPresident = target.getPresidentUuid();
                        if (targetPresident != null) {
                            EntityPlayerMP presidentPlayer = server
                                .getPlayerList()
                                .getPlayerByUUID(targetPresident);
                            if (presidentPlayer != null) {
                                presidentPlayer.sendMessage(
                                    new TextComponentString(
                                        "Alliance request from " +
                                            self.getName() +
                                            ". Use /country ally accept " +
                                            self.getName() +
                                            " or /country ally deny " +
                                            self.getName()
                                    )
                                );
                            }
                        }

                        return;
                    }
                    case "accept": {
                        if (!self.hasIncomingAllianceRequest(target.getId())) {
                            sender.sendMessage(
                                new TextComponentString(
                                    "No incoming alliance request from " +
                                        target.getName()
                                )
                            );
                            return;
                        }
                        self.removeIncomingAllianceRequest(target.getId());
                        self.addAlly(target.getId());
                        target.addAlly(self.getId());
                        // If the other country had also queued a request, clear it.
                        target.removeIncomingAllianceRequest(self.getId());

                        CountryStorage.get(countryWorld).markDirty();
                        sender.sendMessage(
                            new TextComponentString(
                                "Alliance formed with " + target.getName()
                            )
                        );
                        UUID otherPresident = target.getPresidentUuid();
                        if (otherPresident != null) {
                            EntityPlayerMP presidentPlayer = server
                                .getPlayerList()
                                .getPlayerByUUID(otherPresident);
                            if (presidentPlayer != null) {
                                presidentPlayer.sendMessage(
                                    new TextComponentString(
                                        "Alliance formed with " + self.getName()
                                    )
                                );
                            }
                        }
                        return;
                    }
                    case "deny": {
                        if (!self.hasIncomingAllianceRequest(target.getId())) {
                            sender.sendMessage(
                                new TextComponentString(
                                    "No incoming alliance request from " +
                                        target.getName()
                                )
                            );
                            return;
                        }
                        self.removeIncomingAllianceRequest(target.getId());
                        CountryStorage.get(countryWorld).markDirty();
                        sender.sendMessage(
                            new TextComponentString(
                                "Alliance request from " +
                                    target.getName() +
                                    " was denied"
                            )
                        );
                        return;
                    }
                    case "remove": {
                        if (!self.isAlliedWith(target.getId())) {
                            sender.sendMessage(
                                new TextComponentString(
                                    "You are not allied with " +
                                        target.getName()
                                )
                            );
                            return;
                        }
                        self.removeAlly(target.getId());
                        target.removeAlly(self.getId());
                        CountryStorage.get(countryWorld).markDirty();
                        sender.sendMessage(
                            new TextComponentString(
                                "Alliance removed with " + target.getName()
                            )
                        );
                        return;
                    }
                    default:
                        sender.sendMessage(
                            new TextComponentString(
                                "Unknown ally action. Usage: /country ally <request|accept|deny|remove> <countryName>"
                            )
                        );
                        return;
                }
            }
            // --- WAR ---
            case "war": {
                if (args.length < 3) {
                    sender.sendMessage(
                        new TextComponentString(
                            "Usage: /country war <declare|end> <countryName>"
                        )
                    );
                    return;
                }

                String action = args[1].toLowerCase();
                String targetName = args[2];

                Country self = CountryManager.getCountryForPlayer(
                    world,
                    playerUUID
                );
                if (self == null) {
                    sender.sendMessage(
                        new TextComponentString(
                            "You are not part of any country"
                        )
                    );
                    return;
                }
                if (!self.isPresident(playerUUID)) {
                    sender.sendMessage(
                        new TextComponentString(
                            "Only the President can manage wars"
                        )
                    );
                    return;
                }

                Country target = CountryManager.getCountryByName(
                    world,
                    targetName
                );
                if (target == null) {
                    sender.sendMessage(
                        new TextComponentString("Country not found")
                    );
                    return;
                }
                if (target.getId().equals(self.getId())) {
                    sender.sendMessage(
                        new TextComponentString(
                            "You cannot declare war on yourself"
                        )
                    );
                    return;
                }
                if (self.isAlliedWith(target.getId())) {
                    sender.sendMessage(
                        new TextComponentString(
                            "You cannot declare war on an ally. Remove the alliance first"
                        )
                    );
                    return;
                }

                switch (action) {
                    case "declare": {
                        if (self.isAtWarWith(target.getId())) {
                            sender.sendMessage(
                                new TextComponentString(
                                    "You are already at war with " +
                                        target.getName()
                                )
                            );
                            return;
                        }
                        self.addWar(target.getId());
                        target.addWar(self.getId());
                        CountryStorage.get(countryWorld).markDirty();
                        sender.sendMessage(
                            new TextComponentString(
                                "War declared on " + target.getName()
                            )
                        );

                        UUID targetPresident = target.getPresidentUuid();
                        if (targetPresident != null) {
                            EntityPlayerMP presidentPlayer = server
                                .getPlayerList()
                                .getPlayerByUUID(targetPresident);
                            if (presidentPlayer != null) {
                                presidentPlayer.sendMessage(
                                    new TextComponentString(
                                        "War declared by " + self.getName()
                                    )
                                );
                            }
                        }
                        return;
                    }
                    case "end": {
                        if (!self.isAtWarWith(target.getId())) {
                            sender.sendMessage(
                                new TextComponentString(
                                    "You are not at war with " +
                                        target.getName()
                                )
                            );
                            return;
                        }
                        self.removeWar(target.getId());
                        target.removeWar(self.getId());
                        CountryStorage.get(countryWorld).markDirty();
                        sender.sendMessage(
                            new TextComponentString(
                                "War ended with " + target.getName()
                            )
                        );

                        UUID targetPresident = target.getPresidentUuid();
                        if (targetPresident != null) {
                            EntityPlayerMP presidentPlayer = server
                                .getPlayerList()
                                .getPlayerByUUID(targetPresident);
                            if (presidentPlayer != null) {
                                presidentPlayer.sendMessage(
                                    new TextComponentString(
                                        "War ended with " + self.getName()
                                    )
                                );
                            }
                        }
                        return;
                    }
                    default:
                        sender.sendMessage(
                            new TextComponentString(
                                "Unknown war action. Usage: /country war <declare|end> <countryName>"
                            )
                        );
                        return;
                }
            }
            // --- BOUNTY ---
            case "bounty": {
                if (args.length < 2) {
                    sender.sendMessage(
                        new TextComponentString(
                            "Usage: /country bounty add <playerName> <reward>"
                        )
                    );
                    return;
                }

                String action = args[1].toLowerCase();
                if (!"add".equals(action)) {
                    sender.sendMessage(
                        new TextComponentString(
                            "Unknown bounty action. Usage: /country bounty add <playerName> <reward>"
                        )
                    );
                    return;
                }
                if (args.length < 4) {
                    sender.sendMessage(
                        new TextComponentString(
                            "Usage: /country bounty add <playerName> <reward>"
                        )
                    );
                    return;
                }

                Country self = CountryManager.getCountryForPlayer(
                    world,
                    playerUUID
                );
                if (self == null) {
                    sender.sendMessage(
                        new TextComponentString(
                            "You are not part of any country"
                        )
                    );
                    return;
                }
                if (!self.isHighAuthority(playerUUID)) {
                    sender.sendMessage(
                        new TextComponentString(
                            "Only the President or a Minister can set bounties"
                        )
                    );
                    return;
                }

                EntityPlayerMP targetPlayer = server
                    .getPlayerList()
                    .getPlayerByUsername(args[2]);
                if (targetPlayer == null) {
                    sender.sendMessage(
                        new TextComponentString("Player not found")
                    );
                    return;
                }
                UUID targetUUID = targetPlayer.getUniqueID();
                Country targetCountry = CountryManager.getCountryForPlayer(
                    world,
                    targetUUID
                );
                if (targetCountry == null) {
                    sender.sendMessage(
                        new TextComponentString(
                            "Target player is not part of a country"
                        )
                    );
                    return;
                }
                if (!self.isAtWarWith(targetCountry.getId())) {
                    sender.sendMessage(
                        new TextComponentString(
                            "You can only set bounties on enemies during war"
                        )
                    );
                    return;
                }

                long reward;
                try {
                    reward = Long.parseLong(args[3]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(
                        new TextComponentString("Invalid reward amount")
                    );
                    return;
                }
                if (reward <= 0) {
                    sender.sendMessage(
                        new TextComponentString("Reward must be positive")
                    );
                    return;
                }
                if (self.getBalance() < reward) {
                    sender.sendMessage(
                        new TextComponentString("Insufficient country funds")
                    );
                    return;
                }

                self.setBounty(targetUUID, reward);
                CountryStorage.get(countryWorld).markDirty();
                server
                    .getPlayerList()
                    .sendMessage(
                        new TextComponentString(
                            self.getName() +
                                " has placed a bounty on " +
                                targetPlayer.getName() +
                                " for " +
                                reward
                        )
                    );
                return;
            }
            default:
                sender.sendMessage(
                    new TextComponentString(
                        "Unknown subcommand. Usage: " + getUsage(sender)
                    )
                );
        }
    }
}
