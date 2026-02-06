# Configuration File Documentation (Minecraft Forge 1.12.2)

This document describes the configuration options available for the **economic and financial systems** in this mod. All values in this config are designed to be server-owner friendly and can be freely adjusted to balance gameplay, roleplay, or progression.

The configuration file is generated automatically on first launch and is located at:

```
/config/<modid>.cfg
```

Changes require a **server restart** (or singleplayer world reload) to take effect unless otherwise noted.

---

## General Notes

* All monetary values are **abstract units** defined by your mod’s currency system.
* Percentages are expressed as **decimal values**, not whole numbers.

  * Example: `0.05 = 5%`
* Setting values too high may significantly impact balance and inflation.

---

## Wire Transfer Settings

These settings control player-to-player and player-to-entity wire transfers.

```cfg
wireTransfer {
    # Base tax applied to every wire transfer
    # Range: 0.0 ~ 1.0
    D:taxRate=0.02

    # Interest or processing fee multiplier applied to large transfers
    # This is applied after tax
    # Example: 0.01 = +1% fee
    D:interestRate=0.01

    # Minimum amount required before interest is applied
    I:interestThreshold=1000
}
```

### Explanation

* **taxRate**
  Flat percentage removed from every transfer. Useful for sinks that remove money from the economy.

* **interestRate**
  Additional percentage applied when transferring large sums. This discourages instant wealth movement.

* **interestThreshold**
  Transfers below this value are not affected by interest.

---

## Shop Transaction Settings

Controls taxes and fees when interacting with NPC or block-based shops.

```cfg
shop {
    # Tax applied when buying items from a shop
    D:buyTax=0.05

    # Tax applied when selling items to a shop
    D:sellTax=0.03

    # Optional interest-style fee for bulk purchases
    D:bulkInterestRate=0.02

    # Minimum transaction size before bulk interest applies
    I:bulkThreshold=500
}
```

### Explanation

* **buyTax**
  Percentage added to the item price when a player buys from a shop.

* **sellTax**
  Percentage removed from the payout when a player sells to a shop.

* **bulkInterestRate**
  Additional fee applied to very large transactions to prevent shop abuse.

* **bulkThreshold**
  Minimum transaction value before bulk interest applies.

---

## Currency Converter Settings

Controls conversion rates, taxes, and inefficiencies when converting between currencies.

```cfg
currencyConverter {
    # Base tax applied to all currency conversions
    D:conversionTax=0.04

    # Conversion inefficiency factor
    # Simulates market loss or exchange spread
    D:conversionInterest=0.02

    # Minimum amount before interest is applied
    I:interestThreshold=250

    # Allow reverse conversion (currency -> original money)
    B:allowReverseConversion=false
}
```

### Explanation

* **conversionTax**
  Flat tax removed during conversion, regardless of currency type.

* **conversionInterest**
  Represents loss due to exchange rates, spread, or fees.

* **interestThreshold**
  Prevents small conversions from being penalized.

* **allowReverseConversion**
  If disabled, converted currency cannot be exchanged back into its source form.

---

## Example Balanced Setup

```cfg
wireTransfer {
    D:taxRate=0.02
    D:interestRate=0.01
    I:interestThreshold=1000
}

shop {
    D:buyTax=0.05
    D:sellTax=0.03
    D:bulkInterestRate=0.02
    I:bulkThreshold=500
}

currencyConverter {
    D:conversionTax=0.04
    D:conversionInterest=0.02
    I:interestThreshold=250
    B:allowReverseConversion=false
}
```

This setup favors early-game accessibility while discouraging large-scale abuse and hoarding.

---

## Tips for Server Owners

* Increase **tax values** to slow inflation.
* Increase **interest values** to discourage instant mass transfers.
* Disable reverse conversion to prevent money laundering loops.
* Pair with chunk- or time-based income systems for best balance.

---

## Troubleshooting

* **Config not generating:** Ensure the mod is loaded at least once.
* **Values not updating:** Restart the server after editing.
* **Economy breaking:** Reset values gradually instead of large jumps.

---

If you need this documentation exported as a README, wiki page, or in-code JavaDocs, it can be adapted easily.
