# Chunk Renderer & Territory Overlay System

## Overview

The Chunk Renderer overlays **territorial ownership information** on top of the terrain snapshot displayed in the Chunk Map GUI.

It visually distinguishes:

* Chunks owned by the player’s country
* Chunks owned by allied countries
* Chunks owned by hostile countries
* Unclaimed chunks

Access to the Chunk Map is provided through an **icon button in the player’s inventory**, similar to FTB Teams.

---

## Inventory Integration

### Inventory Button

* When a player opens their inventory, a **territory icon** is displayed on the **left side** of the screen.
* Clicking this icon opens the **Chunk Map GUI**.
* The button is client-side only and does not alter inventory behavior.

### Visibility Rules

The inventory icon:

* Is always visible in survival mode
* May be disabled or show a warning if the player is not in a country
* Does not require permissions to view the map

---

## Chunk Map Responsibilities

The Chunk Map GUI is responsible for:

* Rendering terrain snapshots
* Rendering chunk ownership overlays
* Rendering relationship-based coloring
* Displaying player position
* Providing contextual information via tooltips

The Chunk Map **does not modify data**; it is strictly visual.

---

## Ownership & Relationship States

Each chunk rendered on the map is classified into one of the following states:

| State   | Description                                 |
| ------- | ------------------------------------------- |
| Owned   | Claimed by the player’s country             |
| Allied  | Claimed by a country allied with the player |
| Hostile | Claimed by a non-allied country             |
| Neutral | Unclaimed                                   |

Relationship state is determined **server-side** and sent to the client as part of chunk metadata.

---

## Visual Representation

### Base Terrain Layer

* Rendered first
* Uses the terrain snapshot system
* Provides geographical context

---

### Ownership Overlay

Ownership is rendered as an **overlay layer** on top of terrain.

#### Color Scheme

| Chunk Type       | Visual Style             |
| ---------------- | ------------------------ |
| Player’s country | Green                    |
| Allied country   | Blue                     |
| Other countries  | Orange                   |
| Neutral          | No overlay / Gray border |

Colors may be tinted or translucent to preserve terrain visibility.

---

### Borders

* Each chunk is outlined with a border
* Border color matches ownership state
* Borders help clearly define chunk boundaries

---

### Player Marker

* The player’s current chunk is highlighted
* Player position is shown at the center of the map
* The marker always renders above overlays

---

## Tooltip Information

When hovering over a chunk, the following information may be displayed:

```
Chunk: (X, Z)
Owner: Country Name
Relation: Own / Ally / Neutral
Income: +X per day
```

Tooltips are informational only and do not allow interaction.

---

## Data Flow

### Server-Side

The server provides the client with chunk metadata:

* Chunk coordinates
* Owning country ID (or null)
* Relationship state relative to the viewing player

The server remains authoritative for:

* Ownership
* Alliances
* War status

---

### Client-Side

The client:

* Requests chunk data when the GUI opens
* Caches received data
* Renders terrain and overlays
* Refreshes data when necessary

The client does **not** infer ownership or relationships locally.

---

## Update Triggers

The Chunk Renderer should refresh when:

* The Chunk Map GUI is opened
* The player moves into a new chunk range
* Chunk ownership changes
* An alliance is formed or removed
* A chunk becomes unclaimed or captured

Partial updates are preferred over full refreshes.

---

## Rendering Order

To ensure clarity, rendering must follow this order:

1. Terrain snapshot
2. Height shading / biome tint (if enabled)
3. Ownership overlay (fill)
4. Ownership border
5. Player marker
6. Tooltips

This guarantees ownership information is readable without obscuring terrain.

---

## Performance Constraints

* Only chunks within the visible map radius are rendered
* Chunk data is cached client-side
* No real-time world rendering is performed
* Ownership overlays must not trigger world updates

---

## Design Goals

* Immediate visual clarity of territorial control
* Clear distinction between allies and enemies
* Zero dependency on external mods
* Minimal client and server overhead
* Consistent interaction model across systems

---

## Future Extensions (Not Required)

This system is designed to support:

* War-state indicators (flashing borders)
* Capital chunk markers
* Alliance heatmaps
* Strategic overlays (income, defense, upgrades)
