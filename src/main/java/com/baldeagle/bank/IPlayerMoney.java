package com.baldeagle.bank;

public interface IPlayerMoney {
    long getBalance();
    void add(long amount);
    boolean subtract(long amount);
}
