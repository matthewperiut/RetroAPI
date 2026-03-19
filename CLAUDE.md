# RetroAPI - AI Agent Onboarding

## What This Is

RetroAPI is a modular Fabric mod library for **Minecraft Beta 1.4 – Beta 1.7.3** that lets other mods register custom blocks, items, recipes, and more without requiring StationAPI. It handles ID assignment, texture atlasing, custom rendering, networking, and world persistence. When StationAPI is present (b1.7.3 only), RetroAPI delegates to it instead.

## Build & Run

```bash
./gradlew buildLibs                                          # Compiles library JAR only (no test mods)
./gradlew build -x validateAccessWidener                     # Compiles all modules + all test mods
./gradlew :test:test-mcb1.7.3:runClient                      # Launches client with test mod on b1.7.3
./gradlew :test:test-mcb1.4_01:runClient                     # Launches client with test mod on b1.4_01
./gradlew :test:test-mcb1.6.6:runClient                      # Launches client with test mod on b1.6.6
```

Java 21 required. Uses Fabric Loom + Ploceus (Feather mappings). Multi-version, multi-module project following the OSL pattern. Uses Manifold preprocessor for conditional compilation across MC versions.

## Mapping Gotchas (CRITICAL)

This project uses **Feather build 1** mappings (hash `845945349`), NOT modern Yarn/Mojmap. Many class/field/method names differ from what you'd expect:

| What you'd expect | Actual name in this project |
|---|---|
| `Chunk` | `WorldChunk` |
| `Chunk.x` / `Chunk.z` | `WorldChunk.chunkX` / `WorldChunk.chunkZ` |
| `Chunk.getBlockId()` | `WorldChunk.getBlockAt()` |
| `Chunk.setBlock(x,y,z,id)` | `WorldChunk.setBlockAt(x,y,z,id)` |
| `World.getChunk(x,z)` | `World.getChunkAt(x,z)` |
| `World.isRemote` / `isClient` | `World.isMultiplayer` |
| `WorldStorage.save()` | `WorldStorage.saveData(WorldData, List)` |
| `ItemStack.itemId` / `count` | `ItemStack.id` / `size` |
| `NbtCompound.containsKey()` | `NbtCompound.contains()` |
| `ChunkDataS2CPacket` | `WorldChunkPacket` |
| `ServerPlayNetworkHandler` | `net.minecraft.server.network.handler.ServerPlayNetworkHandler` |
| `ServerPlayerEntity` | `net.minecraft.server.entity.mob.player.ServerPlayerEntity` |

**NbtCompound has NO `getIntArray`/`putIntArray`** in b1.7.3. Use `putByteArray`/`getByteArray` with manual int↔byte conversion.

**Static methods matter for mixins.** `AlphaChunkStorage.loadChunkFromNbt` and `saveChunkToNbt` are static — mixin callbacks targeting them must also be `private static`.

To look up mappings yourself, the file is at:
`~/.gradle/caches/fabric-loom/b1.7.3/loom.mappings.b1_7_3.layered+hash.845945349-v2/mappings.tiny`

The merged MC jar with all named classes is at:
`~/.gradle/caches/fabric-loom/b1.7.3/loom.mappings.b1_7_3.layered+hash.845945349-v2/merged-unpicked.jar`
Use `jar tf` on it to verify class/package names before writing imports.

## OSL Networking API

- Client context: `ctx.minecraft()` (not `getMinecraft()`)
- Client listener: `ClientPlayNetworking.registerListener(channel, (ctx, buffer) -> { ... })`
- Server send: `ServerPlayNetworking.send(player, channel, writerLambda)`
- Channels registered via `ChannelRegistry.register(ChannelIdentifiers.from("retroapi", "name"), serverToClient, clientToServer)`

## Project Structure (Multi-Module)

Follows the OSL (Ornithe Standard Libraries) pattern. Each library has a parent directory with shared source and version-specific subdirectories.

```
retroapi/
├── build.gradle              # Root — helper functions (setUpLibrary, setUpModule) + fat JAR
├── settings.gradle           # All subproject includes
├── gradle.properties         # Global versions
├── src/main/resources/       # Root fabric.mod.json (retroapi mod ID)
│
├── libraries/
│   ├── registration/         # Blocks, items, textures, storage, networking, lang, ID management
│   │   ├── src/main/java/    # Shared source (uses #if preprocessor for version differences)
│   │   ├── registration-mcb1.4-mcb1.5_01/   # Version module: resources + access widener
│   │   └── registration-mcb1.6-mcb1.7.3/    # Version module: resources + access widener
│   │
│   ├── blockentity/          # RetroBlockEntityType, RetroMenu, SyncField, BlockActivatedHandler
│   │   └── blockentity-mcb1.4-mcb1.7.3/
│   │
│   ├── rendering/            # RenderType, RenderTypes, CustomBlockRenderer, BlockRenderContext
│   │   └── rendering-mcb1.4-mcb1.7.3/
│   │
│   ├── recipes/              # RetroRecipes — crafting, smelting, fuel registration
│   │   └── recipes-mcb1.4-mcb1.7.3/
│   │
│   └── stationapi/           # StationAPI compat bridge (b1.7.3 ONLY)
│       └── stationapi-mcb1.7.3-mcb1.7.3/
│
└── test/                     # Test mod
    ├── shared/               # Shared test source, resources, and access widener
    ├── test-mcb1.4_01/       # Per-version test runners (14 total, b1.4_01 through b1.7.3)
    ├── test-mcb1.6.6/
    ├── test-mcb1.7.3/        # b1.7.3 also includes StationAPI compat
    └── ...
```

### Module Dependency Graph
```
blockentity (standalone)
rendering (standalone)
registration (depends on: blockentity, rendering)
├── recipes (depends on: registration)
└── stationapi (depends on: registration; b1.7.3 only)
test (depends on: all)
```

Identifiers use OSL's `NamespacedIdentifier` interface (`net.ornithemc.osl.core.api.util.NamespacedIdentifier`) with factory `NamespacedIdentifiers.from(namespace, identifier)`. Methods: `.namespace()`, `.identifier()`, `.toString()` returns `"namespace:identifier"`.

### Manifold Preprocessor

Version differences in shared source are handled via Manifold preprocessor `#if` directives. Each version module defines symbols via `-A` compiler args in its `build.gradle`:

- **b1.6-b1.7.3**: `-AMC_B1_6_OR_LATER`, `-AMC_HAS_ACHIEVEMENTS`, `-AMC_HAS_UPDATE_CLIENTS`
- **b1.4-b1.5_01**: `-AMC_PRE_B1_6`

Usage in Java source:
```java
#if MC_B1_6_OR_LATER
Block.IS_SOLID_RENDER[this.id] = solid;
#else
Block.IS_SOLID[this.id] = solid;
#endif
```

Key version differences:
| Feature | b1.4-b1.5_01 | b1.6-b1.7.3 |
|---------|-------------|-------------|
| Solid render field | `IS_SOLID` | `IS_SOLID_RENDER` |
| Update clients field | doesn't exist | `UPDATE_CLIENTS` |
| `isSolidRender()` method | `isSolid()` | `isSolidRender()` |
| `BlockEntity.cancelRemoval()` | doesn't exist | exists |
| `ItemInHandRenderer.render` | `(ItemStack)` | `(MobEntity, ItemStack)` |
| `ItemRenderer.renderGuiItem` | `(TextRenderer, TextureManager, ItemStack, int, int)` | `(TextRenderer, TextureManager, int, int, int, int, int)` |
| `BlockRenderer.renderAsItem` | `(Block, int)` | `(Block, int)` b1.6.x / `(Block, int, float)` b1.7+ |
| `AchievementsScreen` | doesn't exist | exists |
| `Block$Sounds` (access widener) | `Block$Sounds` | `Block__Sounds` |

### Adding a New Version Range
1. Create `libraries/{module}/{module}-mc{min}-mc{max}/` with `build.gradle`, `gradle.properties`
2. In `build.gradle`: define preprocessor symbols via `-A` compiler args
3. Add version-specific resources (access widener, mixin config, fabric.mod.json)
4. Register in `settings.gradle` via `include`
5. Use `#if` directives in shared source for any API differences

## Key Architecture Concepts

### Block ID Expansion
Vanilla b1.7.3 uses `byte[]` for chunk block storage (max 256 IDs). RetroAPI:
1. Expands `Block.BY_ID` and related arrays from 256→4096 via `BlockArrayExpandMixin`
2. All RetroAPI blocks use IDs ≥ 256 (extended range). They are stored as `0` (air) in vanilla `byte[]`
3. `WorldChunkMixin` intercepts `getBlockAt`/`setBlockAt` to overlay real IDs from `ChunkExtendedBlocks`
4. Sidecar files (`retroapi/chunks/r.X.Z.dat`) persist extended blocks using **string identifiers** (not numeric IDs)

### ID Assignment Flow
1. Mods register blocks via `RetroBlockAccess.create(material).register(id)` during init. Placeholder IDs start at 256.
2. On world load (`AlphaWorldStorageMixin`), `IdAssigner.assignIds()` reads `retroapi/id_map.dat` and remaps blocks to stable numeric IDs
3. Stale entries in `id_map.dat` (blocks/items from removed mods) are purged automatically
4. `remapBlock()` takes `BlockRegistration` (not raw `Block`) so it uses the registration's stored `BlockItem` reference — this prevents BlockItem theft when IDs overlap during batch remapping
5. On multiplayer join, server sends ID mappings via `id_sync` channel; client remaps

### Vanilla Compatibility / Sidecar System

RetroAPI is designed so worlds can be opened in vanilla without crashing, and reopened in RetroAPI without data loss. All modded content is hidden from vanilla saves:

**Block Sidecar** (`retroapi/chunks/r.X.Z.dat`):
- Per-region files with string block identifiers for cross-mod stability
- Extended blocks (ID ≥ 256) are written as air in the vanilla byte array; real data lives in the sidecar
- Loaded on chunk load, saved on chunk save, flushed on world save

**Inventory Sidecar** (`retroapi/inventories/r.X.Z.dat`):
- Strips ALL modded content from vanilla chunk NBT before it reaches disk:
  - **Modded block entities** (e.g. CrateBlockEntity): stripped entirely from `TileEntities`, full NBT saved to sidecar
  - **RetroAPI items in vanilla inventories** (e.g. modded items in chests): stripped from `Items` lists, saved to sidecar
  - **Item entities** carrying RetroAPI items: stripped from `Entities`, saved to sidecar
- On load, restores everything from sidecar
- Block entity conflict handling: if vanilla placed a block entity at a modded position, the modded BE data is preserved in the sidecar (not overwritten) and re-checked each load

**ItemStack NBT** (`ItemStackMixin`):
- On write: saves `retroapi:id` (string identifier), `retroapi:count` (original count), `retroapi:damage` (original damage), then clamps `id=0, Count=0` so vanilla sees empty slots
- On read: if `retroapi:id` is present, resolves back to the correct numeric ID
- The clamped values exist only as a safety net for contexts not handled by the sidecar (e.g. player inventory). The sidecar reads original values from `retroapi:count`/`retroapi:damage`.

**Item entity restoration** creates `ItemEntity` directly via constructor (not NBT deserialization) to ensure proper pickup behavior. Position and motion are read from the saved entity NBT.

### Block Entities

RetroAPI blocks can have block entities without subclassing `BlockWithBlockEntity`:
- `RetroBlockEntityType<T>` registers the BE class and provides a factory
- `BlockMixin` injects into `onAdded`/`onRemoved` to create/remove BEs automatically
- `WorldChunkMixin` overrides `setBlockEntityAt`/`getBlockEntityAt` to bypass the vanilla `instanceof BlockWithBlockEntity` check for blocks with `HAS_BLOCK_ENTITY` flag
- Block activation (right-click) handled via `BlockActivatedHandler` callback set on the block

### Menus / Inventories

`RetroMenu.open(player, menu, menuType)` opens inventory GUIs:
- Uses `MENU_CHEST`, `MENU_FURNACE`, `MENU_DISPENSER` constants to select GUI type
- Client/server split: server sends vanilla open-window packet, client opens the appropriate GUI
- `@SyncField` annotation on menu fields enables automatic slot synchronization

### Translation / Lang

- `LangLoader` loads translations from `assets/{modid}/lang/en_US.lang`
- Auto-generates default translations for any block/item without one (e.g. `test_block` → `Test Block`)
- Works with both StationAPI and non-StationAPI (injects into `Language.translations` properties)

### Multiplayer Sync
- `ChunkSendMixin` hooks `ServerPlayNetworkHandler.sendPacket` — when a `WorldChunkPacket` is sent, it also sends extended block data via `chunk_ext` channel
- Client receives and populates `ChunkExtendedBlocks`, then triggers re-render

## Test Mod

Located in `test/`. Shared source in `test/shared/`, per-version runners in `test/test-mc{version}/`. Registers:
- 5 special blocks (test_block, color_block, pipe, crate with inventory, freezer with furnace-style menu)
- 200 numbered blocks (block_0 through block_199) — spawned in chests near the player
- 200 numbered items (item_0 through item_199) — spawned in a second row of chests
- 1 test item

14 test runners covering every beta version: b1.4_01, b1.5, b1.5_01, b1.6–b1.6.6, b1.7, b1.7_01, b1.7.2, b1.7.3. Run with `./gradlew :test:test-mc{version}:runClient`.

## Access Widener

Each registration version module has its own `retroapi.registration.accesswidener` — makes Block/Item fields mutable, Block constructor accessible, and Block static arrays accessible + mutable for runtime expansion. Also exposes BlockItem.block field. Field names differ between versions (e.g. `IS_SOLID` in b1.4 vs `IS_SOLID_RENDER` in b1.6+, `Block$Sounds` in b1.4 vs `Block__Sounds` in b1.7.3).

## Important Implementation Notes

- **All RetroAPI block IDs are ≥ 256.** `allocatePlaceholderBlockId()` and `findFreeBlockId()` both start from 256. This keeps modded blocks out of vanilla's 0-255 byte range entirely.
- **`remapBlock()` must use `BlockRegistration`'s stored BlockItem reference**, not blindly grab from `Item.BY_ID[oldId]`. During batch remapping, IDs can overlap (block A remapped TO id X, then block B remapped FROM id X), which causes BlockItem theft if not using the stored reference.
- **`NbtCompound` has no key iteration API** in b1.7.3. The `getKeys()` helper in `IdMap` and `InventorySidecar` serializes to bytes and re-reads the binary NBT format to extract keys.
- **`ItemStack.metadata` is private.** Use `getMetadata()` accessor or `@Shadow` in mixins.
- **`NbtDouble.value`** (not `.data`) for reading double values from NBT lists.
- **Item entities must be created via constructor**, not `Entities.create(nbt, world)`, to ensure proper pickup behavior and correct stack data.

## StationAPI Compatibility

When StationAPI is present (b1.7.3 only), `RetroAPIMixinPlugin` disables conflicting mixins:
- **Atlas mixins** disabled (StationAPI handles textures)
- **ItemStackMixin** disabled (StationAPI handles item NBT)
- **Network mixins** disabled: BlockUpdatePacket, BlocksUpdatePacket, WorldChunkPacket, ChunkSend, ClientNetworkHandler, ClientPlayerInteractionManager
- Registration delegates to StationAPI's registry (`StationAPICompat`)
- `IdAssigner.saveCurrentIds()` is called instead of `assignIds()` (StationAPI manages IDs)
- `BackupManager.backupRetroApiData()` runs before world format conversions
- `WorldConversionHelper` injects RetroAPI mappings into StationAPI's flattening schema
- Lang defaults are still injected (works for both StationAPI and non-StationAPI)

## Recipes Module

`RetroRecipes` API for crafting, smelting, and fuel:
```java
RetroRecipes.addShaped(output, "XXX", "X X", "XXX", 'X', new ItemStack(Item.STICK));
RetroRecipes.addShapeless(output, new ItemStack(Block.STONE), new ItemStack(Item.DYE));
RetroRecipes.addSmelting(inputBlockId, new ItemStack(Item.IRON_INGOT));
RetroRecipes.addFuel(itemId, 200); // 200 ticks = 10 seconds
```
