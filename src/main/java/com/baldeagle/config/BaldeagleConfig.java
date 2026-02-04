package com.baldeagle.config;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public final class BaldeagleConfig {

    private static Configuration config;

    public static double wireTransferTaxRate = 0.02D;
    public static double wireTransferInterestRate = 0.01D;
    public static int wireTransferInterestThreshold = 1000;

    public static double shopBuyTax = 0.05D;
    public static double shopSellTax = 0.03D;
    public static double shopBulkInterestRate = 0.02D;
    public static int shopBulkThreshold = 500;

    public static double currencyConversionTax = 0.04D;
    public static double currencyConversionInterest = 0.02D;
    public static int currencyConversionInterestThreshold = 250;
    public static boolean allowReverseConversion = false;

    private BaldeagleConfig() {}

    public static void init(FMLPreInitializationEvent event) {
        config = new Configuration(event.getSuggestedConfigurationFile());
        sync();
    }

    public static void sync() {
        if (config == null) {
            return;
        }

        wireTransferTaxRate = config.getFloat(
            "taxRate",
            "wireTransfer",
            0.02F,
            0.0F,
            1.0F,
            "Base tax applied to every wire transfer."
        );
        wireTransferInterestRate = config.getFloat(
            "interestRate",
            "wireTransfer",
            0.01F,
            0.0F,
            1.0F,
            "Interest or processing fee multiplier applied to large transfers."
        );
        wireTransferInterestThreshold = config.getInt(
            "interestThreshold",
            "wireTransfer",
            1000,
            0,
            Integer.MAX_VALUE,
            "Minimum amount required before interest is applied."
        );

        shopBuyTax = config.getFloat(
            "buyTax",
            "shop",
            0.05F,
            0.0F,
            1.0F,
            "Tax applied when buying items from a shop."
        );
        shopSellTax = config.getFloat(
            "sellTax",
            "shop",
            0.03F,
            0.0F,
            1.0F,
            "Tax applied when selling items to a shop."
        );
        shopBulkInterestRate = config.getFloat(
            "bulkInterestRate",
            "shop",
            0.02F,
            0.0F,
            1.0F,
            "Optional interest-style fee for bulk purchases."
        );
        shopBulkThreshold = config.getInt(
            "bulkThreshold",
            "shop",
            500,
            0,
            Integer.MAX_VALUE,
            "Minimum transaction size before bulk interest applies."
        );

        currencyConversionTax = config.getFloat(
            "conversionTax",
            "currencyConverter",
            0.04F,
            0.0F,
            1.0F,
            "Base tax applied to all currency conversions."
        );
        currencyConversionInterest = config.getFloat(
            "conversionInterest",
            "currencyConverter",
            0.02F,
            0.0F,
            1.0F,
            "Conversion inefficiency factor (exchange spread)."
        );
        currencyConversionInterestThreshold = config.getInt(
            "interestThreshold",
            "currencyConverter",
            250,
            0,
            Integer.MAX_VALUE,
            "Minimum amount before interest is applied."
        );
        allowReverseConversion = config.getBoolean(
            "allowReverseConversion",
            "currencyConverter",
            false,
            "Allow reverse conversion (currency -> original money)."
        );

        if (config.hasChanged()) {
            config.save();
        }
    }
}
