# 💰 Minecraft Mod Currency System Revamp – `long` Implementation

## Overview

This document describes the **revamped currency system** for the mod, designed to handle **late-game economies** where countries or players may have billions or even trillions of dollars. The system replaces 32-bit integers with **64-bit signed `long` values** to prevent overflow and maintain precision.

---

## Data Type Change

| Old Type       | New Type        | Reason                                                                                                                                                            |
| -------------- | --------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `int` (32-bit) | `long` (64-bit) | 32-bit `int` maxes out at ~2.1 billion, which is insufficient for late-game balances. `long` maxes out at ~9 quintillion, comfortably supporting large economies. |

### Benefits

* Supports very large balances without overflow.
* Simple arithmetic operations (`+`, `-`) remain valid.
* Compatible with Minecraft’s Java environment (no external dependencies).

---

## Class Structure Example

```java
public class Country {
    private long balance;

    public Country(long initialBalance) {
        this.balance = initialBalance;
    }

    // Add money to the balance
    public void addMoney(long amount) {
        balance += amount;
    }

    // Subtract money from the balance
    public void subtractMoney(long amount) {
        balance -= amount;
    }

    // Get the current balance
    public long getBalance() {
        return balance;
    }

    // Set a new balance
    public void setBalance(long newBalance) {
        balance = newBalance;
    }
}
```

---

## Guidelines for Arithmetic

1. **Addition/Subtraction**

   ```java
   country.addMoney(5000000000L); // 5 billion
   country.subtractMoney(2500000000L); // 2.5 billion
   ```

   ✅ Always use `L` suffix for literal longs to avoid unintentional int overflow.

2. **Multiplication/Division**

   ```java
   long tax = country.getBalance() / 10; // 10% tax
   ```

   ⚠️ Be cautious with large multiplications; overflow is still theoretically possible if values exceed `Long.MAX_VALUE`.

---

## GUI & Item Storage

* Balances stored in items or GUI displays should also use `long`.
* When saving to NBT:

```java
compound.setLong("Balance", country.getBalance());
```

* When reading from NBT:

```java
long balance = compound.getLong("Balance");
```

* No data loss occurs compared to `int`.

---

## Migration Notes

If you are **upgrading from the previous `int` system**:

1. Convert all existing `int` balances to `long`:

```java
long newBalance = (long) oldBalance;
```

2. Update all methods and references to use `long` instead of `int`.
3. Update configuration files, JSONs, or NBTs storing currency to support long values.

---

## Future Considerations

* If the mod ever allows **balances beyond 9 quintillion**, consider switching to `BigInteger`.
* Always ensure GUI components can format and display large numbers correctly (e.g., using commas or abbreviated notation like “B” for billion).

---

✅ **Conclusion:**
Switching to `long` eliminates the 32-bit integer limitation, ensuring **stable, high-capacity economic simulations** for late-game Minecraft mod scenarios.
