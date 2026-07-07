# RetroAPI changelog

## 0.2.3 — Sharper tools & tags

All items below are exercised by the test mod's headless self-checks (`runPopulateServer`),
logged as `[new-features] ... PASS`.

### Tools & harvesting
- **Custom tools now work on vanilla blocks.** RetroAPI ships beta-accurate default
  `mineable/{pickaxe,axe,shovel}` and `needs_{stone,iron,diamond}_tool` membership for vanilla
  blocks (from beta's own `MinecraftPickaxe`/`Axe`/`Shovel` tables and harvest levels). A
  `.tool(PICKAXE).tier(IRON)` plain item now harvests vanilla iron ore at iron speed, not just
  modded ores. Registered lazily on first tag query (`RetroTags.ensureVanillaDefaults`) so it
  can't lose a race with a consumer mod's init order. Skipped when StationAPI is present.
- **Decoupled semantics (matches modern MC).** A `mineable/<tool>` tag grants SPEED only;
  "requires a tool to drop" comes from the block material (`Material.isHandHarvestable()`) or a
  `needs_<tier>_tool` tag. *Behavior change:* `.mineable(...)` alone no longer gates drops on a
  hand-breakable material — use a stone/metal material or a `needs_<tier>_tool` tag for that.
- **Multi-kind tools:** `.tool(RetroTool... )` (a pickaxe+axe paxel). `RetroTool.kindsOf(item)`.
- **Dynamic tier:** `.tier(stack -> RetroToolTier)`, consulted per harvest. `RetroToolTier.of(ItemStack)`.
- Declared plain-`Item` tools mine at their **tier's** speed (wood 2× … diamond 8×), not a flat boost.
- Added `RetroTool.SHEARS` (parity with StationAPI's tool set).

### Tags
- **Item tags** and **arbitrary runtime tags** for both items and blocks:
  `RetroTagKey.item(...)`, `RetroTags.addToTag(tag, Item...)`, `isIn(Item, tag)`, `itemsIn(tag)`,
  `removeFromTag(...)`. RetroAPI's code tags are live/mutable (StationAPI's are frozen).
- Item tag data files load from `tags/item(s)` and StationAPI's `stationapi/tags/items` layouts;
  vanilla item names resolve via the new `VanillaItemNames` flattening map.

### Items & rendering
- **Layered sprites with no model JSON:** `.layers(base, overlays...)` and `.overlay(id)`
  (base/overlays may be vanilla or modded).

### Blocks
- **`.facing()`** — built-in `RetroFacing` property + furnace-like orient-to-placer on placement,
  no custom enum or `onPlaced`. (Full code-generated `.sided(top, side, front)` is the next step.)

### Ergonomics
- **Interface injection:** `Block`/`Item` implement the RetroAPI access interfaces for consuming
  mods, so no `(RetroBlockAccess)`/`(RetroItemAccess)` cast is needed.
- **Constructor factories:** `RetroBlockAccess.of(Ctor::new)`, `of(Ctor::new, Material)`,
  `RetroItemAccess.of(Ctor::new)`.
- Auto-generated display names are now **named** in the log, a visible reminder to add a lang entry.

### StationAPI compatibility
- Reads StationAPI's exact tag layouts and matches its vanilla flattening names; clean hand-off of
  vanilla harvesting when StationAPI is present. See `STATIONAPI_TAG_COMPAT.md`.
