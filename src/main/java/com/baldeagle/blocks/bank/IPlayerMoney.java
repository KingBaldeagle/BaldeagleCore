package com.baldeagle.blocks.bank;

public interface IPlayerMoney {
    long getBalance();
    void add(long amount);
    boolean subtract(long amount);
}
