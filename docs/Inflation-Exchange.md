# 💱 Currency Exchange System

## Overview

The currency exchange system allows players, governments, and automated systems to convert one country’s currency into another.

Exchange rates are **not fixed** and are **entirely player-driven**, based on:

* 🪙 **Total money supply** of each country
* 🏛️ **Physical reserves** stored in government vaults
* 📈 **Backing strength** (reserves vs money printed)
* 💸 **Exchange fees** and liquidity pressure

This creates a dynamic, realistic economy where currencies rise and fall based on how responsibly a country manages its finances.

---

## Core Principles

### 1️⃣ Currency Has No Intrinsic Value

A currency is only worth what the issuing country can **back** it with.

A country that prints too much money without increasing reserves will see:

* Weaker exchange rates
* Higher inflation
* Worse conversion outcomes

---

### 2️⃣ Reserves Define Strength

Each country has **tracked reserves**, typically:

* Gold
* Diamonds
* Emeralds

These reserves represent **real economic backing**.

More reserves → stronger currency
Less reserves → weaker currency

---

### 3️⃣ Money Supply Matters

The total amount of currency a country has issued directly impacts its value.

* Low money supply + high reserves = strong currency
* High money supply + low reserves = weak currency

---

## Exchange Rate Calculation (Conceptual)

The exchange block compares **two countries**:

* **Source country (A)**
* **Target country (B)**

### Step 1: Calculate Backing Ratio

```
Backing Ratio = Total Reserves / Total Money Supply
```

This ratio represents how well-backed the currency is.

---

### Step 2: Determine Relative Value

The exchange rate is derived from the **relative strength** of both currencies:

```
Exchange Rate (A → B) =
(B Backing Ratio / A Backing Ratio)
```

This means:

* Converting **from a weak currency to a strong one** costs more
* Converting **from a strong currency to a weak one** yields more units

---

### Step 3: Apply Liquidity & Pressure

Large exchanges introduce **market pressure**:

* Massive conversions weaken the source currency
* The system may temporarily worsen rates for large transactions
* Prevents instant economic abuse

---

## Exchange Fee

Every conversion includes a **non-refundable fee**, representing:

* Banking costs
* Market friction
* Government tariffs

### Example Fee Formula

```
Final Amount = Converted Amount × (1 - Fee)
```

Typical fee ranges:

* 🌍 International exchange: 3–8%
* ⚠️ Unstable currencies: higher fees

Fees are:

* Sent to the target exchange country

---

## Example Exchange

### Country A

* Money Supply: 10,000
* Reserves: 100 gold
* Backing Ratio: 0.01

### Country B

* Money Supply: 5,000
* Reserves: 200 gold
* Backing Ratio: 0.04

---

### Exchange: 1,000 A → B

```
Raw Rate = 0.04 / 0.01 = 4.0
Converted = 1,000 / 4 = 250 B
Fee (5%) = 12.5
Final Output = 237.5 B
```

---

## Economic Consequences

Currency exchange is **not neutral**.

### Excessive Exchanging Can:

* Drain reserves
* Weaken a currency
* Increase inflation

### Healthy Economies:

* Offer better exchange rates
* Attract foreign trade
* Encourage reserve accumulation

---

## Player Strategy Implications

* Hoarding reserves strengthens national currency
* Printing money without backing destroys exchange power
* Strong currencies dominate international trade
* Weak currencies rely on fees, tariffs, or protectionism
*
---

## Summary

| Factor               | Effect               |
| -------------------- | -------------------- |
| Reserves ↑           | Currency strengthens |
| Money Supply ↑       | Currency weakens     |
| High Exchange Volume | Rate worsens         |
| Responsible Minting  | Stable exchange      |
| Fees                 | Prevent abuse        |
