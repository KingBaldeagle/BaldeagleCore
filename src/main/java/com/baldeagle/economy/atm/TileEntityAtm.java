package com.baldeagle.economy.atm;

import net.minecraft.tileentity.TileEntity;

public class TileEntityAtm extends TileEntity {

    private String countryName;
    private long clientPlayerBalance = 0L;
    private long clientCountryBalance = 0L;

    public String getCountryName() {
        return countryName;
    }

    public void applySync(String countryName) {
        this.countryName = countryName;
    }

    public long getClientPlayerBalance() {
        return clientPlayerBalance;
    }

    public long getClientCountryBalance() {
        return clientCountryBalance;
    }

    public void applyBalanceSync(long playerBalance, long countryBalance) {
        this.clientPlayerBalance = Math.max(0L, playerBalance);
        this.clientCountryBalance = Math.max(0L, countryBalance);
    }
}
