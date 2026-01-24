Absolutely — this is a *core* piece of the economy, so good docs matter.
Below is **player-facing + admin-facing documentation** for the **Government Vault Block**, written like something you’d ship with the mod.

---

# 🏦 Government Vault Block

*National Reserves & Economic Stability*

---

## 📌 Overview

The **Government Vault** is a **secure, country-owned storage block** used to hold a nation’s **economic reserves**.
Assets stored in the vault directly **back the country’s currency**, influence **inflation**, and affect **exchange rates**.

Think of it as:

> 🏛️ *The physical backbone of a country’s economy*

---

## 🔐 Ownership & Access

* Each vault is **bound to a Country ID**
* Only authorized roles may interact:

  * 🧑‍⚖️ President
  * 💼 Treasurer


Unauthorized players:

* Cannot open the inventory
* Cannot break the block
* Cannot pipe items in or out

---

## 📦 What Can Be Stored

| Asset                   | Counts as Reserves | Notes                     |
| ----------------------- | ------------------ | ------------------------- |
| 🪙 Gold Ingots          | ✅ Yes             | Primary reserve asset     |
| 💎 Diamonds             | ✅ Yes             | High-value reserve        |
| 🧱 Emeralds             | ✅ Yes             | Can be enabled            |
| ❌ Coins / Bills         | ❌ No                | Currency is not a reserve |

> 💡 **Money stored in a vault does NOT increase reserves**
> Reserves must be **externally valuable assets**

---

## 🧮 Reserve Value Calculation

Each item has a **Reserve Weight**:

| Item       | Base Value |
| ---------- | ---------- |
| Gold Ingot | 1.0        |
| Diamond    | 4.0        |
| Emerald    | 2.0        |

```
TotalReserves =
Σ(itemCount × itemReserveValue)
```

Example:

```
64 Gold + 10 Diamonds
= 64 × 1.0 + 10 × 4.0
= 104 Reserve Units
```

---

## 📈 Effects on the Economy

### 1️⃣ Currency Strength

Vault reserves are used in:

* Currency exchange rates
* International trade
* Sanctions & war economy

Higher reserves → stronger currency

---

### 2️⃣ Inflation Control

When minting money:

```
MintingPower = Reserves / MoneySupply
```

If reserves rise:

* Inflation slows
* Currency stabilizes

If reserves fall:

* Inflation accelerates
* Currency weakens

---

### 3️⃣ Minting Requirement (Optional Rule)

Servers may enforce:

> **Money cannot be minted unless reserves exist**

```
MaxMintableMoney = Reserves × MintMultiplier
```

Example:

```
100 Reserves × 10 = 1,000 Currency
```

---

## 🧱 Physical Mechanics

* Vault is **not portable**
* Breaking requires:

  * Country leadership permissions
  * 30–60 second break time (configurable)
* Explodes contents on unauthorized break attempt (optional)

---

## 🖥️ GUI Features

Vault UI shows:

* 📦 Stored assets
* 📊 Total reserve value
* 📈 Backing ratio (Reserves / Money Supply)

Example:

```
Reserves: 312
Money Supply: 4,800
Backing Ratio: 0.065
Status: ⚠️ Overextended
```

---

## 🔁 Integration with Other Systems

### 🏭 Minting Block

* Pulls reserve data from the vault
* Consumes 1 gold for each currency coin/bill minted (First it uses the gold inputted into the mint gui if there is none present it will take from the vault)

---

### 🏦 Central Bank

* Vault acts as the **national treasury**
* Interest & bond systems use reserve value

---

### 💱 Currency Exchange Block

* Exchange rates reference vault-backed reserve value
* Weak reserves → terrible conversion rates


Perfect addition — this actually makes the system *feel real*.
Here’s a **drop-in documentation addendum** you can append to the Vault Block docs.

---

## 🔍 Reserve Tracking & External Access

### 📊 Tracked Reserve Inventory

The mod **actively tracks reserve assets stored in the Government Vault**, including:

* 🪙 **Gold Ingots**
* 💎 **Diamonds**
* 🧱 **Emeralds** (if enabled)

For each country, the following values are continuously recorded:

```
Gold Count
Diamond Count
Emerald Count
Total Reserve Value
```

These values are:

* Updated in real time
* Saved with the world
* Used by all economic systems (minting, inflation, exchange rates)

> ⚠️ Only items physically stored in a vault count as reserves
> Items in player inventories, banks, or chests do **not**

---

## 🔁 Reserve Access via Treasury Blocks

Reserves stored in the vault **do not need to be manually removed** from the vault block itself.

Instead, they can be accessed through **authorized government blocks**, such as:

* 🏦 **Central Bank Block**
* 🏭 **Minting Block**
* 🧾 **Treasury / Reserve Withdrawal Block**

These blocks interface directly with the vault’s tracked data.

---

## 🏧 Reserve Withdrawal Block (Government Use)

Authorized roles may use a **Reserve Withdrawal Block** to:

* Withdraw gold, diamonds, or emeralds
* Transfer assets to another vault
* Allocate reserves for:

  * Minting
  * Trade payments
  * War reparations
  * Emergency liquidity

### Permissions Required

* President
* Treasurer
* Central Bank Authority

---

### 📤 Withdrawal Rules

* Withdrawals **reduce national reserves immediately**
* Inflation and exchange rates update instantly
* Large withdrawals may trigger:

  * 📉 Currency devaluation
 

Example:

```
Withdraw: 20 Gold
Before Reserves: 120
After Reserves: 100
Backing Ratio ↓
```

---

## 🧠 Data Access (Internal & Automation)

Other blocks do **not physically pull items** from the vault inventory.

Instead, they query the **reserve ledger**, ensuring:

* No item duplication
* No desync between blocks
* Centralized economic authority

### Internal API Example

```java
VaultData data = VaultManager.get(countryId);

int gold = data.getGold();
int diamonds = data.getDiamonds();
int emeralds = data.getEmeralds();
```

---
