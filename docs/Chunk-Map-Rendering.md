# Chunk Map Landscape Rendering

## Overview

The Chunk Map displays a **top-down representation of the world’s terrain** to provide geographical context for claimed territory.

Rather than rendering the real world, the system uses a **lightweight terrain snapshot** per chunk. This approach ensures:

* High performance
* Server authority
* Scalability to large worlds
* Consistent visuals across clients

This system is independent of chunk ownership logic and may be layered with borders or overlays.

---

## Design Principles

* Terrain data is **sampled**, not fully rendered
* The server is the **source of truth**
* The client only renders **preprocessed snapshot data**
* Data is **cached** and updated selectively

---

## Terrain Sampling Model

Each chunk shown on the map provides a **summary of its surface terrain**.

### Required Data Per Chunk

At minimum, each chunk provides:

* Chunk X coordinate
* Chunk Z coordinate
* Surface height
* Surface block type

The surface block is defined as the **highest non-air block** at a representative position within the chunk.

---

## Sampling Strategy

### Primary Sample Point

* The **center of the chunk** is used for baseline sampling
* This ensures consistent results and minimal overhead

### Optional Multi-Sample Grid

For increased visual fidelity, a chunk may be subdivided into a small grid (e.g. 4×4):

* Each sub-cell samples one column
* Each sample includes height and top block
* The result is a low-resolution heightmap per chunk

This produces visible terrain features such as:

* Rivers
* Hills
* Coastlines

---

## Terrain Representation

### Block-Based Coloring

The terrain color of a chunk is determined by the sampled surface block.

Example mapping:

| Surface Block | Map Color |
| ------------- | --------- |
| Grass         | Green     |
| Sand          | Tan       |
| Stone         | Gray      |
| Water         | Blue      |
| Snow          | White     |

Unrecognized blocks fall back to a neutral color.

---

### Height Shading (Optional)

Surface height may be used to adjust brightness:

* Higher elevation → lighter shade
* Lower elevation → darker shade

This provides depth perception without increasing data size.

---

### Biome Tinting (Optional)

Biome information may be used to apply subtle color tinting:

| Biome Type | Tint        |
| ---------- | ----------- |
| Plains     | Light green |
| Desert     | Yellow      |
| Snowy      | White       |
| Jungle     | Dark green  |

Biome tinting is purely visual and does not affect ownership or permissions.

---

## Data Synchronization

### Server Responsibilities

* Sample terrain data
* Package terrain snapshots
* Send data only for visible chunks
* Reuse cached values where possible

### Client Responsibilities

* Cache received snapshot data
* Render terrain tiles
* Overlay ownership and interaction layers
* Discard outdated data when out of range

---

## Update Conditions

Terrain snapshot data should be refreshed when:

* The player opens the chunk map
* The player moves far enough to view new chunks
* Chunk ownership changes (optional visual refresh)
* Terrain changes significantly (optional)

Minor block changes do **not** require immediate updates.

---

## Rendering Order

Terrain visualization must be drawn in the following order:

1. Terrain base color
2. Height shading
3. Ownership overlays (borders or tint)
4. Player marker
5. Tooltips or indicators

This ensures terrain remains readable under overlays.

---

## Performance Constraints

The following rules must be enforced:

* Do not send full block data
* Do not render real world chunks
* Do not sample entire chunks
* Limit sampling to **1–64 points per chunk**
* Cache aggressively on both server and client

These constraints are mandatory to maintain server performance.

---

## Extensibility

This system is designed to support future enhancements, including:

* Animated chunk updates
* Resource overlays
* Strategic heatmaps
* Dynamic fog-of-war

All extensions must preserve the snapshot-based model.

---

## Design Goals

* Accurate terrain representation
* Minimal network traffic
* Predictable rendering cost
* Independence from external mods
* Compatibility with future UI systems
