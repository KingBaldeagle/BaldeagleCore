
---

# **Advanced Economy & Governance Features for BaldeagleCore**

These are **planned or optional enhancements** that extend the current country/economy system.

---

## **🏛️ Central Banks**

**Purpose:**
Central banks act as a governing financial authority for a country or server-wide economy.

**Features:**

* Each country may have **one central bank** block which is given to the president when the country is created.
* Holds **reserves of coins and bills**.

* Tracks **country-level inflation** and treasury.
* Only accessible by high-authority roles (president, treasurer).

**Implementation Notes:**

* **TileEntityCentralBank** extends `TileEntityBank`.
* GUI shows **country reserves**, issued currency, and interest settings.
* Updates `Country.money` and `Country.interestRate` automatically.

---

## **📈 Inflation**

**Purpose:**
Dynamic adjustment of currency value to simulate economic growth or scarcity.

**Features:**

* Inflation rate adjusts **value of coins and bills** over time.
* Controlled by **central bank**
* Affects **exchange rate for deposits/withdrawals**:

    * 1 Coin might become 0.95 of a unit if inflation is high.
* Can be used as a **gameplay mechanic** to encourage spending or trade.

**Implementation Notes:**

* Store an **inflation factor** per country in `CountryStorage`.
* Apply inflation during:

    * Deposit (reduce incoming value)
    * Withdrawal (adjust output)
    * Interest calculation (compound growth)
* Optional: Configurable global inflation for the server.

---

## **🪙 Physical Vault Storage**

**Purpose:**
Provides players/countries a **secure location for coins and bills**.

**Features:**

* New block: `Vault`.
* Stores **ItemCoin** and **ItemBill** physically in world.
* Can be **locked** for specific roles or players.
* Optional **reduces risk of loss** (e.g., death, theft mods integration).
* GUI displays stored currency and total value.

**Implementation Notes:**

* Use `TileEntityVault` with internal `ItemStack` inventory.
* Sync with player/country balance optionally.
* Can integrate with bank for deposits/withdrawals.

---

## **💳 Wire Transfers**

**Purpose:**
Allows sending currency **directly between players or countries** without physical coins.

**Features:**

* `/transfer <player/country> <amount>` command.
* Only authorized roles can send from **country balance**.
* A percentage fee of 5% is taken by the sender country if the player is doing a player to player transfer.
* Transfer history can be logged in server files.

**Implementation Notes:**

* Validate sender has **sufficient balance**.
* Deduct from sender, add to recipient.

---





## **Summary**

| Feature                   | Purpose                              | Player/Server Benefit                    |
| ------------------------- | ------------------------------------ | ---------------------------------------- |
| Central Bank              | Governing authority for currency     | Roleplay depth, interest control         |
| Inflation                 | Adjust currency value dynamically    | Simulate economy, encourage trade        |
| Vault Storage             | Secure coin/bill storage             | Reduce theft/loss, strategic planning    |
| Wire Transfers            | Remote transfers between accounts    | Convenience, roleplay economy            |

---

