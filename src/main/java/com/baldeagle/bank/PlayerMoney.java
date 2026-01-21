package com.baldeagle.bank;

public class PlayerMoney implements IPlayerMoney {

    private long balance = 0;

    @Override
    public long getBalance() {
        return balance;
    }

    @Override
    public void add(long amount) {
        balance += amount;
    }

    @Override
    public boolean subtract(long amount) {
        if (balance >= amount) {
            balance -= amount;
            return true;
        }
        return false;
    }
}
