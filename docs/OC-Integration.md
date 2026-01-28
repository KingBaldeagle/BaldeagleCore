# OpenComputers Integration Documentation — Country Mod

## Overview

This mod exposes key blocks to **OpenComputers (OC)**, allowing players and governments to automate economy and country operations. All operations are **server-authoritative**, meaning Lua scripts cannot bypass economy rules or permissions.

**Key features:**

* Query player country and role
* Wire transfers with automatic country fee
* Minting and treasury access
* ATM operations
* Country management (for government computers)
* Read-only methods to inspect balances, reserves, and exchange rates

---

## 1️⃣ Component Names

| Block               | Component Name     | Notes                                |
| ------------------- | ------------------ | ------------------------------------ |
| ATM                 | `country_atm`      | Player balance access, withdraw only |
| Bank / Treasury     | `country_bank`     | Authorized roles only                |
| Mint                | `country_mint`     | Only president/treasurer can mint    |
| Currency Exchange   | `country_exchange` | All players can query rates          |
| Government Computer | `country_gov`      | Authorized government scripts        |

---

## 2️⃣ Player & Country Queries (All Blocks)

OC can check the **country membership** and **role** of any player.

### Method: `getPlayerCountry(uuid: string) → string | nil`

* Returns the **country ID** of the player, or `nil` if not in a country
* Example:

```lua
local country = component.country_bank.getPlayerCountry("player-uuid-1234")
if country then
  print("Player belongs to country:", country)
else
  print("Player is not part of any country")
end
```

### Method: `getPlayerRole(uuid: string) → string | nil`

* Returns the **role** of the player within the country (`citizen`, `treasurer`, `president`, etc.), or `nil` if not in a country
* Example:

```lua
local role = component.country_bank.getPlayerRole("player-uuid-1234")
print("Player role:", role or "none")
```

---

## 3️⃣ ATM Methods (`country_atm`)

Accessible by **all players**.

| Method                                   | Parameters     | Returns          | Notes                                                                     |
| ---------------------------------------- | -------------- | ---------------- | ------------------------------------------------------------------------- |
| `getBalance(uuid: string)`               | UUID of player | number           | Returns player's balance in country currency                              |
| `withdraw(uuid: string, amount: number)` | UUID, amount   | boolean, string? | Withdraws currency if enough funds. Returns false + error if insufficient |

Example:

```lua
local atm = component.country_atm
local ok, err = atm.withdraw("player-uuid", 100)
if ok then print("Withdraw successful") else print("Failed:", err) end
```

---

## 4️⃣ Bank/Treasury Methods (`country_bank`)

Accessible by **authorized roles only** (`treasurer`, `president`).

| Method                                                                   | Parameters    | Returns          | Notes                                           |
| ------------------------------------------------------------------------ | ------------- | ---------------- | ----------------------------------------------- |
| `getBalance()`                                                           | none          | number           | Returns country treasury balance                |
| `depositPlayer(uuid: string, amount: number)`                            | UUID, amount  | boolean, string? | Deposit funds into player account               |
| `withdrawPlayer(uuid: string, amount: number)`                           | UUID, amount  | boolean, string? | Withdraw from player account                    |
| `wireTransfer(senderUUID: string, receiverUUID: string, amount: number)` | UUIDs, amount | boolean, string? | Transfers funds with 5% fee to country treasury |
| `getWireFeeRate()`                                                       | none          | number           | Returns 0.05 for 5%                             |
| `getPlayerCountry(uuid: string)`                                         | UUID          | string           | Returns player's country ID                     |
| `getPlayerRole(uuid: string)`                                            | UUID          | string           | Returns player's role                           |

**Example: Wire Transfer**

```lua
local bank = component.country_bank
local ok, err = bank.wireTransfer("uuid1", "uuid2", 1000)
if ok then print("Transfer successful") else print("Failed:", err) end
```

---

## 5️⃣ Mint Methods (`country_mint`)

Accessible only by `treasurer` or `president`.

| Method                 | Parameters | Returns          | Notes                               |
| ---------------------- | ---------- | ---------------- | ----------------------------------- |
| `mint(amount: number)` | Amount     | boolean, string? | Creates currency. Server caps apply |
| `getCountryBalance()`  | none       | number           | Current country balance             |

**Example: Minting**

```lua
local mint = component.country_mint
local ok, err = mint.mint(10000)
if ok then print("Minted successfully") else print("Failed:", err) end
```

---

## 6️⃣ Currency Exchange (`country_exchange`)

Accessible by all players.

| Method                                                    | Parameters       | Returns | Notes                                        |
| --------------------------------------------------------- | ---------------- | ------- | -------------------------------------------- |
| `getExchangeRate(targetCurrency: string)`                 | Currency code    | number  | Current exchange rate                        |
| `convertCurrency(amount: number, targetCurrency: string)` | amount, currency | number  | Returns converted value including server fee |

---

## 7️⃣ Government Computer (`country_gov`)

Accessible by authorized government scripts. Allows **automation of policies and budgets**.

| Method                                    | Parameters   | Returns | Notes                                            |
| ----------------------------------------- | ------------ | ------- | ------------------------------------------------ |
| `paySalary(role: string, amount: number)` | role, amount | boolean | Pays salary to all players with the role         |
| `getInflation()`                          | none         | number  | Returns current inflation rate                   |
| `getReserves()`                           | none         | number  | Returns gold/asset reserves                      |


---

## 8️⃣ Permissions & Role Checks

* `getPlayerCountry()` and `getPlayerRole()` are **read-only**
* Methods modifying economy:

  * ATM: only withdraw player’s own funds
  * Bank: requires `treasurer` or `president`
  * Mint: requires `treasurer` or `president`
  * Government Computer: requires `treasurer` or `president`
* Wire transfers validate both player country membership and sender role automatically

---

## 9️⃣ Notes for Developers

* All OC methods **use server UUIDs** to prevent spoofing
* Wire transfer fee is automatically routed to the country treasury
* Economy logic **never runs purely client-side**
* OC access is configurable by **chunk, role, or whitelist**
* Methods return `(boolean success, string? error)` for consistency

---

## 🔹 10️⃣ Resources

* For developers integrating or scripting with OpenComputers:

* Official OC Wiki: https://ocdoc.cil.li/

Documentation on Lua components, callbacks, nodes, and the full OC API.

* OC GitHub Repository: https://github.com/MightyPirates/OpenComputers

 Source code, examples, and OC driver implementation.

* OC Lua API Reference: https://ocdoc.cil.li/api:component

 Full list of standard OC components, arguments, and return types.

* Lua Basics: https://www.lua.org/manual/5.2/

 Lua language reference for writing scripts for OC.

Example Scripts: Check examples folder in OC GitHub repository or community forums for practical automation scripts.
