# 🏪 Shop System Design (Forge 1.12.2)

## Core Goals Recap

* Shops sell **physical items**
* Buyer pays from their **player wallet**
* Money becomes **physical cash** for the shop owner
* **8% tax** goes to the shop owner’s **country balance**
* Buyer can **only pay using the shop owner’s country currency**
* Multiplayer-safe & server-authoritative

---

## 1️⃣ Shop Block + Tile Entity

### Block

```java
BlockShop
```

* Opens a GUI
* Has an **owner UUID**
* Has a **linked country**
* Has an internal **item storage**
* Has a **cash storage**

### TileEntity

```java
TileEntityShop
```

Fields:

```java
UUID owner;
UUID countryId;
CurrencyType currency; // country currency
NonNullList<ItemStack> itemsForSale;
long[] prices; // price per slot
long cashStored;
```

Why TileEntity?

* Needs persistent inventory
* Needs ownership & country data
* Needs server-side validation

---

## 2️⃣ Shop GUI (Two Modes)

### 🧍 Owner View

* Add/remove items
* Set price per slot
* Withdraw physical money
* See tax rate (fixed 8%)
* See country balance impact

### 🧑 Buyer View

* View items
* See prices **in shop currency only**
* Buy button per slot
* No editing

You can detect mode via:

```java
player.getUniqueID().equals(shop.owner)
```

---

## 3️⃣ Currency Enforcement (Critical Rule)

When a player clicks **Buy**:

```java
if (playerWallet.getCurrency() != shop.currency) {
    fail("You must use " + shop.currency.getDisplayName());
}
```

💡 No automatic exchange here — that’s intentional.
If you want exchange, force players to use a **Currency Exchange Block** first.

This keeps:

* Inflation controlled
* Countries economically meaningful
* Shops politically relevant

---

## 4️⃣ Transaction Flow (Server-Side Only)

### Example Purchase: Item costs **100**

#### Step 1: Validate

* Item exists
* Buyer has ≥ 100
* Buyer currency == shop currency
* Shop owner country exists

#### Step 2: Calculate Tax

```java
long price = 100;
long tax = Math.floor(price * 0.08); // 8%
long ownerReceives = price - tax;    // 92
```

✔ Always round **down** to avoid duplication exploits.

---

### Step 3: Apply Money Changes

#### Buyer

```java
playerWallet.subtract(100);
```

#### Shop (Physical Cash)

```java
shop.cashStored += ownerReceives;
```

#### Country Treasury

```java
country.balance += tax;
```

#### Item Transfer

```java
giveItemToPlayer(buyer, itemStack.copy());
removeItemFromShop(slot);
```

---

## 5️⃣ Physical Money Handling

### Shop Cash Is NOT Virtual

* Stored internally as a number
* Only becomes **physical items** when withdrawn

When owner clicks **Withdraw**:

```java
CurrencyItemHelper.spawnMoney(
    shop.currency,
    shop.cashStored,
    ownerInventory
);
shop.cashStored = 0;
```

This prevents:

* Item spam
* Lag
* Duplication exploits

---

## 6️⃣ Data Model Summary

### Player Wallet (Capability)

```java
long balance;
CurrencyType currency;
```

### Country

```java
UUID id;
CurrencyType currency;
long treasuryBalance;
```

### Shop

```java
UUID owner;
UUID countryId;
CurrencyType currency;
Inventory items;
long cashStored;
```

---

## 7️⃣ Multiplayer & Anti-Exploit Rules

**Server-side checks only**

* Never trust GUI values
* Re-check item, price, balance on click

**Lock shop during transaction**

```java
synchronized(shop) {
    // process purchase
}
```

**Chunk unload safety**

* Save shop data in `writeToNBT`
* Reload cleanly in `readFromNBT`

---

## 9️⃣ Why This Design Works Well

✔ No floating point money
✔ Works with huge balances (use `long`)
✔ Keeps currency meaningful
✔ Encourages geopolitics
✔ Easy to extend into tariffs, embargoes, inflation
