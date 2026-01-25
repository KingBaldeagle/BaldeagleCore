package com.baldeagle.country;

import com.baldeagle.country.mint.MintingConstants;
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
    private double balance;
    private long treasury;
    private long moneyInCirculation;
    private double inflation;
    private double baseValue;
    private double exchangeFee = 0.03D;

    private final Map<UUID, Role> members = new HashMap<>();
    private final Set<UUID> joinRequests = new HashSet<>();

    public Country(String name, UUID creator) {
        this.name = name;
        this.id = UUID.randomUUID();
        this.balance = 0;
        this.treasury = 0;
        this.moneyInCirculation = 1;
        this.inflation = 1.0D;
        this.baseValue = 1.0D;
        this.exchangeFee = 0.03D;
        this.members.put(creator, Role.PRESIDENT);
    }

    public Country(String name) {
        this.name = name;
        this.id = UUID.randomUUID();
        this.balance = 0;
        this.treasury = 0;
        this.moneyInCirculation = 1;
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

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public Map<UUID, Role> getMembers() {
        return members;
    }

    public Set<UUID> getJoinRequests() {
        return joinRequests;
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
        balance += balance * rate;
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

    public void deposit(UUID byPlayer, double amount) {
        if (!isAuthorized(byPlayer)) throw new IllegalArgumentException(
            "Not authorized"
        );
        balance += amount;
    }

    public void transfer(UUID byPlayer, Country to, double amount) {
        if (!isAuthorized(byPlayer)) throw new IllegalArgumentException(
            "Not authorized"
        );
        if (amount > balance) throw new IllegalArgumentException(
            "Insufficient funds"
        );
        balance -= amount;
        to.balance += amount;
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
        nbt.setDouble("balance", balance);
        nbt.setLong("treasury", treasury);
        nbt.setLong("circulation", moneyInCirculation);
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

        return nbt;
    }

    public static Country fromNBT(NBTTagCompound nbt) {
        String name = nbt.getString("name");
        UUID id = UUID.fromString(nbt.getString("id"));
        double balance = nbt.getDouble("balance");
        long treasury = nbt.getLong("treasury");
        long circulation = nbt.getLong("circulation");
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

        c.recalculateBaseValue();
        return c;
    }
}
