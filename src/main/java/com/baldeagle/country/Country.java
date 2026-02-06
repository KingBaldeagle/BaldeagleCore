package com.baldeagle.country;

import com.baldeagle.blocks.mint.MintingConstants;
import java.util.*;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

public class Country {

    public enum Role {
        PRESIDENT,
        MINISTER,
        MEMBER,
    }

    private static final double MIN_INFLATION = 1.0D;
    private static final double MAX_INFLATION = 25.0D;

    private String name;
    private UUID id;
    private long balance;
    private long treasury;
    private long moneyInCirculation;
    private long researchCredits;
    private double inflation;
    private double baseValue;
    private double exchangeFee = 0.03D;

    private final Map<UUID, Role> members = new HashMap<>();
    private final Set<UUID> joinRequests = new HashSet<>();
    private final Set<UUID> allies = new HashSet<>();
    private final Set<UUID> incomingAllianceRequests = new HashSet<>();
    private final Set<UUID> wars = new HashSet<>();
    private final Map<UUID, Long> bounties = new HashMap<>();

    public Country(String name, UUID creator) {
        this.name = name;
        this.id = UUID.randomUUID();
        this.balance = 0L;
        this.treasury = 0;
        this.moneyInCirculation = 1;
        this.researchCredits = 0L;
        this.inflation = 1.0D;
        this.baseValue = 1.0D;
        this.exchangeFee = 0.03D;
        this.members.put(creator, Role.PRESIDENT);
    }

    public Country(String name) {
        this.name = name;
        this.id = UUID.randomUUID();
        this.balance = 0L;
        this.treasury = 0;
        this.moneyInCirculation = 1;
        this.researchCredits = 0L;
        this.inflation = 1.0D;
        this.baseValue = 1.0D;
        this.exchangeFee = 0.03D;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public long getBalance() {
        return balance;
    }

    public void setBalance(long balance) {
        this.balance = Math.max(0L, balance);
    }

    public Map<UUID, Role> getMembers() {
        return members;
    }

    public Set<UUID> getJoinRequests() {
        return joinRequests;
    }

    public Set<UUID> getAllies() {
        return allies;
    }

    public Set<UUID> getIncomingAllianceRequests() {
        return incomingAllianceRequests;
    }

    public Set<UUID> getWars() {
        return wars;
    }

    public Map<UUID, Long> getBounties() {
        return bounties;
    }

    public Long getBountyReward(UUID target) {
        return target == null ? null : bounties.get(target);
    }

    public void setBounty(UUID target, long reward) {
        if (target == null || reward <= 0) {
            return;
        }
        bounties.put(target, reward);
    }

    public boolean removeBounty(UUID target) {
        if (target == null) {
            return false;
        }
        return bounties.remove(target) != null;
    }

    public UUID getPresidentUuid() {
        for (Map.Entry<UUID, Role> entry : members.entrySet()) {
            if (entry.getValue() == Role.PRESIDENT) {
                return entry.getKey();
            }
        }
        return null;
    }

    public boolean isPresident(UUID player) {
        return getRole(player) == Role.PRESIDENT;
    }

    public boolean isAlliedWith(UUID otherCountryId) {
        return otherCountryId != null && allies.contains(otherCountryId);
    }

    public boolean isAtWarWith(UUID otherCountryId) {
        return otherCountryId != null && wars.contains(otherCountryId);
    }

    public void addWar(UUID otherCountryId) {
        if (otherCountryId == null || otherCountryId.equals(id)) {
            return;
        }
        wars.add(otherCountryId);
    }

    public void removeWar(UUID otherCountryId) {
        if (otherCountryId == null) {
            return;
        }
        wars.remove(otherCountryId);
    }

    public void addIncomingAllianceRequest(UUID fromCountryId) {
        if (fromCountryId == null || fromCountryId.equals(id)) {
            return;
        }
        incomingAllianceRequests.add(fromCountryId);
    }

    public boolean removeIncomingAllianceRequest(UUID fromCountryId) {
        if (fromCountryId == null) {
            return false;
        }
        return incomingAllianceRequests.remove(fromCountryId);
    }

    public boolean hasIncomingAllianceRequest(UUID fromCountryId) {
        return (
            fromCountryId != null &&
            incomingAllianceRequests.contains(fromCountryId)
        );
    }

    public void addAlly(UUID otherCountryId) {
        if (otherCountryId == null || otherCountryId.equals(id)) {
            return;
        }
        allies.add(otherCountryId);
    }

    public void removeAlly(UUID otherCountryId) {
        if (otherCountryId == null) {
            return;
        }
        allies.remove(otherCountryId);
    }

    public long getTreasury() {
        return treasury;
    }

    public void addTreasury(long amount) {
        if (amount <= 0) return;
        long beforeTreasury = treasury;
        treasury += amount;
        applyReservePressure(amount, beforeTreasury);
        recalculateBaseValue();
    }

    public boolean withdrawTreasury(long amount) {
        if (amount <= 0 || amount > treasury) {
            return false;
        }
        long beforeTreasury = treasury;
        treasury -= amount;
        applyReservePressure(-amount, beforeTreasury);
        recalculateBaseValue();
        return true;
    }

    public void adjustTreasury(long delta) {
        if (delta == 0) {
            return;
        }
        long beforeTreasury = treasury;
        if (delta > 0) {
            addTreasury(delta);
            return;
        }
        treasury = Math.max(0, treasury + delta);
        applyReservePressure(delta, beforeTreasury);
        recalculateBaseValue();
    }

    public long getMoneyInCirculation() {
        return Math.max(1, moneyInCirculation);
    }

    public long getResearchCredits() {
        return Math.max(0L, researchCredits);
    }

    public void addResearchCredits(long amount) {
        if (amount <= 0) {
            return;
        }
        long updated = researchCredits + amount;
        researchCredits = updated < researchCredits ? Long.MAX_VALUE : updated;
    }

    public boolean consumeResearchCredits(long amount) {
        if (amount <= 0 || researchCredits < amount) {
            return false;
        }
        researchCredits -= amount;
        return true;
    }

    public void addMoneyInCirculation(long amount) {
        if (amount <= 0) return;
        moneyInCirculation = Math.max(1, moneyInCirculation + amount);
        recalculateBaseValue();
    }

    public void removeFromCirculation(long amount) {
        if (amount <= 0) return;
        moneyInCirculation = Math.max(1, moneyInCirculation - amount);
        recalculateBaseValue();
        applyMoneyBurn(amount);
    }

    public double getInflation() {
        return inflation;
    }

    public void setInflation(double inflation) {
        this.inflation = clampInflation(inflation);
    }

    public double getBaseValue() {
        return baseValue;
    }

    public void recalculateBaseValue() {
        if (moneyInCirculation <= 0) {
            baseValue = 0;
        } else {
            baseValue = treasury / (double) moneyInCirculation;
        }
    }

    public double calculateInflationImpact(
        long mintedAmount,
        double inflationFactor
    ) {
        if (treasury <= 0 || mintedAmount <= 0) {
            return 0;
        }
        return (mintedAmount / (double) treasury) * inflationFactor;
    }

    public void applyMinting(long mintedAmount, double inflationFactor) {
        if (mintedAmount <= 0) {
            return;
        }
        addMoneyInCirculation(mintedAmount);
        double impact = calculateInflationImpact(mintedAmount, inflationFactor);
        setInflation(inflation + impact);
    }

    public double getRealValue(long faceValue) {
        if (faceValue <= 0) {
            return 0;
        }
        return faceValue / getInflation();
    }

    public double getExchangeValue() {
        if (moneyInCirculation <= 0) {
            return 0;
        }
        return (double) treasury / (double) moneyInCirculation;
    }

    public double getExchangeRateAgainst(Country other) {
        if (other == null) {
            return 0;
        }
        double otherValue = other.getExchangeValue();
        if (otherValue <= 0) {
            return 0;
        }
        return getExchangeValue() / otherValue;
    }

    public double getExchangeFee() {
        return exchangeFee;
    }

    public void setExchangeFee(double exchangeFee) {
        if (Double.isNaN(exchangeFee) || Double.isInfinite(exchangeFee)) {
            return;
        }
        this.exchangeFee = Math.max(0.0D, Math.min(0.25D, exchangeFee));
    }

    private double clampInflation(double value) {
        return Math.max(MIN_INFLATION, Math.min(MAX_INFLATION, value));
    }

    private void applyReservePressure(long deltaReserves, long treasuryBefore) {
        if (deltaReserves == 0) {
            return;
        }
        long denom = Math.max(1, treasuryBefore);
        double magnitude =
            (Math.abs(deltaReserves) / (double) denom) *
            MintingConstants.RESERVE_INFLATION_FACTOR;
        if (magnitude <= 0) {
            return;
        }
        if (deltaReserves > 0) {
            setInflation(inflation - magnitude);
        } else {
            setInflation(inflation + magnitude);
        }
    }

    private void applyMoneyBurn(long burnedAmount) {
        if (burnedAmount <= 0) {
            return;
        }
        long denom = Math.max(1, treasury);
        double magnitude =
            (burnedAmount / (double) denom) *
            MintingConstants.BURN_INFLATION_FACTOR;
        if (magnitude <= 0) {
            return;
        }
        setInflation(inflation - magnitude);
    }

    public void applyExchangePressure(long exchangedFaceValue) {
        if (exchangedFaceValue <= 0) {
            return;
        }
        long denom = Math.max(1, treasury);
        double magnitude =
            (exchangedFaceValue / (double) denom) *
            MintingConstants.EXCHANGE_PRESSURE_INFLATION_FACTOR;
        if (magnitude <= 0) {
            return;
        }
        setInflation(inflation + magnitude);
    }

    public void applyInterest(double rate) {
        if (balance <= 0) {
            return;
        }
        long interest = Math.round(balance * rate);
        if (interest > 0) {
            balance += interest;
        }
    }

    public boolean isMember(UUID player) {
        return members.containsKey(player);
    }

    public Role getRole(UUID player) {
        return members.getOrDefault(player, null);
    }

    public boolean isAuthorized(UUID player) {
        Role role = getRole(player);
        return role == Role.PRESIDENT || role == Role.MINISTER;
    }

    public void deposit(UUID byPlayer, long amount) {
        if (!isAuthorized(byPlayer)) throw new IllegalArgumentException(
            "Not authorized"
        );
        if (amount <= 0) {
            return;
        }
        balance += amount;
    }

    public void transfer(UUID byPlayer, Country to, long amount) {
        if (!isAuthorized(byPlayer)) throw new IllegalArgumentException(
            "Not authorized"
        );
        if (to == null || amount <= 0) {
            return;
        }
        if (amount > balance) throw new IllegalArgumentException(
            "Insufficient funds"
        );

        double taxRate =
            com.baldeagle.config.BaldeagleConfig.wireTransferTaxRate;
        double interestRate =
            com.baldeagle.config.BaldeagleConfig.wireTransferInterestRate;
        int interestThreshold =
            com.baldeagle.config.BaldeagleConfig.wireTransferInterestThreshold;

        double safeTaxRate = Math.max(0.0D, Math.min(1.0D, taxRate));
        double safeInterestRate = Math.max(0.0D, Math.min(1.0D, interestRate));

        long tax = (long) Math.floor(amount * safeTaxRate);
        long afterTax = Math.max(0L, amount - tax);
        long interest = 0L;
        if (amount >= interestThreshold && safeInterestRate > 0.0D) {
            interest = (long) Math.floor(afterTax * safeInterestRate);
        }
        long netTransfer = Math.max(0L, afterTax - interest);
        if (netTransfer <= 0) {
            throw new IllegalArgumentException(
                "Transfer amount too small after fees."
            );
        }

        balance -= amount;
        to.balance += netTransfer;
    }

    public void requestJoin(UUID player) {
        joinRequests.add(player);
    }

    public boolean approveJoin(UUID byPlayer, UUID applicant) {
        if (
            !isAuthorized(byPlayer) || !joinRequests.contains(applicant)
        ) return false;
        members.put(applicant, Role.MEMBER);
        joinRequests.remove(applicant);
        return true;
    }

    public void denyJoin(UUID byPlayer, UUID applicant) {
        if (!isAuthorized(byPlayer)) return;
        joinRequests.remove(applicant);
    }

    public boolean promote(UUID byPlayer, UUID target) {
        if (getRole(byPlayer) != Role.PRESIDENT) return false;
        if (!members.containsKey(target)) return false;
        members.put(target, Role.MINISTER);
        return true;
    }

    public boolean isHighAuthority(UUID player) {
        Role role = getRole(player);
        return role == Role.PRESIDENT || role == Role.MINISTER;
    }

    public NBTTagCompound writeToNBT() {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setString("name", name);
        nbt.setString("id", id.toString());
        nbt.setLong("balance", balance);
        nbt.setLong("treasury", treasury);
        nbt.setLong("circulation", moneyInCirculation);
        nbt.setLong("researchCredits", researchCredits);
        nbt.setDouble("inflation", inflation);
        nbt.setDouble("baseValue", baseValue);
        nbt.setDouble("exchangeFee", exchangeFee);

        NBTTagList memberList = new NBTTagList();
        for (Map.Entry<UUID, Role> entry : members.entrySet()) {
            NBTTagCompound memberTag = new NBTTagCompound();
            memberTag.setString("uuid", entry.getKey().toString());
            memberTag.setString("role", entry.getValue().name());
            memberList.appendTag(memberTag);
        }
        nbt.setTag("members", memberList);

        NBTTagList requestsList = new NBTTagList();
        for (UUID u : joinRequests) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString("uuid", u.toString());
            requestsList.appendTag(tag);
        }
        nbt.setTag("joinRequests", requestsList);

        NBTTagList alliesList = new NBTTagList();
        for (UUID ally : allies) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString("id", ally.toString());
            alliesList.appendTag(tag);
        }
        nbt.setTag("allies", alliesList);

        NBTTagList incomingList = new NBTTagList();
        for (UUID req : incomingAllianceRequests) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString("id", req.toString());
            incomingList.appendTag(tag);
        }
        nbt.setTag("incomingAllianceRequests", incomingList);

        NBTTagList warsList = new NBTTagList();
        for (UUID war : wars) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString("id", war.toString());
            warsList.appendTag(tag);
        }
        nbt.setTag("wars", warsList);

        NBTTagList bountyList = new NBTTagList();
        for (Map.Entry<UUID, Long> entry : bounties.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString("uuid", entry.getKey().toString());
            tag.setLong("reward", entry.getValue());
            bountyList.appendTag(tag);
        }
        nbt.setTag("bounties", bountyList);

        return nbt;
    }

    public static Country fromNBT(NBTTagCompound nbt) {
        String name = nbt.getString("name");
        UUID id = UUID.fromString(nbt.getString("id"));
        long balance = readLegacyLong(nbt, "balance");
        long treasury = nbt.getLong("treasury");
        long circulation = nbt.getLong("circulation");
        long researchCredits = nbt.hasKey("researchCredits")
            ? nbt.getLong("researchCredits")
            : 0L;
        double inflation = nbt.getDouble("inflation");
        double baseValue = nbt.getDouble("baseValue");
        double exchangeFee = nbt.hasKey("exchangeFee")
            ? nbt.getDouble("exchangeFee")
            : 0.03D;

        Country c = new Country(name);
        c.setId(id);
        c.setBalance(balance);
        c.treasury = Math.max(0, treasury);
        c.moneyInCirculation = Math.max(1, circulation);
        c.researchCredits = Math.max(0L, researchCredits);
        c.inflation = inflation <= 0 ? 1.0D : c.clampInflation(inflation);
        c.baseValue = baseValue;
        c.setExchangeFee(exchangeFee);

        NBTTagList membersList = nbt.getTagList("members", 10);
        for (int i = 0; i < membersList.tagCount(); i++) {
            NBTTagCompound memberTag = membersList.getCompoundTagAt(i);
            UUID uuid = UUID.fromString(memberTag.getString("uuid"));
            Role role = Role.valueOf(memberTag.getString("role"));
            c.getMembers().put(uuid, role);
        }

        NBTTagList requestsList = nbt.getTagList("joinRequests", 10);
        for (int i = 0; i < requestsList.tagCount(); i++) {
            NBTTagCompound reqTag = requestsList.getCompoundTagAt(i);
            c.getJoinRequests().add(UUID.fromString(reqTag.getString("uuid")));
        }

        if (nbt.hasKey("allies")) {
            NBTTagList alliesList = nbt.getTagList("allies", 10);
            for (int i = 0; i < alliesList.tagCount(); i++) {
                NBTTagCompound tag = alliesList.getCompoundTagAt(i);
                String raw = tag.getString("id");
                try {
                    c.getAllies().add(UUID.fromString(raw));
                } catch (IllegalArgumentException ignored) {}
            }
        }

        if (nbt.hasKey("incomingAllianceRequests")) {
            NBTTagList incomingList = nbt.getTagList(
                "incomingAllianceRequests",
                10
            );
            for (int i = 0; i < incomingList.tagCount(); i++) {
                NBTTagCompound tag = incomingList.getCompoundTagAt(i);
                String raw = tag.getString("id");
                try {
                    c.getIncomingAllianceRequests().add(UUID.fromString(raw));
                } catch (IllegalArgumentException ignored) {}
            }
        }

        if (nbt.hasKey("wars")) {
            NBTTagList warsList = nbt.getTagList("wars", 10);
            for (int i = 0; i < warsList.tagCount(); i++) {
                NBTTagCompound tag = warsList.getCompoundTagAt(i);
                String raw = tag.getString("id");
                try {
                    c.getWars().add(UUID.fromString(raw));
                } catch (IllegalArgumentException ignored) {}
            }
        }

        if (nbt.hasKey("bounties")) {
            NBTTagList bountyList = nbt.getTagList("bounties", 10);
            for (int i = 0; i < bountyList.tagCount(); i++) {
                NBTTagCompound tag = bountyList.getCompoundTagAt(i);
                String raw = tag.getString("uuid");
                long reward = tag.getLong("reward");
                if (reward <= 0) {
                    continue;
                }
                try {
                    c.getBounties().put(UUID.fromString(raw), reward);
                } catch (IllegalArgumentException ignored) {}
            }
        }

        c.recalculateBaseValue();
        return c;
    }

    private static long readLegacyLong(NBTTagCompound nbt, String key) {
        if (nbt == null || key == null || !nbt.hasKey(key)) {
            return 0L;
        }
        int type = nbt.getTagId(key);
        if (type == 4) {
            // long
            return Math.max(0L, nbt.getLong(key));
        }
        if (type == 3) {
            // int
            return Math.max(0L, (long) nbt.getInteger(key));
        }
        if (type == 6) {
            // double
            return Math.max(0L, (long) Math.floor(nbt.getDouble(key)));
        }
        if (type == 1) {
            // byte
            return Math.max(0L, (long) nbt.getByte(key));
        }
        if (type == 2) {
            // short
            return Math.max(0L, (long) nbt.getShort(key));
        }
        if (type == 5) {
            // float
            return Math.max(0L, (long) Math.floor(nbt.getFloat(key)));
        }
        return 0L;
    }
}
