# RetroAPI changelog

## 0.3.4 - Tinted item layers on any item

### Added
- **Statically tinted item layers: `RetroItemAccess.overlay(textureId, tint)`**, the item twin of
  `RetroBlockAccess.overlay(id, tint)`. The untinted `overlay(id)` flattens its sprite into the atlas at
  stitch time, which is why it cannot tint - by then the layers are one image. This draws as a separate
  render-time pass, so the `0xRRGGBB` multiply survives. Crucially it is *declared*, not implemented, so
  it works on any item - including a subclass the mod does not own, where `RetroLayeredTexture` cannot be
  added. An item that does implement that interface still wins, so a component-driven per-stack look
  keeps overriding the declared one. The overlay sprite goes through `getOrAddItemTexture`, so declaring
  it across twenty items costs one atlas slot, and the handle resolves its index at draw time - a texture
  named before the atlas is stitched still points at the right sprite.
- `RetroItemAccess.layer(RetroTextureLayer)` appends a fully-specified layer (a tinted base, or one built
  from a sprite index you already hold), and `getDeclaredLayers()` reads them back - the read-back for
  what `.overlay(...)`/`.layers(...)` was given.

## 0.3.3 - Closing the gaps around block state

A sweep for one shape of bug: an API that quietly does less than it looks like it does. Every entry
below is a call that compiled, ran, and silently lost data or had no way to express the thing it
documented.

### Added
- **A no-notify state setter.** `RetroStates.set(...)` always notified neighbors, which during world
  generation can cascade a block update back into the chunk still being built. There was no way to opt
  out, so anything placing a stateful block had to work around it.
  `RetroStates.setWithoutNotifyingNeighbors(...)` is the state equivalent of
  `World.setBlockWithoutNotifyingNeighbors`, and `RetroStates.placeWithoutNotifyingNeighbors(...)` does
  block + state in one call. The position is still marked dirty and a dedicated server still syncs the
  index - those are not neighbor updates, and skipping them would leave the block invisible rather than
  merely un-notified.
- **`RetroFeatures.setBlock(world, x, y, z, state)`.** The existing overload takes a 4-bit `meta`, so a
  feature simply could not place a block with more than 16 states - the index truncated to the nibble.
- **`RetroWorldGen.setStateInChunk(...)` and `RetroMultiblock.Match.fill(world, state)`** - the same
  truncation, in the two other places that place blocks. A custom chunk generator had no way at all to
  put a >16-state block into a chunk, and a multiblock could only be formed out of the low nibble.
- **`RetroToolTier.NONE`.** A `Dynamic`/`Contextual` tier could not refuse: `null` means "no opinion,
  fall through", and falling through lands on `WOOD`. `NONE` sits below every tier, so it satisfies no
  requirement - a drill bit that only bites certain ores is now
  `.tier((stack, block, player) -> isOre(block) ? DIAMOND : NONE)`.
- **`RetroTextures.getOrAddItemTexture(...)` / `getOrAddBlockTexture(...)`.** `addItemTexture` allocates
  a *new* atlas slot on every call, so two callers wanting the same sprite silently burned a slot and got
  two handles to one image. The get-or-add form is safe on either side and at any time, which is what
  code that tints someone else's sprite needs.
- **`RetroToolTier.getTagName()`**, for building `needs_<tier>_tool` ids from code.

### Testing
- The four-stage conversion pipeline actually runs all four stages. Step 4 - load the reverse-converted
  world on a plain, non-StationAPI server and prove the modded content is *runtime*-valid, not merely
  intact on disk - existed as a documented scenario, was described in the pipeline comment, and was
  wired into no task. Disk verification proves the bytes survived; this proves the world is usable,
  which is the actual claim RetroAPI makes about conversion.
- The populate stage now places a state index of 19 through the no-notify path and reads it back, so a
  setter that dropped the sidecar bits would fail the build.
- **A failing populate stage now fails the build.** It wrote its own PASS/FAIL, but `convCopyWorld`
  deliberately drops that file so the round-trip gate reads only the round-trip's verdict - so a failing
  populate sailed through silently. It is gated where it is written, before the copy.

## 0.3.2 - Shared component state, and the IDE storm

### Fixes
- **Every item shared one set of components.** `RetroComponentType.getDefault()` returned the exact
  instance passed at registration, so `get(stack, TYPE)` on any stack that had not set a value handed
  back the same object every time - and mutating it (`get(stack, LIST).add(x)`) wrote into the value
  every other stack reads, so the data appeared on every item in the game at once. Only mutable
  components could show it, which is why it presented as "all items share the same components, at least
  for list components". `List`/`Set`/`Map` defaults are now copied per read, so existing mods are fixed
  without a code change; `RetroComponents.registerSupplied(id, supplier, serializer)` covers a mutable
  default of a type RetroAPI cannot copy for you.
- **Mods no longer inherit 56 unimplemented methods.** `RetroItemAccess`/`RetroBlockAccess` declared
  their methods abstract, and 0.3.0 made interface injection actually work - so from an IDE's point of
  view every class extending `Item` or `Block` suddenly had to implement all of them. `javac` never
  complained (it does not re-verify a binary superclass), which is why it looked like an IDE-only
  hallucination and only surfaced once a class implemented some interface of its own. Every method is
  now a `default` that throws; the mixin's real implementation is a method on the class, and a class
  method always wins over an interface default, so nothing changes at runtime.
- **The client no longer tries to load dedicated-server classes.** Thirteen mixins targeting
  `net.minecraft.server.*` (and `ServerPlayerEntity`, `EntityTracker`, `ServerChunkCache`) sat in the
  mixin config's common `mixins` list, so every client launch attempted each one and logged
  `Cannot load class ... in environment type CLIENT`. b1.7.3 has no integrated server - singleplayer
  *is* the client - so none of them could ever apply there. They are in the `server` list now, and a
  client launch logs no mixin warnings at all.
- The mixin sweep fails on any common-section mixin whose targets are all absent for the running side,
  so this cannot drift back.

## 0.3.1 - Crash fixes, and a suite that catches them

0.3.0 shipped two crashes that nothing in the build could see. Both are fixed, and both now have a
test that fails loudly if they come back.

### Fixes
- **The client crashed on launch.** `WorldRendererParticleMixin` shadowed a field called `minecraft`;
  the field in `WorldRenderer` is `client`. A `@Shadow` is not checked at compile time - it is checked
  the moment the game loads the target class, and `WorldRenderer` is built during `Minecraft.init()`,
  so every client died at startup with
  `InvalidMixinException: @Shadow field minecraft was not located`.
- **Tags that reference other tags crashed with a `StackOverflowError`.** Resolving a `#namespace:tag`
  entry called the public `blocksIn`/`itemsIn`, which re-enter the tag cache - and the cache is not
  populated until resolution finishes, so resolution restarted from the top and recursed until the
  stack died. Any pack whose tag files reference each other hit it. The resolver now reads
  code-registered membership straight from its own map.

### Changed
- **Contextual tool tiers now receive the player**: `.tier((stack, block, player) -> ...)`, where the
  old form took `(stack, block)`. The documentation always advertised "a tool that is weaker in the
  nether", but the lambda had no way to reach a world, so it could not actually be written. The player
  carries one. It is `null` only when a tier is asked for outside a harvest - a tooltip, another mod's
  query. `RetroToolTier.of(stack, block)` still exists and passes `null`.

### Testing
- **New launch smoke suite: `./gradlew smokeTest`** (`clientSmokeTest`, `serverSmokeTest`, and both
  again under `-Pstationapi`). Each one boots the game, reads RetroAPI's mixin configs, and force-loads
  every class RetroAPI mixes into with `initialize = false` - so every mixin is applied and every
  injector runs, in one pass, in seconds. Mixin only validates a mixin when the game happens to load
  its target, which is why a broken `@Shadow` on a render class can ship: nothing in a build, and
  nothing in a data test, ever loads it. The client run also asserts the particle registry resolves and
  that the world-renderer hook is present in the class.
- The server half runs in CI. The client half needs a display, so run it locally before releasing.
- The test mod now registers a particle and ships tag files that reference other tags, so both of the
  above crashes are covered by a test that would have caught them.

## 0.3.0 - The `retroapi` entrypoint

All items below are exercised by the test mod's headless self-checks (`runPopulateServer`),
logged as `[new-features] ... PASS`.

### Entrypoints
- **New `retroapi` / `retroapi-client` / `retroapi-server` entrypoints.** Implement
  `RetroModInitializer.initRetro()` (plus the client/server interfaces) and declare them in
  `fabric.mod.json`. RetroAPI invokes them at the one point where registration is safe: after its
  registries, tag defaults and lang files are ready, after vanilla's order-sensitive
  `Block`/`Item`/`Stats` static-init cycle has been entered from the safe side, before RetroAPI's own
  registration events, before recipes are sorted, and before any world assigns ids. The loader's
  `init` stage has **no defined order**, so a consuming mod could (and intermittently did) register
  into a platform that was not built yet. They also fire identically with and without StationAPI,
  which RetroAPI's registration events deliberately do not. A failure names the offending mod.
  `init`/`client-init`/`server-init` keep working unchanged.

### Fixes
- **Recipes (and smelting, fuels, achievement icons) no longer silently stop working.** An
  `ItemStack` stores a numeric id; mods build them at init, when ids are still provisional, while the
  real ids arrive later from a world's `id_map.dat` or a server's sync. Every stack built beforehand
  kept pointing at the old number, so recipes stayed in the list - right count, no matches - unless
  this session's provisional ids happened to match, which is why relaunching sometimes "fixed" it.
  RetroAPI now collects every id change into an `IdRemap` and repairs its own tables, then fires
  `IdRemapCallback` so mods can fix stacks/ids they cached themselves, then re-sorts the crafting list
  (so late-registered recipes are ordered too).
- **Modded tools can now break modded blocks, and vanilla tools can break modded blocks.** Two halves
  of one trap: beta answers "is this tool effective?" from hardcoded block *lists* inside each vanilla
  tool item. (a) A block with no `mineable` tag now infers one from its material (stone/metal →
  pickaxe, wood → axe, soil/sand/snow → shovel, leaves → shears/sword/hoe, wool/cobweb → shears);
  (b) RetroAPI hooks `Item.isSuitableFor`, so a declared `.tool(...)` item satisfies vanilla's own
  check - drops, breaking speed and the correct-tool test now agree. Leaves are mineable by
  shears/sword/hoe as a result.
- **Interface injection actually applies on Ornithe.** The `loom:injected_interfaces` keys were
  babric-era intermediary names (`class_17`/`class_124`); the Ornithe build uses calamus gen2, so
  nothing was ever injected and consumers had to cast. Both namespaces are declared now.
- **`.states(...)` after `.facing()` no longer drops the facing property.** The two declarations merge
  in either order; auto-facing also preserves any state placement already wrote.
- **Blocks with more than 16 states survive break → place.** `BlockItemStateMixin` decodes the stack's
  damage as a flattened state index after placement and writes it back through `RetroStates` (nibble +
  sidecar), instead of truncating to the metadata nibble.
- `Block.setUnbreakable()` is reachable as `.unbreakable()` (no subclass just to call a protected setter).

### Particles (new)
- `RetroParticleRegistry.register(id, factory)` (client) + `RetroParticles.spawn/spawnCloud/spawnOnBlock`
  (common). Beta resolved particle names in a hardcoded `if` chain with no way in; namespaced names now
  resolve through the registry and unknown names fall through to vanilla.
- `RetroSpriteParticle`: a ready-made particle drawing any registered texture, chainable
  (`lifetime`, `scale`, `gravity`, `drag`, `tint`, `shrink`), drawing the whole sprite rather than a
  quarter of it. Particle fields are widened so mod subclasses can set themselves up.
- **Multiplayer bridge.** Vanilla's server-side `addParticle` is an empty method and the protocol has no
  particle packet, so server-spawned particles reached nobody. Now bridged to players in range, exactly
  like the existing sound bridge.

### World generation (new)
- `RetroFeatures.ore(block) / cluster(block, size) / custom(feature)` with placement: `.count(n)`,
  `.heightRange(min, max)`, `.rarity(n)`, `.dimensions(...)`, and for ores `.size(n)`, `.meta(n)`,
  `.replace(blocks…)` / `.replaceAnything()`. Vanilla's `OreFeature` has no rarity, no metadata, and
  replaces only stone; adding anything at all previously required a mixin into the chunk generator.
- The hook sits on the chunk cache, so vanilla dimensions, modded dimensions and custom generators are
  all covered; the decoration `Random` is seeded exactly like vanilla's, so seeds still reproduce.
  A throwing feature is logged and skipped rather than taking the chunk down.

### Blocks
- **Per-face textures in code:** `.sided(top, side, front[, bottom])`, `.column(top, side)`,
  `.textures(bottom, top, north, south, west, east)` - the furnace/log look with no model JSON, no
  blockstate file and no `getTexture` override. `.sided(...)` follows the facing state (4- or 6-way).
- **`.facingAll()`** - six-way facing (`RetroDirection.PROPERTY`), the dispenser/piston rule.
- **`.tint(provider)`** - per-position color for plain code-textured blocks, not just model faces with
  a `tintindex`; drives the inventory form too.
- **`.overlay(...)`** - extra render passes over the same block, each with its own tint (vanilla's
  grass-edge trick, generalised). Static, tinted, or chosen per position by a provider.
- **`.tag(...)`, `.needsTool(tier)`, `.unbreakable()`** - tags and tiers at registration.
- **`RetroCharProperty`** - characters as state values (`letters`, `digits`, or an explicit set).

### Tools & items
- **Tools without a `ToolMaterial`** (which is an enum mods cannot extend): `.miningSpeed(f)`,
  `.attackDamage(n)`, `.durability(n)`, `.damageOnMine(bool)` on any item.
- **`RetroToolTier.Contextual`** - `.tier((stack, block) -> ...)`, a tier that sees what is being mined
  (diamond-tier on stone, wood-tier elsewhere), consulted per harvest ahead of the stack-only form.

### Positions, directions, multiblocks
- **`RetroVec3i`** - immutable block position: `add`/`subtract`/`multiply`/`negate`, `up/down/north/…`,
  `offset(direction[, n])`, `rotateTo(facing)`, distances, and world access (`blockId`, `block`, `meta`,
  `state`, `setBlock`, `setState`, `blockEntity`).
- **`RetroDirection`** - all six, with `offsetX/Y/Z`, `vector()`, `face()`/`fromFace()`, `opposite()`,
  `rotateLeft/Right()`, `isHorizontal()`, `axis()`, `fromYaw`, `fromPlacer`, `nearest(dx, dy, dz)`.
  `RetroFacing` gained the same helpers plus `toDirection()`.
- **`RetroMultiblock`** - declare a structure as ASCII layers plus a legend (block, state, any-of, or a
  predicate), match it at a position in one facing or `matchAnyRotation`, read back every matched
  position (optionally by pattern character), and `fill(...)` to form it.
- **`BlockEntityLoadedCallback`** - fires once per block entity on its first tick, i.e. after NBT is read
  AND the world exists; replaces the hand-rolled "have I initialized yet?" boolean.

### Logging
- **RetroAPI is quiet now.** Registration, id assignment (one line *per item*), tag/lang/sound loading,
  atlas expansion, id-sync packets and per-teleport chatter all moved to the debug logger. A normal
  launch prints a single `RetroAPI ready: N blocks, M items, E entities, D dimensions` line (plus the
  StationAPI notice when it applies); warnings and errors are untouched.

### Rendering
- `BlockRenderContext` gained the primitives a connected-texture/decal renderer needs:
  `spriteOverride`, `clearSpriteOverride`, `flipTexture`, `faceRotation`, `clearFaceRotations`,
  `renderAllFaces`, plus `renderFaceUv(face, sprite, u0, v0, u1, v1[, flip])` and
  `renderFaceCorner(face, sprite, corner, flip)` for explicit sub-rectangles of a sprite.


## 0.2.4 - State-aware drops

### Block states
- **Blocks with more than 16 states now emit metadata-preserving drops.** `Block.dropStacks` handed the
  vanilla nibble (bits 0-3) to `getDroppedItemId`/`getDroppedItemMeta`, so a block whose flattened state
  index spills into the chunk sidecar could not recover its full state at drop time. A new
  `register.BlockDropStateMixin` swaps the nibble for `RetroStates.get(...).getIndex()` whenever the block
  has an explicit state definition wider than the nibble; decode it in your hook with
  `RetroStates.fromIndex(this, meta)`. Vanilla blocks, implicit-meta blocks, and `<= 16`-state blocks are
  untouched.

## 0.2.3 - Sharper tools & tags

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
  hand-breakable material - use a stone/metal material or a `needs_<tier>_tool` tag for that.
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
- **`.facing()`** - built-in `RetroFacing` property + furnace-like orient-to-placer on placement,
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
