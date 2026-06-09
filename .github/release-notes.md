# RetroAPI 0.2.0

RetroAPI is a content-registration library for **Minecraft Beta 1.7.3** that lets mods add blocks,
items, recipes, entities, dimensions and more **without requiring StationAPI**. It runs on **Ornithe**
(Fabric Loader + OSL) and **Babric**, and transparently delegates to **StationAPI** when that mod is
also installed.

## What it implements

**Registration**
- Custom blocks and items with automatic, stable ID assignment - block IDs are expanded well past the
  vanilla 256 cap, and assignments persist across loads.
- Block entities without subclassing `BlockWithBlockEntity` (`RetroBlockEntityType`).
- Inventory / menu GUIs (chest, furnace and dispenser styles) with automatic slot sync (`@SyncField`).
- Crafting, smelting and furnace-fuel recipes (`RetroRecipes`).
- Custom entities with renderers, and custom dimensions reached through walk-in portal blocks.
- Achievements and achievement pages.
- Sound autoloading and automatic lang / translation generation.

**Rendering**
- Expanded texture atlas plus custom block and item renderers and render types.

**Vanilla-safe world storage (sidecar system)**
- All modded content is written to `retroapi/` sidecar files keyed by string identifiers, so a world
  can be opened in **vanilla** without crashing or losing data and reopened in RetroAPI intact.
- Covers extended block IDs, modded block entities, modded items inside vanilla containers, and
  dropped item entities.

**Multiplayer**
- OSL-based networking with server-to-client ID mapping sync and extended-block chunk sync.

**StationAPI compatibility (optional, bundled)**
- Ships a `retroapi-stationapi` companion that loads **only** when StationAPI is also present and
  delegates registration, rendering, dimensions and world conversion to StationAPI. With it, RetroAPI
  worlds round-trip through StationAPI's flattened world format without data loss.

## Downloads

- **Ornithe** - `retroapi-0.2.0.jar` (requires OSL).
- **Babric** - `retroapi-0.2.0-babric.jar` (self-contained: OSL is bundled in).

Both jars include the StationAPI compatibility layer; it activates automatically if StationAPI is
installed and stays dormant otherwise.
