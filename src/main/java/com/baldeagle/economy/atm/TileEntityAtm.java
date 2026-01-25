package com.baldeagle.economy.atm;

import net.minecraft.tileentity.TileEntity;

public class TileEntityAtm extends TileEntity {

    private String countryName;

    public String getCountryName() {
        return countryName;
    }

    public void applySync(String countryName) {
        this.countryName = countryName;
    }
}
