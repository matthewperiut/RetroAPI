# RetroAPI changelog

## 0.3.7 - World generation, a launcher that works, and blocks that are not cubes

Four releases' worth of work going out as one, because none of it shipped: 0.3.6 was the last tag, and
everything below has been sitting on main since.

### World generation: modern noise, cubic biomes, carvers, and world height


Four additions, all from the same gap. RetroAPI could add a block to a world and could add a feature to
decoration, and had nothing at all to say about the two things a world-generation mod actually needs: the
*shape* of the terrain, and somewhere to record what a place **is**.

#### Added

- **`world.noise`: the modern noise stack, ported.** Beta ships `OctavePerlinNoiseSampler`, which takes an
  octave *count* and halves the amplitude each step - one fixed spectrum. Modern worldgen takes a
  `firstOctave` and an amplitude *per octave*, so a noise can be deliberately lumpy at one scale and smooth
  at another, and it sums two Perlins at incommensurable scales so the lattice artifacts that make a single
  Perlin's tunnels run along the axes cancel out. Neither is expressible on beta's sampler, and both are
  load-bearing for anything that wants modern cave or terrain shapes.

  `RetroNormalNoise` / `RetroPerlinNoise` / `RetroImprovedNoise` / `RetroXoroshiro` /
  `RetroPositionalRandom` / `RetroDensity` are faithful ports with no dependency on beta's or Mojang's
  classes, including the positional random factory - noises are forked by *name*
  (`fromHashOf("mymod:cheese")`), which is what keeps a dozen simultaneous noises independent and means
  adding a new one later does not shift the ones a world already generated with.

  ```java
  RetroPositionalRandom forks = new RetroXoroshiro(world.getSeed()).forkPositional();
  RetroNormalNoise cheese = RetroNormalNoise.create(
      forks.fromHashOf("mymod:cave_cheese"), -8, 0.5, 1, 2, 1, 2, 1, 0, 2, 0);
  double d = cheese.getValue(x, y, z, 1.0, 2.0 / 3.0);
  ```

  `RetroDensity` carries the density-function transforms (`squeeze`, `quarterNegative`, `intervalSelect`,
  `yGradient`, ...) so a formula transcribed from modern worldgen data reads the way its source did.
- **`RetroCubicBiomes`: biomes on a cubic grid, stored with the world.** Beta has exactly one notion of
  where you are: `Biome.getBiome(temperature, rainfall)`, a pure function of two 2D noises. Everything at
  an X/Z is in the same biome from bedrock to sky and the answer is recomputed from the seed rather than
  stored. That cannot express a cave - a mushroom cavern under a desert is not a property of the desert,
  two caverns a hundred blocks apart vertically are not the same place, and a cavern a player has walked
  through has an identity that has to survive being unloaded.

  So: a sparse grid of 16x16x16 cells, each either **unassigned** or holding a registered
  `RetroCubicBiome`. `get` returns `null` for unassigned rather than a fallback biome, because "no cave
  biome here" is the right answer for most of the world and code that must distinguish the two cannot if
  the API lies about it. A chunk whose cells are all unassigned writes **no sidecar section at all**, so
  its file stays byte-identical to a 0.3.6 one.

  ```java
  public static final RetroCubicBiome MUSHROOM = RetroCubicBiomes.register(id("mushroom"));

  RetroCubicBiomes.setCell(chunk, cellY, MUSHROOM);              // during generation
  RetroCubicBiome here = RetroCubicBiomes.get(world, x, y, z);   // null when unassigned
  ```

  Cells are stored by *name* through a per-chunk palette, and a cell naming a biome whose mod is missing
  this session is parked and written back out on save rather than erased - the same discipline the modded
  block and block-reference sections already use, for the same reason. Sync rides its own
  `retroapi:cubic_biome` channel rather than the chunk packet, deliberately: `WorldChunkPacketMixin` is
  disabled under StationAPI, so a section on that packet would silently stop syncing in exactly the
  configuration that has to work.
- **`RetroCarvers`: carving you can register, and vanilla carving you can turn off.** Beta welds caves into
  its generators - each constructs its own `CaveCarver` and calls it from `getChunk`, with no registry, no
  event and no switch. A mod wanting different caves had to mixin every generator, or carve afterwards
  through `world.setBlock` and pay lighting and re-render costs per block of every cave in the world.

  ```java
  RetroCarvers.setVanillaCarving(0, false);
  RetroCarvers.register(new MyNoiseCarver()).dimension(0).register();
  ```

  Carvers run against the chunk's raw `byte[]` before lighting, heightmaps or decoration exist, which is
  the cheapest possible place to shape terrain. `RetroCarverContext.surfaceTop` deserves a note: it reports
  the topmost **solid** block, not the heightmap, because beta's heightmap counts water - a carver that
  measured depth from it would open holes in sea floors and drain oceans into the caves.
- **`RetroWorldHeight`: per-dimension vertical extension.** Beta's Y is seven bits wide in the chunk array,
  so a block at y128 or y-1 has nowhere to go - which turns out to be what makes extension vanilla-safe:
  the blocks live in the sidecar (a new dense `vext` section, absent from chunks with none) and a vanilla
  session plays an ordinary 0-127 world, unaware and undamaged.

  ```java
  RetroWorldHeight.extend(0, 48, 0);        // overworld: y -48 .. 127
  RetroWorldHeight.setBlock(world, x, -20, z, Block.STONE.id, 0);
  ```

  With a downward extension the y0-4 bedrock band is **not** duplicated at the new bottom; `bedrockY`
  reports the extended floor and the world's floor moves down rather than the world gaining a second one
  halfway up its new depth. The cost is explicit: a vanilla session then finds breakable stone at y0.

  **Scope: this release ships the data layer only.** Bounds, storage, persistence and block read/write all
  work. Client rendering and lighting of the extension range do **not** - beta's `WorldRenderer` builds its
  chunk grid over a hardcoded 0-127 column and the light arrays are sized to match, so a block below y0 is
  saved and reloaded correctly and is not drawn or lit. A dimension that never calls `extend` is completely
  unaffected either way.

### Stop shipping a broken launcher to every consumer


A packaging fix, and it is the kind worth a release of its own because nobody downstream could work around
it.

#### Fixed
- **`starac` no longer reaches consumers.** It was declared `modRuntimeOnly`, and `maven-publish` exports
  `runtimeOnly` dependencies into the POM as `scope=runtime`, so every mod that depended on RetroAPI also
  silently got starac on its runtime classpath. starac's `MinecraftMixin` redirects a `noCanvas` target that
  does not exist on Fabric's applet launch path, and a redirect that finds no target is a **hard** mixin
  failure, not a warning:

  ```
  Critical injection failure: Redirector noCanvas(Lnet/minecraft/client/Minecraft;Ljava/awt/Canvas;)V
  in starac.mixins.json:MinecraftMixin from mod starac failed injection check, (0/1) succeeded
  ```

  So on any machine whose environment launches through the applet entrypoint (macOS does), `runClient` in a
  consumer's project died before drawing a frame, in a mixin belonging to a mod they never asked for and
  could not remove without knowing to exclude it by hand. RetroAPI's own smoke suites never caught it
  because they launch through a different path.

  starac is replaced by **retrodragon 0.1.10**, which is the launcher that actually works here. It is still
  `modRuntimeOnly` and so still exported at runtime scope, which is now a feature rather than a trap: a
  consumer gets a working LWJGL 3 launcher instead of a broken one.
- **`ComponentNbt.write` no longer crashes on a null component type.** `read` had always skipped a
  component whose type could not be resolved; `write` assumed every key was non-null and threw an NPE
  otherwise. It runs from `ItemStack.writeNbt`, so one bad component took down the save of a whole
  inventory, and from a tick it took down the game. Unknown types are now skipped with an error naming the
  cause (a mod calling `RetroComponents.set` with a static its own init had not assigned yet).

#### Changed
- **Toolchain moved up to match, because retrodragon requires it.** Loom `1.15-SNAPSHOT` to `1.17.13`,
  ploceus to `1.17.4`, the Gradle wrapper to `9.6.1` (Loom 1.17 needs the newer plugin API), and Fabric
  Loader to `0.19.3` (retrodragon declares a hard dependency on it). Two consequences of the ploceus and
  Loom bump are worth recording because both fail silently:
  - the six exception/signature/nest configurations now have to be declared **before**
    `ploceus.mappings(...)`, which resolves them; declared after, they are still empty and the game jar is
    remapped with no inner classes at all;
  - Loom 1.17 claims the whole `org.lwjgl` group for Mojang's libraries repo, which mirrors LWJGL 3.4.0
    only for the platforms Minecraft itself ships, so `natives-linux-arm64` has to be claimed from Central.

Both launch smoke suites pass, client and server: 57/57 client mixin targets and 46/47 server (one is not on
that side's classpath) applying cleanly.

### Partial blocks look like partial blocks


Two rendering fixes, both from the same shape of assumption: beta decides something once, for a whole
block, and then applies the answer to a piece of one.

#### Fixed
- **Smooth lighting on anything smaller than a full cube.** Beta computes ambient occlusion as four
  brightness values, one per corner of the whole block face, then hands them to the face methods, which
  draw the quad at the block's current bounding box. For a full cube those are the same four corners and
  nothing is wrong. For a stair's upper step, which spans half the block, the entire light gradient is
  stretched across half the distance - and because a stair is two boxes drawn in two passes, the two
  halves stretch the same gradient over different sub-rectangles and disagree where they meet. That
  seam down the middle of every beta stair is this, and every partial block has some version of it.

  Those four numbers describe a bilinear field over the face, so the fix is to sample it at the corners
  the quad actually occupies, which is what modern Minecraft's ambient-occlusion pass does. Hooked on
  the six face methods rather than the renderer above them, so vanilla stairs and slabs, pressure
  plates, cake, RetroAPI's own `renderLitFace` and any mod's custom renderer are all corrected without
  asking for it. A full-size face samples its own corners and gets its own values back, so ordinary
  blocks render exactly as they did.

#### Added
- **`RetroBlockAccess.droppedItemScale(float)` / `compactDroppedItem()`.** Beta draws a dropped block at
  quarter scale unless it is not a full cube, in which case it doubles it:

  ```java
  float scale = 0.25F;
  if (!block.isFullCube() && id != Block.SLAB.id && block.getRenderType() != 16) scale = 0.5F;
  ```

  That is a rule about torches and flowers, which are mostly empty space and want to be visible on the
  floor. Applied to a block that is *nearly* a cube it is just wrong: a cactus, a stair or any
  custom-rendered shape lands at twice the size of every other item in the pile and clips through the
  ground. There was no way to opt out, because the rule reads only `isFullCube()`, which such a block
  cannot answer true to without lying to collision and lighting as well. `compactDroppedItem()` is the
  value every Minecraft version after beta uses for everything. A block that never calls it keeps beta's
  number exactly.

### Partial blocks line up, and can be flat in the inventory


#### Fixed
- **Texture alignment on the two faces beta draws mirrored.** Every face method takes its texture
  coordinates from the block's bounding box, so a box covering half the block gets half the texture, and
  on four of the six faces that is also where you would expect it. The other two emit their horizontal
  coordinate reversed: `renderEastFace` gives the vertex at `maxX` the coordinate computed from `minX`,
  and `renderSouthFace` gives the vertex at `minZ` the one computed from `maxZ`. On a full cube that only
  mirrors the tile, which is invisible on a symmetric texture and old enough to count as the intended
  look. On a partial box it also takes the wrong half: a stair's upper step, spanning x 0.5 to 1, is drawn
  with texture columns 8 to 16, while a whole block in that same space shows columns 0 to 8 there, because
  it is mirrored too. The step does not line up with the block beside it, and the seam runs the length of
  every staircase.

  The mirroring is kept and the sub-rectangle it implies is used instead: a point at `c` across the block
  shows column `16 - 16c`. Written that way a full face comes out byte-identical to vanilla's, epsilon
  included, so ordinary blocks do not move. The correction stands down on out-of-range bounds, on an
  active face rotation and on an explicit `flipTextureHorizontally`, since in each of those it can no
  longer tell which coordinate it is holding.

#### Added
- **`RenderType.setFlatItem(id)` / `registerFlatItem(id, renderer)`.** Beta already decides, in
  `BlockRenderManager.isSideLit`, whether a block's item form is a small 3D block or a flat sprite, and
  draws its door, ladder, torch and pane flat for a good reason: a three pixel thick panel is unreadable
  at inventory size, and those shapes are recognised from an outline rather than from a perspective view
  of an edge. Custom render types were forced to the 3D form. This is the way to say otherwise, and it
  covers the inventory slot, the hand and a dropped stack together, because all three ask that one
  question. It is per render type rather than per block because that is the only thing `isSideLit` is
  given.

### The StationAPI half of the smoke suite could not run

`smokeTest -Pstationapi` had stopped launching anything, and two of the three reasons came in with the
toolchain move above. `:stationapi` and `:test:stapi` never got the `org.lwjgl` claim the root project
took, so neither could be configured at all, and `:test:stapi` still asked for the `starac_version`
property that went away with starac. A suite that cannot start looks exactly like a suite with nothing
to report.

With it running again it immediately earned its keep: `DroppedItemScaleMixin` crashed the StationAPI
client on load. Arsenic merges `ItemRenderer.render`, and an injector cannot target a method another mixin
has merged at the same priority, which is a hard `InvalidInjectionException` at class load rather than a
hook quietly not applying, so `require = 0` does not cover it. The native mixin is disabled under
StationAPI.

The feature itself still works there. Raising the priority until the injection was legal showed both the
scale constant and the `glScalef` call it guards report `Scanned 0 target(s)`, so arsenic really has
rewritten that branch, but it kept beta's rule verbatim one class along in
`ArsenicItemRenderer.renderVanilla`. The StationAPI module corrects it there instead, and the declaration
a block makes is the same either way.

### A block can present as another block

`RetroBlockDisguise` answers, per position, three questions beta only ever answers per block type. All
three had the same shape of problem: the value lives on the block, and a block whose appearance is per
position has no way to vary it.

| | where beta decides it | why a hook was needed |
| --- | --- | --- |
| which tool works | `mineableTools(Block)` | takes a block, no coordinates |
| what its particles look like | `BlockParticle` reads `block.getTexture(0, meta)` | the block's static sprite |
| what it sounds like | `Block.soundGroup` | a field on the block type |

```java
public class FramedBlock extends Block implements RetroBlockDisguise {
    public Block disguisedBlock(BlockView world, int x, int y, int z) { return whatItWears(world, x, y, z); }
}
```

Tools are **additive**, deliberately: a wooden frame wearing stone answers to an axe *and* a pickaxe.
Taking the axe away because of what it is currently wearing would mean a block got harder to break the
more it had been decorated. The position comes from `RetroBreakTarget`, which validates against the
world, so a stale record cannot answer for somewhere else.

Sounds needed two hooks rather than one, and missing the second is the obvious mistake: beta plays a
tapping sound every four ticks while a block is being mined, from the interaction managers, and a
separate crunch when it finally gives way, from world event 2001. The tapping is what you hear for
essentially the whole interaction. Fixing only the crunch leaves a stone-clad block sounding like wood
for exactly the part you were listening to.

The crunch and the cloud of debris also needed the disguise **written down before the block leaves**.
Both are produced by an event that arrives after the position is already empty, carrying an id, a
metadata value and no way to ask the world anything, so `RetroDisguises` records the disguise on removal
and those two read it back. A sixteen entry ring is enough: the event follows the removal within the tick.

## 0.3.6 - Auxiliary per-position block data

One addition, for the storage gap between "a few bits" and "a block entity". RetroAPI already had both
ends: 12 bits of block state, and vanilla's block entities. Everything that needs more than a nibble and
less than an object per block fell down the middle.

### Added
- **`RetroBlockData`: a 32-bit value per block position, per registered type, saved and synced.** State
  is 12 bits total for every property a block has, which is the right home for a stair's facing and half
  and cannot also hold "which block is this stair pretending to be" - a block reference alone needs 16.
  The other option was a block entity, and b1.7.3 walks every loaded block entity every tick and
  range-checks it against the chunk map before calling `tick()`: fine for the dozens of chests a world
  has, ruinous for the tens of thousands of blocks a decoration mod places, and paid forever for data
  that changes when a player right-clicks. This is sparse maps on the chunk instead, one entry per
  position that actually carries data, no per-tick cost at all, riding the save and sync plumbing the
  extended block ids already use.

  ```java
  public static final RetroBlockDataType CAMO = RetroBlockData.registerBlockRef(id("camo"));

  RetroBlockData.set(world, x, y, z, CAMO, RetroBlockData.encodeBlockRef(Block.GLASS.id, 0));
  int worn = RetroBlockData.get(world, x, y, z, CAMO);
  ```

  Data persists in the region sidecar (a new v4 section, absent from chunks that carry none, so files
  stay byte-identical to v3 otherwise), rides the chunk packet on chunk send, and is pushed to the
  players who can see a position when it changes. Reads work from the chunk-render thread's `WorldRegion`
  view, because a block's own renderer is exactly the caller that wants this.
- **`registerBlockRef`: a data type whose values are block references, saved by name.** A runtime block
  id is a property of the installed mod set, not of the world, so a raw int storing one means the day a
  mod is added or removed, every stored reference quietly points at a different block. Block-reference
  types go through a per-chunk string palette, the same way RetroAPI already stores the modded blocks
  themselves, and a reference whose mod is missing this session is parked and written back out on save
  rather than erased. Vanilla ids are fixed for all time and are written numerically.
- **A position's data is dropped when the block there changes.** Metadata and state changes (a door
  opening, a crop growing) go through `setBlockMeta` and keep their data; a genuine block change does
  not, so a value can never be inherited by whatever is placed at that position next.

## 0.3.5 - Right-click behavior, block entity sync, freeform multiblocks

Everything here comes from one modder hitting the same wall three different ways: RetroAPI gave you a
place to put your block, and nothing to put in it. A block could not gain right-click behavior without
breaking other mods, a block entity could not talk to the client without pretending to be a chest, and a
multiblock had to be a fixed shape with a controller in the middle.

### Added
- **`BlockUseCallback`: right-click behavior on any block, safely.** Beta's `CropBlock` (and most blocks)
  never overrides `onUse`, which tempts you into mixing a fresh `onUse` INTO `CropBlock` to add, say,
  right-click harvest. That method then shadows `Block.onUse`, and every other mod's `@Inject` into
  `Block.onUse` silently stops running for crops - their mod breaks, from three dependencies away, with
  no error anywhere. Listeners on this event compose instead: they run in registration order until one
  returns `SUCCESS` or `FAIL`, and it fires for every block, before the block's own `onUse`, so it can
  also replace or veto vanilla behavior. Hooked on the side that actually decides the interaction (the
  client in singleplayer, the dedicated server in multiplayer), so a listener runs exactly once per
  click.
- **`RetroSyncedBlockEntity`: block entity data on the client, without an inventory.** b1.7.3's protocol
  has no generic block-entity packet - the only one that carries block-entity data is the sign packet -
  so the only vanilla-shaped way for a modded block entity to reach the client was to masquerade as a
  container and push its state through the inventory/window packets. Anything that is not an inventory (a
  tank's fluid level, a machine's progress bar, a barrel's displayed stack) had no answer at all.
  Implement the interface and RetroAPI carries the block entity's NBT over its own channel, automatically
  on chunk send and on every `setBlockDirty`; `RetroBlockEntities.sync(blockEntity)` is the explicit push.
  It rides vanilla's own per-chunk player tracking, so only players who can see the block get the packet.
  Override `writeSyncNbt`/`readSyncNbt` to send less than goes to disk.
- **`RetroMultiblock.matchAnywhere(...)`: find the structure from any of its blocks.** `match` and
  `matchAnyRotation` both assume the position IS the anchor, which is the dedicated-controller shape:
  walk around to the core block and click that. This tries the position as every cell of the pattern in
  every rotation and returns the first whole structure standing around it, so right-clicking any part
  works. `Match.anchor()` reports where the controller actually landed.
- **`RetroBlockRegion`: freeform multiblocks.** A pattern is the wrong tool for a structure whose size
  and shape are the player's choice - a tank however many blocks tall, a room walled off with your
  bricks, a shrine that just has to be big enough. There is no pattern to write for those, only a rule
  for what counts as a member. `RetroBlockRegion.flood(...)` walks outward from any block through
  everything that rule accepts, face-adjacent or through corners, with a visit limit so an unbounded
  structure comes back marked incomplete rather than freezing the tick.
- **Registrable tool tiers.** `RetroToolTier` was an enum, which made its five tiers the only tiers that
  could ever exist: a mod adding bronze between stone and iron, or mithril above diamond, had to pick the
  nearest vanilla tier and lose the distinction. It is a registry now, and the built-ins are ordinary
  entries in it: `RetroToolTier.register("bronze", 1, 5.0F)`. Levels need not be unique, so two tiers can
  harvest the same blocks and still differ in speed and in which `needs_<name>_tool` tag they answer to.
  Being a class rather than an enum, it can no longer be used in a `switch` or an `EnumSet`; compare with
  `isAtLeast` or read `getLevel()`. `values()` is kept so existing loops still compile.
- **`RetroToolTier.Positional`: a tool tier that can see the block's position and state.** `Contextual`
  gets the `Block`, which is the block TYPE, and every state of a block is the same `Block` object, so
  "diamond-tier on lit ore, wood-tier on unlit" was not expressible. Beta's harvest hooks carry no
  coordinates at all, so RetroAPI records what a player is breaking (`RetroBreakTarget`, captured in the
  interaction managers) and hands the position over. Reads are validated against the world, so a stale
  record from an abandoned break can never answer for the wrong block, and the world is null outside an
  actual break rather than wrong.
- **`RetroBlockAccess.AUTO_ID`.** The item side has had this since 0.3.0 and the block side simply never
  got it. Pass it to a `Block` constructor in place of an id and RetroAPI fills in a free slot from
  inside the constructor, so the scan and the store are one atomic step.
- **`RetroBlockAccess.block()` and `RetroItemAccess.item()`.** Every builder method returns the RetroAPI
  interface, so from a consuming mod - where interface injection puts all of these methods on `Block` and
  `Item` themselves - a chain that starts with a RetroAPI call is stuck in RetroAPI's half of the API
  until `register` hands the vanilla type back. These are the way out and back mid-chain. (The return
  types cannot simply be flipped to `Block`/`Item`: RetroAPI cannot compile against its own interface
  injection, so its own source would stop chaining.)

### Fixed
- **Hoes no longer mine leaves faster.** Material inference gave any block with no declared tags a
  sensible default tool, and it was applying to vanilla blocks too. Leaves are the `LEAVES` material,
  modern Minecraft files leaves under `mineable/hoe`, and so merely installing RetroAPI handed every beta
  hoe a leaf-cutting speed bonus. Vanilla membership is spelled out block by block in `VanillaToolTags`,
  transcribed from beta's own tool code, and that list is now the whole truth about vanilla blocks;
  inference stops at the modded id range. A mod that wants beta leaves in a tag can still say so
  explicitly. A library has no business changing how vanilla plays.
- **`RetroBlockAccess.allocateId()` now reserves what it hands out.** It scanned `Block.BLOCKS` for a
  null slot and returned it, so two allocations before either constructor ran picked the same id and the
  second store silently won - the exact race `RetroItemIds` was written to close on the item side. Block
  ids now go through the same reserving, synchronized allocator.
- **`RetroModInitializer.initRetro()`'s own javadoc caused a bug.** It listed recipes and achievements
  among the things to register there. Every mod's `initRetro()` runs before any callback fires, but they
  run in mod load order relative to each other, so a recipe built there that names another mod's item
  works or fails depending on which mod loaded first: an intermittent crash that moves around when you add
  an unrelated dependency. The doc now says what the ordered callbacks
  (`RecipeRegistrationCallback`, `AchievementRegistrationCallback`) are for and spells out the full order.

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
