package com.baldeagle.country;

import java.util.*;

public class Country {

    public enum Role {
        PRESIDENT,
        MINISTER,
        CITIZEN
    }

    private final String name;
    private final UUID id;
    private double balance;

    private final Map<UUID, Role> members = new HashMap<>();
    private final Set<UUID> joinRequests = new HashSet<>();

    public Country(String name, UUID creatorUUID) {
        this.name = name;
        this.id = UUID.randomUUID();
        this.balance = 0.0;
        members.put(creatorUUID, Role.PRESIDENT); // creator is president
    }

    public String getName() { return name; }
    public UUID getId() { return id; }
    public double getBalance() { return balance; }

    public Role getRole(UUID player) { return members.get(player); }

    public boolean isAuthorized(UUID player) {
        Role role = getRole(player);
        return role == Role.PRESIDENT || role == Role.MINISTER;
    }

    // --- Join Requests ---
    public void requestJoin(UUID player) { joinRequests.add(player); }

    public boolean approveJoin(UUID approver, UUID applicant) {
        if (!isAuthorized(approver)) return false;
        if (!joinRequests.contains(applicant)) return false;
        members.put(applicant, Role.CITIZEN);
        joinRequests.remove(applicant);
        return true;
    }

    public void denyJoin(UUID approver, UUID applicant) {
        if (!isAuthorized(approver)) return;
        joinRequests.remove(applicant);
    }

    public Set<UUID> getJoinRequests() { return Collections.unmodifiableSet(joinRequests); }

    // --- Economy ---
    public void deposit(UUID player, double amount) {
        if (!isAuthorized(player)) throw new IllegalArgumentException("Not authorized");
        if (amount < 0) throw new IllegalArgumentException("Cannot deposit negative money");
        balance += amount;
    }

    public void withdraw(UUID player, double amount) {
        if (!isAuthorized(player)) throw new IllegalArgumentException("Not authorized");
        if (amount < 0) throw new IllegalArgumentException("Cannot withdraw negative money");
        if (balance < amount) throw new IllegalArgumentException("Not enough funds");
        balance -= amount;
    }

    public void transfer(UUID player, Country target, double amount) {
        if (!isAuthorized(player)) throw new IllegalArgumentException("Not authorized");
        if (balance < amount) throw new IllegalArgumentException("Not enough funds");
        balance -= amount;
        target.balance += amount;
    }
    // Promote a member to Minister
    public boolean promote(UUID promoter, UUID member) {
        if (getRole(promoter) != Role.PRESIDENT) return false; // Only President can promote
        if (!members.containsKey(member)) return false; // Player must be a member
        members.put(member, Role.MINISTER);
        return true;
    }


    public Map<UUID, Role> getMembers() {
        return Collections.unmodifiableMap(members);
    }
}
