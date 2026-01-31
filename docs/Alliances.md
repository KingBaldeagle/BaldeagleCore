# Country Alliance System

## Overview

The Alliance system allows countries to form **mutual diplomatic relationships** that grant **shared access to claimed territory**.

Alliances are:

* **Explicit** (must be requested and approved)
* **Mutual** (both countries must agree)
* **President-controlled**
* **Immediately enforced** in chunk protection logic

This system is independent of any GUI or map features.

---

## Core Rules

* Only **country presidents** may manage alliances
* Alliances must be **requested and accepted**
* Allied countries may **build and interact** in each other’s claimed chunks
* Alliances can be **removed at any time**
* Players with **no country** never gain access through alliances

---

## Data Model

Each country maintains:

```java
Country {
    String id;
    UUID president;
    Set<String> allies;
    Set<String> incomingAllianceRequests;
}
```

### Invariants

* Alliances are **bidirectional**
* If Country A is allied with Country B, Country B must be allied with Country A
* Incoming requests are one-directional until accepted

---

## Alliance Request Flow

### 1. Sending a Request

**Command**

```
/country ally request <country>
```

**Requirements**

* Sender is president of their country
* Target country exists
* Countries are not already allied
* No pending request already exists

**Effect**

* Adds a pending alliance request to the target country
* Notifies the target country’s president

---

### 2. Accepting a Request

**Command**

```
/country ally accept <country>
```

**Requirements**

* Sender is president of the sending country
* Incoming request exists from the specified country

**Effect**

* Both countries add each other to their ally lists
* Pending request is removed
* Alliance becomes active immediately

---

### 3. Denying a Request

**Command**

```
/country ally deny <country>
```

**Effect**

* Removes the pending request
* No relationship is formed

---

### 4. Removing an Alliance

**Command**

```
/country ally remove <country>
```

**Requirements**

* Sender is president
* Countries are currently allied

**Effect**

* Alliance is removed from both countries
* Chunk access permissions update immediately

---

## Chunk Protection & Permissions

### Claimed Chunk Access Rules

| Player Type    | Can Place Blocks | Can Break Blocks   |
| -------------- | ---------------- | ------------------ |
| Owner country  | ✅ Yes            | ✅ Yes              |
| Allied country | ✅ Yes            | ✅ Yes              |
| Enemy country  | ❌ No             | ❌ No (except flag) |
| No country     | ❌ No             | ❌ No               |

### Flag Block Exception

* **Only enemy countries** may destroy the Claim Flag Block
* Allied countries may **not** attack flag blocks
* This prevents alliance abuse during war

---

## War Interaction

* Allied countries **cannot attack each other**
* Flag blocks in allied territory are fully protected
* Removing an alliance:

  * Does **not** automatically declare war
  * Immediately revokes build access

---

## Player Without Country

Players who are not members of any country:

* Cannot build in claimed chunks
* Cannot benefit from alliances
* Are always treated as outsiders

---

## Persistence

All alliance data must be saved and restored on server restart.

Example storage:

```json
{
  "id": "Germany",
  "allies": ["France", "Italy"],
  "incomingAllianceRequests": []
}
```

Validation on load:

* Remove invalid country references
* Enforce bidirectional alliances

---

## Edge Case Handling

* Country deletion removes all alliances
* President transfer preserves alliances
* Self-alliances are disallowed
* Duplicate or crossed requests are prevented

---

## Design Goals

* Simple permission model
* Clear authority boundaries
* Predictable diplomacy
* No external dependencies
* Easily extendable to future systems (war, trade, treaties)
