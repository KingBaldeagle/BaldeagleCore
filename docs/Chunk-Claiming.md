# Chunk Claiming & Protection System

This system allows countries to **physically claim chunks**, earn money from them, and **protect territory**, while still enabling **war-based capture** through a special flag block.

---

## Overview

* Chunks can be claimed by countries using a **Claim Flag Block**.
* Each claimed chunk:

  * Belongs to exactly **one country**
  * Generates **economic income**
  * Is **protected** from non-owners
* Territory can be captured during war by **destroying the flag block**.
* Players **not part of any country** are treated as outsiders and have no build permissions in claimed chunks.

---

## Chunk Ownership Model

Each claimed chunk is stored as:

```
Chunk (x, z) → Country ID
```

Ownership is persisted across server restarts.

A chunk is considered **claimed** if:

* It has a registered owner country
* A valid flag block exists within the chunk

---

## Claiming a Chunk

### Requirements

* Player must be a member of a country
* Chunk must be unclaimed
* Player must place a **Claim Flag Block**

### Behavior

* When the flag is placed:

  * The chunk becomes owned by the player’s country
  * The chunk is added to the country’s territory count
  * Economy rewards update automatically

### Restrictions

* Only **one claim flag per chunk**
* Flags cannot be placed in already-claimed chunks

---

## Block Protection Rules

### Claimed Chunks

| Player Type                | Can Place Blocks | Can Break Blocks   |
| -------------------------- | ---------------- | ------------------ |
| Owner country member       | ✅ Yes            | ✅ Yes              |
| Other country member       | ❌ No             | ❌ No (except flag) |
| Player with **no country** | ❌ No             | ❌ No               |

**Exception:**

* Non-owners **may break the Claim Flag Block** during war.

---

## Players Without a Country

Players who are **not part of any country**:

* **Cannot place blocks** in claimed chunks
* **Cannot destroy blocks** in claimed chunks
* **Cannot place or destroy claim flags**
* Are treated the same as enemy players for protection checks

This ensures:

* Territory is meaningful
* Random players cannot grief claimed land
* Countries control their borders

---

## War & Chunk Capture

### Capturing a Chunk

1. Attacking player enters an enemy-owned chunk
2. Player destroys the **Claim Flag Block**
3. Chunk becomes **unclaimed**
4. Attacking country may place their own flag to claim it

### Important Notes

* Destroying the flag **does not auto-claim** the chunk
* This prevents instant flipping and allows counterplay
* Protection is lifted once the chunk becomes unclaimed

---

## Flag Block Rules

The Claim Flag Block:

* Represents ownership of the chunk
* Can only be placed by country members
* Can only be broken by:

  * The owning country
  * Enemy countries (during war)
* Is the **only block** outsiders can destroy in a claimed chunk

---

## Economy Integration

* Each claimed chunk generates **base income**
* Income increases by **5% per additional chunk**
* Rewards are calculated daily and added to:

  * The owning country’s balance

If a chunk is lost:

* The country immediately loses:

  * That chunk’s income
  * Its compounding effect

---

## Design Goals

This system is designed to:

* Avoid external dependencies (FTB, etc.)
* Make land ownership **physical and visible**
* Enable wars without full griefing
* Tie territory directly into the economy
* Keep permissions simple and predictable

---

## Future Extensions (Optional)

* Chunk upgrades (increase income or defense)
* Notifications when flags are under attack
* Fortification blocks inside claimed chunks
* Alliances so allied countries can place/destroy blocks in allied borders.
