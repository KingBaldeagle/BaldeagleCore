# 🏧 ATM Block

The **ATM Block** is a convenient interface for players and authorized government roles to **withdraw currency** from either their **personal balance** or their **country’s balance**.
It supports multiple denominations and enforces **role-based permissions**.

---

## 🔹 Overview

* Withdraw money from **your personal player balance**
* Withdraw money from **country balance** if authorized (President, Treasurer, or other roles)
* Output is in **physical currency items** (coins, bills) with the country’s **current currency ID**
* Supports **typed amount input**, automatically converting to available denominations
* Prevents overdraft: cannot withdraw more than your balance or your country’s available funds

---

## 🔐 Permissions

| Action                             | Who Can Do It                                         |
| ---------------------------------- | ----------------------------------------------------- |
| Withdraw from **personal balance** | Any player                                            |
| Withdraw from **country balance**  | Authorized roles only (President, Treasurer, etc.)    |
| Access ATM GUI                     | All players, but withdrawal restricted by permissions |

---

## 💵 Currency Handling

* Withdrawals are in the **currency of the player’s country**.
* Denominations are automatically calculated:

  * Coins (low value)
  * Bills (higher value)
* Ensures efficient, stackable output
* Supports future multi-currency if a player changes countries

Example:
If the player requests 137 units:

```
100 Bill x1
25 Coin x1
10 Coin x1
2 Coin x1
```

---

## 🖱️ User Interface

### GUI Features

* **Balance Display**: Shows current player or country balance
* **Amount Input**: Type the amount to withdraw
* **Withdraw Button**: Dispenses currency items
* **Balance Selector**: Select which balance to withdraw from

---

### Step-by-Step Use

1. Place ATM block in the world
2. Right-click to open the GUI
3. Enter withdrawal amount
4. Select source:

   * Player balance
   * Country balance (if authorized)
5. Click **Withdraw**
6. Receive coins/bills in your inventory

---

## ⚖️ Internal Mechanics

* Withdrawals **reduce the corresponding balance** immediately
* Denominations are calculated using a **greedy algorithm** (largest first)
* Block checks **current country currency ID** and pulls the **correct items** from the registry
* Prevents exploits: cannot create money without reducing balance

---

## 🧩 Integration

* Works seamlessly with **Country balance system**
* Uses **physical currency items**: coins and bills
* Compatible with **Vault Block** and **Central Bank systems**

---

## ⚠️ Notes & Best Practices

* Ensure the ATM has space to deposit currency items into player inventory
* Withdrawals exceeding available denominations may partially fail
* Only authorized roles should withdraw from the country balance
* Player balance withdrawals automatically match the player’s current country currency

---

### Example Scenario

**Player “Alice”** is part of **Country X**:

* Player balance: 500
* Country balance: 10,000

1. Alice clicks the ATM
2. Types 137
3. Withdraws from **player balance**
4. Receives:

   * 100 Bill x1
   * 25 Coin x1
   * 10 Coin x1
   * 2 Coin x1
5. Player balance is reduced to 363
6. Country balance remains unchanged

If Alice were the **President**, she could select **country balance**, withdraw 137 units, and **country balance would decrease**.
