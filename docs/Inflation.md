# 📈 Inflation System

Inflation represents the **loss of purchasing power** of a country’s currency over time.
In this mod, inflation is **fully systemic**, driven by player actions — not arbitrary timers.

Inflation affects **prices, exchange rates, taxes, upkeep, and economic stability**.

---

## 🔥 What Causes Inflation?

Inflation rises when **money supply grows faster than economic backing**.

### Primary Inflation Sources

### 🏭 1. Excessive Minting

* Minting coins/bills without sufficient reserves
* Printing money faster than gold/diamond backing increases
* Emergency minting during wars or crises

> Printing money is easy. Fixing inflation is not.

---

### 🪙 2. Weak Reserve Backing

* Low ratio of reserves to total money supply
* Withdrawing gold/diamonds from the vault
* Spending reserves without reducing money supply

---

### 🔁 4. Currency Exchange Pressure

* Large-scale conversions into stronger currencies
* Trade imbalances

---

## 📊 How Inflation Is Calculated (Conceptually)

Each country tracks:

```
Money Supply (coins + bills in circulation)
Reserve Value (gold, diamonds, emeralds)
Backing Ratio = Reserve Value / Money Supply
```

Inflation rises when:

```
Backing Ratio ↓
Money Supply ↑ faster than Reserves
```

---

## ⚠️ Effects of Inflation

### 💰 Economic Effects

* Prices increase (shops, upkeep, services)
* Taxes must rise to maintain income
* Minted money yields diminishing returns

---

### 🌍 International Effects

* Currency devalues in exchanges
* Imports become more expensive
* Foreign trade favors stronger economies


## 📉 How to Reduce Inflation

Inflation **never drops automatically**.
It must be *actively managed*.

---

## ✅ 1. Increase Reserves

The most reliable method.

### How:

* Deposit gold, diamonds, or emeralds into the vault
* Secure loot, mines, or trade deals
* Recover reserves from war reparations

### Result:

```
Reserve Value ↑
Backing Ratio ↑
Inflation ↓
```

---

## 🧮 Inflation Decay Formula (Example)

```
inflationChange =
    mintingPressure
  + spendingPressure
  - reserveGrowth
  - moneyBurned
```

Inflation decays **gradually**, not instantly.
