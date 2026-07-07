# RetroAPI ↔ StationAPI tag compatibility

Investigation of StationAPI's tag system (source cloned from
`github.com/ModificationStation/StationAPI`, `2.0.0-alpha.6.2+gen2` era) and the resulting
compatibility decisions baked into RetroAPI's tag/tool engine. The goal: a StationAPI mod's tag
files and a RetroAPI mod's tag files should be **the same files**, and when both APIs are present
the responsibilities are split cleanly rather than fought over.

## What StationAPI does

| Concern | StationAPI | RetroAPI (now) |
|---|---|---|
| Tag file layout | `data/<ns>/stationapi/tags/blocks/<p>.json`, `.../tags/items/<p>.json` (**plural**, `stationapi/` segment) | scans that layout **and** modern `tags/block(s)` / `tags/item(s)` |
| JSON schema | `{ "values": [...], "replace": false }`; entries `"ns:id"`, `"#ns:tag"`, `{"id":...,"required":false}` | identical |
| Bare id namespace | defaults to `minecraft:` | identical |
| Block + item tags | both (separate registries) | both |
| Tool kinds | `mineable/{axe,hoe,pickaxe,shears,shovel,sword}` | same set (`RetroTool`, incl. `SHEARS`) |
| Tool tiers | `needs_{stone,iron,diamond}_tool` (+ `needs_tool_level_<i>`) | `needs_{stone,iron,diamond}_tool` (`RetroToolTier`) |
| Vanilla harvest defaults | ships them as data tags in `station-vanilla-fix-v0` | ships them in code (`VanillaToolTags`), matching beta harvest levels |
| Vanilla-name resolution | flattening map in `VanillaBlockFixImpl` (`oak_planks`→PLANKS, `carved_pumpkin`→PUMPKIN, …) | `VanillaBlockNames` / `VanillaItemNames`, matching those spellings |
| Add-to-tag at runtime | **not supported** (tags frozen from resources) | supported (`RetroTags.addToTag(...)`, live immediately) |
| "custom logic skipped when block+tool both `minecraft:`" | yes (`ToolEffectivenessImpl.shouldApplyCustomLogic`) | equivalent: RetroAPI does not ship vanilla defaults when StationAPI is present |

## The compatibility decisions

1. **Same tag files, no data changes.** `RetroTagLoader` scans StationAPI's exact
   `data/<ns>/stationapi/tags/blocks|items/**.json` layout in addition to the modern
   `tags/block(s)`/`tags/item(s)` layouts. A mod written for StationAPI drops into RetroAPI
   unchanged, and vice-versa. Tags are namespace-blind (path only), so `minecraft:` vanilla files
   and mod files union with zero ceremony.

2. **Matching vanilla names.** `VanillaBlockNames`/`VanillaItemNames` mirror StationAPI's
   flattening spellings (including both `oak_planks` and `planks`, `cod`/`raw_fish`,
   `golden_pickaxe`/`gold_pickaxe`, …) so the same `minecraft:`-namespaced entries resolve on
   both.

3. **Clean hand-off when StationAPI is present.** StationAPI already ships the vanilla
   `mineable`/`needs_*_tool` data tags and owns vanilla harvesting (its `StationTool` +
   `ToolEffectivenessImpl`). So RetroAPI **does not register its default vanilla tool tags** when
   `stationapi` is loaded (`RetroTags.ensureVanillaDefaults` is a no-op there). RetroAPI's harvest
   hooks still fire, but only for blocks that are in a RetroAPI tag — i.e. modded blocks RetroAPI
   itself tagged — so the two systems act on disjoint block sets and never fight.

4. **RetroAPI-unique things stay unique.** Runtime `addToTag` (item **and** block), the dynamic
   per-stack tool tier, multi-kind tools, and plain-`Item` tools have no StationAPI equivalent;
   they keep working whether or not StationAPI is present, because they ride RetroAPI's own hooks.

## Decoupled harvest semantics (matches modern Minecraft)

A `mineable/<tool>` tag grants **speed only**. Whether a block needs a tool to **drop** comes from
its material (`Material.isHandHarvestable()` — beta's own stone/metal rule) or from a
`needs_<tier>_tool` tag. This is the modern split, and it is why RetroAPI can now ship vanilla
`mineable/axe`/`mineable/shovel` membership for hand-harvestable wood/dirt blocks (so custom axes
and shovels get their speed) without ever wrongly gating those blocks' hand-harvest.

## Not yet mirrored (candidates)

- The `c:` convention-tag tree (~280 files: `c:ores`, `c:logs`, `c:tools/pickaxes`, …). RetroAPI
  resolves them if a mod ships them, but doesn't ship the tree itself. Worth vendoring if
  cross-mod recipe/interop conventions become a goal.
- `needs_tool_level_<i>` custom tiers beyond DIAMOND. `RetroToolTier` stops at DIAMOND (beta's
  ceiling); a numeric/graph tier system could be added later if a mod needs supra-diamond tools.
