# Research Assembler – Currency-to-Core System

## Overview

The **Research Assembler** is a dedicated machine block used to convert **country-issued currency** into **research cores**. This system is intentionally **one‑way**: once currency is converted into a research core, it can **never** be converted back into money.

This design creates a permanent **economic sink**, prevents currency arbitrage between countries, and allows research progression to scale naturally with a country’s economic strength.

---

## Design Goals

* Provide a fair way to use **multiple national currencies** with different values
* Prevent **infinite crafting loops** and currency duplication
* Centralize research progression in a **machine**, not recipes
* Allow server owners to rebalance research costs without touching recipes
* Integrate cleanly with countries, inflation, and exchange rates

---

## High-Level Behavior

1. Players insert **currency items** (coins or bills) into the Research Assembler
2. The machine reads:

   * Denomination value
   * Issuing country
   * Exchange rate
   * Inflation modifier
3. Currency is converted into a **normalized internal value**
4. Once enough value is accumulated, the machine outputs a **research core**
5. Inserted currency is **destroyed permanently**
6. Research cores **cannot** be converted back into currency

---

## One-Way Conversion Rule (Critical)

> **Currency → Research Core is irreversible**

This rule is strictly enforced by design:

* ❌ No crafting recipes convert cores back into money
* ❌ No machine disassembles cores
* ❌ No refunds when the block is broken
* ❌ No NBT value exposed on cores

Research cores are treated as **abstract research artifacts**, not stored value.

---

## Normalized Value System

Because different countries have different currencies, the assembler uses a **global base unit**:

### Research Credits (RC)

All currencies are converted into **Research Credits (RC)** before being stored.

### Conversion Formula

```
RC = denominationValue
     × countryExchangeRate
     × inflationModifier
```

This ensures:

* Strong economies generate research faster
* Inflated currencies lose effectiveness
* All currencies remain usable

---

## Example Conversion

| Currency  | Issuing Country | Effective RC |
| --------- | --------------- | ------------ |
| $100 Bill | Stable Economy  | 10,000 RC    |
| $100 Bill | High Inflation  | 6,500 RC     |
| $100 Bill | Weak Economy    | 4,000 RC     |

---

## Research Core Costs (Example)

| Core Tier | Cost (Research Credits) |
| --------- | ----------------------- |
| T1 Core   | 10,000 RC               |
| T2 Core   | 90,000 RC               |
| T3 Core   | 800,000 RC              |

These values are **configurable** and do not rely on crafting recipes.

---

## Block Responsibilities

The Research Assembler:

* Maintains an internal RC buffer
* Accepts currency items only
* Rejects non-currency items
* Produces research cores when thresholds are met
* Prevents output overflow
* Syncs state server → client

---

## Tile Entity Data

Stored server-side only:

* `storedResearchCredits (long)`
* Input inventory
* Output inventory

No stored value is exposed to items or players directly.

---

## GUI Behavior

The GUI should display:

* Inserted currency
* Converted Research Credits
* Progress bar toward next core
* Selected output tier (T1 / T2 / etc.)
* Issuing country and exchange modifier

Clear feedback is important to maintain player trust.

---

## Why This Is NOT a Recipe System

Crafting recipes in 1.12.2:

* Cannot read NBT
* Cannot read country ownership
* Cannot apply exchange rates
* Cannot handle inflation
* Are easily exploited via JEI

A **TileEntity machine** is required for correctness and balance.

---

## Anti-Exploit Safeguards

The Research Assembler enforces:

* No output if output slot is full
* No partial refunds
* No reverse conversions
* Optional: country membership checks
* Optional: claimed‑land‑only placement

These rules prevent abuse in multiplayer environments.

---

## Integration Notes

This system is designed to integrate with:

* Country ownership systems
* Inflation tracking
* Exchange rate logic
* Claimed chunk rules
* Tech progression gating

It intentionally avoids hard dependencies on recipe mods.

---

## Future Extensions (Optional)

* Country research tax
* Research speed modifiers
* Embargoes or sanctions
* Government-only assemblers
* Tier‑locked machines

---

## Summary

The Research Assembler provides a:

* Fair
* Scalable
* Exploit‑resistant
* Economy‑aware

solution for converting national currency into irreversible research progress.

This block is the **only valid entry point** for turning money into research cores.
