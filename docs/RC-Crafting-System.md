# Research Core Crafting System (Documentation)

## Overview

This system introduces **Research Cores** as a non-reversible, value-normalized progression resource. Instead of crafting cores directly in a vanilla crafting table, players must use a dedicated block (the **Research Assembler**) which consumes **country-specific currency**.

Key goals:

* Support multiple countries with different currency values
* Prevent currency arbitrage and reconversion exploits
* Create a clean progression ladder (T1 → T2 → T3, etc.)
* Centralize all balancing logic server-side

Once currency is converted into a Research Core, **it can never be converted back into money**.

---

## Concepts

### Country Currency

Each country issues its own currency item. Currencies differ in **exchange value**, but share the same internal interface.

Example:

* USD Bill = value 100
* EUR Bill = value 120
* RUB Bill = value 30

All values are normalized internally.

---

### Research Cores

Research Cores represent *locked economic value* used for:

* Technology unlocks
* High-tier crafting
* Nation progression

| Tier    | Internal Value | Notes                     |
| ------- | -------------- | ------------------------- |
| T1 Core | 900            | Entry-level research unit |
| T2 Core | 8100           | 9 × T1                    |
| T3 Core | 72,900         | 9 × T2                    |

Cores:

* Are items
* Stack normally
* Cannot be dismantled
* Cannot be traded for currency

---

## Research Assembler Block

### Purpose

The Research Assembler is the **only block capable of creating Research Cores**.

It performs three critical functions:

1. Currency value aggregation
2. Country ownership validation
3. One-way conversion enforcement

---

### UI Layout

Suggested GUI layout:

* Currency input slot(s)
* Core output slot
* Country indicator
* Progress bar
* Error/status text

---

## Crafting Rules

### Rule 1: Currency Must Match Country

Only currency belonging to the *same country as the block owner* is accepted.

Invalid currency:

* Is rejected immediately
* Does not consume items
* Shows an error message

---

### Rule 2: Value Thresholds

#### T1 Core

* Requires **900 normalized value**
* Example combinations:

  * 9 × 100-value bills
  * 3 × 300-value notes

#### T2 Core

* Requires **9 T1 Cores**

This rule ensures:

* No direct money → T2 skipping
* Predictable scaling

---

### Rule 3: One-Way Conversion

Once converted:

* Currency is destroyed
* Cores cannot be reverse-crafted
* No recipe exists to break cores down

This prevents:

* Inflation loops
* Currency laundering
* Cross-country exploits

---

## Recipes (Vanilla Disabled)

Vanilla crafting table recipes are **intentionally NOT used** for Research Cores.

Reasons:

* Cannot validate country ownership
* Cannot normalize currency value
* Cannot prevent exploits

All crafting logic lives inside the Research Assembler TileEntity.

---

## Internal Logic Flow

1. Player inserts currency
2. Block validates:

   * Player country
   * Currency country
3. Block sums currency value
4. If value ≥ required tier:

   * Currency is consumed
   * Core is produced
5. Excess value is discarded (intentional sink)

---

## Anti-Exploit Design

| Exploit            | Mitigation                |
| ------------------ | ------------------------- |
| Currency arbitrage | Country-locked validation |
| Duplication        | Server-side crafting only |
| Reverse conversion | No dismantle recipes      |
| Value hoarding     | Excess value sink         |

---

## Balance Notes

* Excess value loss is intentional
* Higher tiers exponentially increase effort
* Encourages cooperation and planning

Recommended tuning knobs:

* Core value thresholds
* Currency values
* Assembly time
* Power requirements (optional)

---

## Extension Points

Future upgrades may include:

* Power consumption (RF/FE)
* Automation via OpenComputers
* Country-wide research pools
* Core decay or upkeep

---

## Summary

The Research Core system acts as:

* A **currency sink**
* A **progression gate**
* A **country-scaled balancing mechanism**

By enforcing one-way conversion through a dedicated block, the system ensures long-term economic stability while still supporting diverse country currencies.
