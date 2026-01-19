package com.baldeagle.country;

import java.util.UUID;

public class Country {
    private final String name; // The FTB Utilities team name
    private final UUID teamID; // Optional: team UUID if available
    private long balance;      // Country's economy balance

    public Country(String name, UUID teamID) {
        this.name = name;
        this.teamID = teamID;
        this.balance = 0L;
    }

    public String getName() {
        return name;
    }

    public UUID getTeamID() {
        return teamID;
    }

    public long getBalance() {
        return balance;
    }

    public void deposit(long amount) {
        balance += amount;
    }

    public boolean withdraw(long amount) {
        if (balance >= amount) {
            balance -= amount;
            return true;
        }
        return false;
    }
}