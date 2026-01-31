# **BaldeagleCore – Minecraft Geopolitics Mod**

**Version:** 1.0 (custom)
**Minecraft:** 1.12.2
**Forge:** 14.23.5.x

**Author:** Baldeagle
**Dependencies:** None required, optional integration with Opencomputers.

---

## **Overview**

**BaldeagleCore** is a custom Minecraft mod designed for geopolitics-style roleplay servers. It introduces:

* **Countries**: Player-created nations with presidents and roles.
* **Economy**: Player and country banking, coins, bills, and interest.
* **Banks**: Blocks that allow players to deposit currency and interact with the economy.
* **Persistent storage**: All data (countries, balances, roles) is saved with the world.

This mod is **self-contained**, handling currency, countries, and roles independently, while optionally integrating with FTB Teams for chunk ownership management.

---

## **Key Features**

### **1. Countries**

* Players can **create countries**.
* Country creation validates **unique names**.
* Country creator becomes **President**.
* Roles can be assigned through **roleplay-based voting**.
* Players can **request to join countries**; presidents or high-authority roles approve requests.
* Country data is **persistent across world loads**.

### **2. Economy**

* **Player balances** track individual money.
* **Country balances** aggregate deposits from members.
* **Deposits** and **transfers** restricted to players with proper roles.
* **Coins and bills** as physical items:

   * `ItemCoin` (denominations: 1, 5, etc.)
   * `ItemBill` (denominations: 10, etc.)
* Currency can be **deposited into banks**, converted to balance values.
* **Interest system** adds growth to player and country deposits over time.

### **3. Bank Block**

* Right-click opens a **custom GUI**.
* Allows deposits to:

   * **Player balance**
   * **Country balance** (if member of a country)
* Applies **interest over time**.
* Supports **coin/bill items**.
* Configurable interest rate (`TileEntityBank` handles periodic interest application).

### **4. Commands**

* `/countrymoney balance` — shows **player or country balance**.
* `/country list` — lists **all created countries**.
* `/country joinrequests` — shows **pending requests** to join a country.
* `/country create <name>` — creates a new country.
* `/countrymoney deposit <amount>` — deposits currency into bank.
* Role restrictions enforced: only authorized players can deposit, transfer, or manage funds.

### **5. Creative Tab**

* Custom **“Economy” creative tab**:

   * Groups all coins, bills, and bank blocks.
   * Icon set to `coin_1`.
   * Tab shows items in inventory and creative mode cleanly.

### **6. Persistence**

* All data (countries, player balances, country balances) is stored **in the world**.
* Data survives world reloads and server restarts.
* Uses:

   * `CountryStorage` for world-level country data.
   * Player capabilities for individual balances.

---

## **Items Added**

| Item    | Description      | Creative Tab |
| ------- | ---------------- | ------------ |
| Coin_1  | Single unit coin | Economy      |
| Coin_5  | 5-unit coin      | Economy      |
| Bill_10 | 10-unit bill     | Economy      |

---

## **Blocks Added**

| Block | Description                                                | GUI Available |
| ----- | ---------------------------------------------------------- | ------------- |
| Bank  | Deposit coins/bills, add money to balances, apply interest | Yes           |

---

## **Mod Architecture**

* **Core**

   * `CountryManager` — manages countries, members, roles.
   * `CountryStorage` — persistent storage of country data.
* **Economy**

   * `ItemCoin`, `ItemBill` — currency items.
   * Player balances tracked via **capabilities**.
* **Bank**

   * `BlockBank` — physical bank block.
   * `TileEntityBank` — handles interest and server ticks.
   * `GuiBank` / `ContainerBank` — GUI and container logic.
* **Client**

   * `ClientModelRegistry` — handles item/block model registration.
   * Custom **creative tab** for economy items.

---


## **Mod Dependencies**

* **Forge 1.12.2**
No other mods are required.

---

## **Future Features (Planned / Possible)**

* **Central bank** mechanics
* **Inflation / dynamic currency values**
* **Vaults for physical storage**
* **Wire transfers between players/countries**
* **Taxes and upkeep**
* **OpenComputers / scripting integration**
