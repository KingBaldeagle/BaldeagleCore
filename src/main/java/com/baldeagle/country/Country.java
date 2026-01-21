package com.baldeagle.country;

import java.util.*;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

public class Country {

    public enum Role {
        PRESIDENT,
        MINISTER,
        MEMBER
    }

    private String name;
    private UUID id;
    private double balance;
    private final Map<UUID, Role> members = new HashMap<>();
    private final Set<UUID> joinRequests = new HashSet<>();

    // --- Constructor for NEW countries ---
    public Country(String name, UUID creator) {
        this.name = name;
        this.id = UUID.randomUUID();
        this.balance = 0;
        this.members.put(creator, Role.PRESIDENT);
    }

    public void applyInterest(double rate) {
        balance += balance * rate;
    }

    // --- Empty constructor for loading from NBT ---
    public Country(String name) {
        this.name = name;
        this.id = UUID.randomUUID();
        this.balance = 0;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }

    public Map<UUID, Role> getMembers() { return members; }
    public Set<UUID> getJoinRequests() { return joinRequests; }

    // --- Membership / roles ---
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

    public void requestJoin(UUID player) {
        joinRequests.add(player);
    }

    public boolean approveJoin(UUID byPlayer, UUID applicant) {
        if (!isAuthorized(byPlayer) || !joinRequests.contains(applicant)) return false;
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

    public void deposit(UUID byPlayer, double amount) {
        if (!isAuthorized(byPlayer)) throw new IllegalArgumentException("Not authorized");
        balance += amount;
    }

    public void transfer(UUID byPlayer, Country to, double amount) {
        if (!isAuthorized(byPlayer)) throw new IllegalArgumentException("Not authorized");
        if (amount > balance) throw new IllegalArgumentException("Insufficient funds");
        balance -= amount;
        to.balance += amount;
    }

    // --- NBT Saving ---
    public NBTTagCompound writeToNBT() {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setString("name", name);
        nbt.setString("id", id.toString());
        nbt.setDouble("balance", balance);

        // Members
        NBTTagList memberList = new NBTTagList();
        for (Map.Entry<UUID, Role> entry : members.entrySet()) {
            NBTTagCompound memberTag = new NBTTagCompound();
            memberTag.setString("uuid", entry.getKey().toString());
            memberTag.setString("role", entry.getValue().name());
            memberList.appendTag(memberTag);
        }
        nbt.setTag("members", memberList);

        // Join requests
        NBTTagList requestsList = new NBTTagList();
        for (UUID u : joinRequests) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString("uuid", u.toString());
            requestsList.appendTag(tag);
        }
        nbt.setTag("joinRequests", requestsList);

        return nbt;
    }

    // --- Load from NBT ---
    public static Country fromNBT(NBTTagCompound nbt) {
        String name = nbt.getString("name");
        UUID id = UUID.fromString(nbt.getString("id"));
        double balance = nbt.getDouble("balance");

        Country c = new Country(name); // empty constructor
        c.setId(id);
        c.setBalance(balance);

        // Members
        NBTTagList membersList = nbt.getTagList("members", 10);
        for (int i = 0; i < membersList.tagCount(); i++) {
            NBTTagCompound memberTag = membersList.getCompoundTagAt(i);
            UUID uuid = UUID.fromString(memberTag.getString("uuid"));
            Role role = Role.valueOf(memberTag.getString("role"));
            c.getMembers().put(uuid, role);
        }

        // Join requests
        NBTTagList requestsList = nbt.getTagList("joinRequests", 10);
        for (int i = 0; i < requestsList.tagCount(); i++) {
            NBTTagCompound reqTag = requestsList.getCompoundTagAt(i);
            c.getJoinRequests().add(UUID.fromString(reqTag.getString("uuid")));
        }

        return c;
    }
}