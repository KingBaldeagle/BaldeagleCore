
# 💰 Currency Creation & Multi-Country Money System

## Core Principles

1. **Money is not global**
   Each country has **its own currency**.
2. **Money is physical first, digital second**
   Coins/bills exist as items and can be deposited into banks.
3. **Only governments can mint**
   Currency creation is controlled by **minting blocks**.
4. **Exchange rates are dynamic**
   Value is based on **country reserves & economic health**, not hardcoded rates.

---

## 🏛️ Country Currency Model

Each `Country` has:

```java
UUID countryId;
String name;

long treasury;          // Total backing value (gold, diamonds, energy, etc.)
long moneyInCirculation;
double inflation;
double baseValue;       // Used for exchange rate calculation
```

> **Important rule:**
> 💡 *You cannot mint value from nothing* — minting increases circulation but affects inflation.

---

## 🪙 Physical Money Items

### Item Data (NBT)

Every coin or bill has:

```nbt
{
  country_id: "UUID",
  denomination: 1 | 5 | 10 | 50 | 100,
  type: "coin" | "bill"
}
```

### Why this is good

* One **ItemCoin** class can represent *all* currencies
* Texture/model is chosen by **denomination**
* Country identity is fully data-driven

---

## 🏭 Minting Block (Currency Creation)

### 🧱 Block: `BlockMint`

**Given to the president automatically when a country is created**

```java
onCountryCreate(player) {
    giveItem(player, new ItemStack(ModBlocks.MINT));
}
```

---

### 🔐 Access Rules

Only:

* President
* Treasurer
* Custom high-authority roles

---

### ⚙️ Mint GUI

Options:

* Select **coin or bill**
* Select **denomination**
* Select **amount**

Shows:

* Inflation impact
* New total money in circulation
* Current exchange rate

---

### 🧮 Minting Formula

```text
inflation += (minted_amount / treasury) * inflationFactor
moneyInCirculation += minted_amount
```

✔ Minting is **allowed**
❌ Over-minting causes **currency devaluation**

---

## 📈 Inflation Mechanics

Each country has:

```java
double inflation; // starts at 1.0
```

### Value of currency

```java
realValue = denomination / inflation
```

So:

* Inflation ↑ → currency worth less
* Mint too much → exchange rate drops

---

## 🔁 Currency Exchange Block

### 🧱 Block: `BlockCurrencyExchange`

This is **not player-to-player** — it’s an official exchange.

---

### 🖥️ Exchange GUI

* Input: coins/bills from country A
* Output: coins/bills from country B
* Shows:

    * Current exchange rate
    * Fee (optional)
    * Inflation impact

---

### 🧮 Exchange Rate Formula

```java
valueA = countryA.treasury / countryA.moneyInCirculation
valueB = countryB.treasury / countryB.moneyInCirculation

exchangeRate = valueA / valueB
```

Example:

* A is strong → 1 A coin = 3 B coins
* B is inflating → value drops automatically

---

### 🔒 Safety Rules

* Exchange consumes **physical items**
* Cannot exchange if:

    * Country B treasury too low
    * Exchange would destabilize economy

---

## 🏦 Banks (Player & Country)

### Player Bank

* Holds personal balance (by country)
* Can deposit/withdraw physical currency

### Country Bank

* Holds treasury
* Collects taxes
* Pays salaries
* Funds minting

---

## 🧾 Backing the Currency (Very Important)

Currency value is tied to **backing assets**, such as:

* Gold
* Diamonds
* Energy (RF)
* Vaulted items
* Taxes collected

```java
treasury += valueOfDepositedAssets;
```

This prevents:
❌ Infinite money
❌ Worthless currencies
✔ Creates real economic gameplay

---

## 🏗️ Example Gameplay Loop

1. Country created → President gets Mint block
2. Treasury funded with gold/diamonds
3. Coins minted carefully
4. Players earn & trade currency
5. Inflation reacts naturally
6. Exchange block enables global economy
7. Strong economies dominate naturally

---

## 🔥 Why This System Works

✅ Fully server-side
✅ No FTB dependencies
✅ Physical + digital economy
✅ Emergent gameplay
✅ Naturally balanced
✅ Roleplay friendly
✅ Extensible (OpenComputers, taxes, wars, sanctions)


