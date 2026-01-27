package com.baldeagle.oc;

import li.cil.oc.api.Driver;

public final class OCIntegration {

    private static boolean initialized = false;

    private OCIntegration() {}

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        Driver.add(new com.baldeagle.oc.driver.DriverCountryAtm());
        Driver.add(new com.baldeagle.oc.driver.DriverCountryBank());
        Driver.add(new com.baldeagle.oc.driver.DriverCountryMint());
        Driver.add(new com.baldeagle.oc.driver.DriverCountryExchange());
        Driver.add(new com.baldeagle.oc.driver.DriverCountryGov());
    }
}
