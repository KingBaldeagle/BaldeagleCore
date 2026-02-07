# Developer Implementation Details

### Classes, Hooks, and Methods Used

This section documents the **actual code-level integration** between **BaldeagleCore** and **Advanced Rocketry (dercodeKoenig fork)**.
It is intended for developers and maintainers.

---

## Integration Architecture

Advanced Rocketry integration is **fully optional** and isolated to a dedicated package to prevent classloading crashes when AR is not installed.

### Package Layout

```text
com.baldeagle
 ├─ BaldeagleCore.java
 ├─ country
 │   ├─ Country.java
 │   ├─ CountryStorage.java
 │   └─ CountryManager.java
 └─ integration
     └─ ar
         ├─ AdvancedRocketryIntegration.java
         └─ ARStationEventHandler.java
```

**No Advanced Rocketry classes are referenced outside `integration.ar`.**

---

## Mod Initialization Flow

### Entry Point

**Class:** `com.baldeagle.BaldeagleCore`
**Method:** `init(FMLInitializationEvent event)`

```java
if (Loader.isModLoaded("advancedrocketry")) {
    AdvancedRocketryIntegration.init();
}
```

This ensures:

* AR integration only loads if the mod is present
* BaldeagleCore can run without Advanced Rocketry installed

---

## AdvancedRocketryIntegration

### Class

`com.baldeagle.integration.ar.AdvancedRocketryIntegration`

### Responsibility

* Registers all Advanced Rocketry–specific event handlers
* Acts as the single entry point for AR integration

### Key Method

```java
public static void init()
```

### Behavior

* Registers `ARStationEventHandler` on the Forge event bus
* Performs no logic itself

---

## Space Station Creation Interception

### Handler Class

`com.baldeagle.integration.ar.ARStationEventHandler`

### Responsibility

* Intercepts space station creation
* Enforces country station limits
* Cancels unauthorized station creation attempts

---

### Primary Intercept Point

**Event:** `ARStationCreateEvent`
*(Event name may differ slightly depending on AR fork; see notes below)*

**Method:**

```java
@SubscribeEvent
public void onStationCreate(ARStationCreateEvent event)
```

---

### Enforcement Logic (Step-by-Step)

1. Retrieve the player creating the station:

   ```java
   EntityPlayerMP player = event.getPlayer();
   ```

2. Resolve the player’s country:

   ```java
   Country country = CountryManager.getCountry(player);
   ```

3. Validate station capacity:

   ```java
   if (country == null || !country.canCreateStation()) {
       event.setCanceled(true);
   }
   ```

4. Notify the player:

   ```java
   player.sendMessage(new TextComponentString(
       "Your country is not authorized to create a space station."
   ));
   ```

5. Allow creation if validation passes

---

### Station Count Increment

Once station creation is confirmed successful, the country’s station count is incremented:

```java
country.onStationCreated();
```

This call must occur **only once**, after the station is finalized.

---

## Country Data Methods Used

### Class

`com.baldeagle.country.Country`

### Fields

```java
private int stationCap;
private int stationsBuilt;
```

---

### Methods

#### `canCreateStation()`

```java
public boolean canCreateStation()
```

Returns:

* `true` if `stationsBuilt < stationCap`
* `false` otherwise

---

#### `onStationCreated()`

```java
public void onStationCreated()
```

* Increments `stationsBuilt`
* Marks country data dirty for persistence

---

#### `setStationCap(int cap)`

```java
public void setStationCap(int cap)
```

* Sets maximum allowed stations
* Used exclusively by commands / quests

---

## FTB Quest → Country Upgrade Path

### Command Handler

**Class:** `com.baldeagle.country.CountryCommand`

### Subcommands Used

```text
/country station upgrade 1
/country station upgrade 2
```

---

### Command Logic

1. Validate executor is a country leader

2. Validate upgrade tier order

3. Apply new station cap:

   ```java
   country.setStationCap(1); // or 2
   ```

4. Persist country data

---

## Server Startup Validation

### Location

**Class:** `CountryStorage`
**Called from:** `serverLoad(FMLServerStartingEvent event)`

---

### Behavior

On server start:

1. All existing space stations are scanned
2. Stations are mapped to countries
3. If a country exceeds its cap:

   * No stations are deleted
   * Further creation is blocked
   * Violations are logged

This prevents:

* Save corruption
* Silent data loss
* Accidental admin errors

---

## Advanced Rocketry API Usage Notes

### What Is Used

* Space station creation hooks
* Space object identification
* Player context during station creation

### What Is NOT Used

* AR progression system
* AR ownership system
* AR whitelist system
* AR station limits

Advanced Rocketry is treated as a **dimension and transport provider only**.

---

## Classloading & Safety Rules

To prevent crashes:

* **Never** import AR classes in:

  * `BaldeagleCore`
  * Country logic
  * Economy logic
* **Always** guard AR integration with:

  ```java
  Loader.isModLoaded("advancedrocketry")
  ```
* Keep all AR references inside `integration.ar`

---

## Event Name Compatibility Note

Depending on the exact commit of the dercodeKoenig fork, station creation may be exposed as:

* `ARStationCreateEvent`
* A `SpaceObjectManager` hook
* A Forge event wrapping station registration

If no event exists:

* A method-level hook is used at station registration time
* Enforcement logic remains identical

---

## Extensibility Notes

This integration design supports future expansion:

* Orbital weapon authorization
* Station taxation per country
* Station-to-country treaties
* Space warfare permissions
* Per-station access control

All without modifying Advanced Rocketry.

---

## Developer Summary

* **BaldeagleCore owns authority**
* **Advanced Rocketry provides mechanics**
* **Countries gate access**
* **FTB Quests drive progression**
* **Server enforces all rules**

This separation is intentional and required for long-term stability.
