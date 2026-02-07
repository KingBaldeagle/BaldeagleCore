# BaldeagleCore ↔ Advanced Rocketry Integration

### Country-Based Space Station Authorization System

## Overview

This integration replaces Advanced Rocketry’s default player-based space station system with a **country-controlled authorization model** managed by **BaldeagleCore**.

By default:

* **No country can create space stations**
* Space station capacity is unlocked via **FTB Quests**
* Space stations are **owned and limited at the country level**
* Advanced Rocketry handles **rockets and dimensions only**
* BaldeagleCore is the **single authority** for station permissions

This prevents:

* Station spam
* Player bypasses
* Dimension abuse
* Client-side exploits

---

## Design Goals

* Countries, not players, own space stations
* Station limits are **explicitly unlocked**
* Progression is **quest-driven**
* All checks are **server-side**
* No Advanced Rocketry fork required

---

## Default Rules

| Rule                 | Value                       |
| -------------------- | --------------------------- |
| Starting station cap | `0`                         |
| Upgrade tier 1       | `1` station                 |
| Upgrade tier 2       | `2` stations                |
| Max stations         | `2`                         |
| Ownership            | Country                     |
| Enforcement          | Server-side (BaldeagleCore) |

---

## How It Works (High Level)

1. Player attempts to create a space station
2. BaldeagleCore intercepts the creation request
3. Player’s country is checked
4. If the country has not unlocked station capacity → **creation denied**
5. FTB Quest completion increases the country’s station cap
6. Once unlocked, station creation succeeds

Advanced Rocketry never decides *if* a station may exist — only *how* it functions.

---

## Country Data Model

Each country stores the following persistent values:

| Field           | Description                        |
| --------------- | ---------------------------------- |
| `stationCap`    | Maximum number of stations allowed |
| `stationsBuilt` | Number of stations currently owned |

### Logic

```text
stationsBuilt < stationCap → creation allowed
stationsBuilt ≥ stationCap → creation denied
```

---

## Advanced Rocketry Integration

### Intercepted Action

* Space station creation (server-side)

### Enforcement

When a player attempts to create a space station:

1. Resolve player → country
2. Check country station cap
3. Cancel creation if cap exceeded
4. Send player a failure message

### Failure Message Example

```
Your country is not authorized to create a space station.
Complete the Orbital Authorization quest to unlock one.
```

---

## Advanced Rocketry Configuration

The following settings are recommended to avoid conflicts:

```cfg
# Advanced Rocketry
B:restrictProgression=false
B:useWhitelist=false
```

> Station limits are enforced entirely by BaldeagleCore.

If the config option exists:

```cfg
B:allowPlayerStations=false
```

Otherwise, BaldeagleCore vetoes station creation at runtime.

---

## Station Cap Progression (FTB Quests)

### Quest Structure

Two quests control orbital access:

| Quest Name               | Reward          | Cost     |
| ------------------------ | --------------- | -------- |
| Orbital Authorization I  | Station cap → 1 | T3 Cores |
| Orbital Authorization II | Station cap → 2 | T3 Cores |

---

## How FTB Quests Apply Upgrades

FTB Quests trigger **server commands** provided by BaldeagleCore.

### Command Interface

```text
/country station upgrade 1
/country station upgrade 2
```

### Command Rules

* Must be executed by a country leader (or authorized role)
* Cannot skip tiers
* Cannot downgrade
* Server-side only

---

## Example Quest Reward Configuration

**Reward Type:** Command

```
country station upgrade 1
```

Repeat for tier 2:

```
country station upgrade 2
```

---

## Security & Exploit Prevention

### Prevented Exploits

* Player-owned stations
* Infinite station creation
* Dimension bypass via RFTools
* Client-side spoofing
* Station duplication

### Enforcement Points

* Station creation
* Station access
* Station ownership resolution
* Server startup validation

---

## Server Startup Validation

On server load:

* All stations are scanned
* Stations are mapped to countries
* If a country exceeds its cap:

  * Creation is blocked
  * No stations are deleted automatically
  * Violations are logged for admins

---

## Player Experience Flow

1. Player builds rocket
2. Attempts to create space station → **denied**
3. Player completes Orbital Authorization quest
4. Country station cap increases
5. Station creation now succeeds
6. Country owns the station permanently

---

## Why This System Exists

* Prevents dimension inflation
* Encourages cooperation
* Makes orbital access meaningful
* Supports geopolitical gameplay
* Scales cleanly to:

  * Orbital weapons
  * Station taxation
  * Space warfare
  * Treaties & alliances

---

## Compatibility Notes

| Mod                               | Status          |
| --------------------------------- | --------------- |
| Advanced Rocketry (dercodeKoenig) | Fully supported |
| FTB Quests                        | Required        |
| CraftTweaker                      | Optional        |
| KubeJS                            | Optional        |
| RFTools Dimensions                | Disabled        |
| FTB Utilities claiming            | Disabled        |

---

## Admin Notes

* Station caps should **never** be edited directly in save files
* Always use quests or commands
* Logs are written for all denied creation attempts
* This system assumes **Earth is the overworld**

---

## Summary

This integration:

* Makes Advanced Rocketry a **transport & dimension engine**
* Makes BaldeagleCore the **authority**
* Ties orbital expansion to **economic progression**
* Eliminates player abuse vectors
* Enables large-scale, long-term server play
