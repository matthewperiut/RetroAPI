package com.periut.retroapi.storage;

import net.ornithemc.osl.core.api.util.NamespacedIdentifier;
import net.ornithemc.osl.core.api.util.NamespacedIdentifiers;
import com.periut.retroapi.registry.BlockRegistration;
import com.periut.retroapi.registry.ItemRegistration;
import com.periut.retroapi.registry.RetroRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityRegistry;
import net.minecraft.entity.ItemEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.*;

public class InventorySidecar {
	private static final Logger LOGGER = LogManager.getLogger("RetroAPI/InventorySidecar");
	private static final int VERSION = 1;

	/**
	 * Block entities that couldn't be restored because a vanilla block entity
	 * occupied the position. Preserved so they can be re-saved to the sidecar.
	 * Keyed by "chunkX,chunkZ".
	 */
	private static final Map<String, NbtList> pendingBlockEntities = new HashMap<>();

	/**
	 * Modded full-entities whose id is not registered this session (the providing mod was
	 * removed) so we could not reconstruct them on load. Kept here so {@link #filterAndSave}
	 * re-writes them to the sidecar on every save, surviving repeated autosaves until the mod
	 * returns and they restore. Keyed by "chunkX,chunkZ". Unlike {@link #pendingBlockEntities}
	 * this is PEEKED (not removed) on save - a remove would drop the data on the second
	 * autosave of a still-loaded chunk. Cleared by {@link #restoreChunkContent} once the entity
	 * reconstructs successfully (mod returned).
	 */
	private static final Map<String, NbtList> pendingFullEntities = new HashMap<>();

	/**
	 * Drop all cross-load deferral state. Called on world change (SidecarManager.flush) so a chunk
	 * "x,z" deferred in one world can never leak its modded content into another world's chunk "x,z"
	 * (chunkKey is world-agnostic, and these maps are static).
	 */
	public static void clearPending() {
		pendingBlockEntities.clear();
		pendingFullEntities.clear();
	}

	private final File file;
	private NbtCompound root;
	private boolean dirty = false;

	public InventorySidecar(File file) {
		this.file = file;
		this.root = new NbtCompound();
		load();
	}

	private void load() {
		if (!file.exists()) {
			root.putInt("version", VERSION);
			root.put("chunks", new NbtCompound());
			return;
		}
		try (FileInputStream fis = new FileInputStream(file)) {
			root = NbtIo.readCompressed(fis);
		} catch (IOException e) {
			LOGGER.error("Failed to load inventory sidecar {}", file, e);
			root = new NbtCompound();
			root.putInt("version", VERSION);
			root.put("chunks", new NbtCompound());
		}
	}

	public void save() {
		if (!dirty) return;
		try {
			// Snapshot on this (main) thread, gzip+atomic-write on the SidecarIo background thread.
			byte[] payload = SidecarIo.snapshot(root);
			dirty = false;
			SidecarIo.writeAsync(file, payload);
		} catch (IOException e) {
			LOGGER.error("Failed to snapshot inventory sidecar {}", file, e);
		}
	}

	/**
	 * Filter the chunk NBT to remove all modded content:
	 * - Modded block entities (at positions with extended blocks) -> saved to sidecar
	 * - RetroAPI items in vanilla block entity inventories -> saved to sidecar
	 * - Item entities carrying RetroAPI items -> saved to sidecar
	 */
	public void filterAndSave(int chunkX, int chunkZ, NbtCompound nbt, ChunkExtendedBlocks extended) {
		if (!root.contains("chunks")) {
			root.put("chunks", new NbtCompound());
		}
		NbtCompound chunks = root.getCompound("chunks");
		String chunkKey = chunkX + "," + chunkZ;
		NbtCompound chunkData = new NbtCompound();

		// 1. Filter TileEntities
		NbtList tileEntities = nbt.getList("TileEntities");
		if (tileEntities != null) {
			NbtList filteredTEs = new NbtList();
			NbtList moddedBEs = new NbtList();
			NbtList inventoryItems = new NbtList();

			for (int i = 0; i < tileEntities.size(); i++) {
				NbtCompound be = (NbtCompound) tileEntities.get(i);
				int bx = be.getInt("x");
				int by = be.getInt("y");
				int bz = be.getInt("z");
				int localX = bx & 15;
				int localZ = bz & 15;
				int index = ChunkExtendedBlocks.toIndex(localX, by, localZ);

				if (extended.hasEntry(index)) {
					// Modded block entity - strip entirely
					moddedBEs.add(be);
				} else {
					// Vanilla block entity - check for RetroAPI items in inventory
					if (be.contains("Items")) {
						NbtList items = be.getList("Items");
						NbtList filteredItems = new NbtList();
						boolean hasModdedItems = false;

						for (int j = 0; j < items.size(); j++) {
							NbtCompound item = (NbtCompound) items.get(j);
							if (item.contains("retroapi:id")) {
								// RetroAPI item - save to sidecar
								NbtCompound invEntry = new NbtCompound();
								invEntry.putInt("x", bx);
								invEntry.putInt("y", by);
								invEntry.putInt("z", bz);
								invEntry.putByte("Slot", item.getByte("Slot"));
								invEntry.putString("retroapi:id", item.getString("retroapi:id"));
								invEntry.putByte("Count", item.contains("retroapi:count")
								? item.getByte("retroapi:count") : item.getByte("Count"));
								invEntry.putShort("Damage", item.contains("retroapi:damage")
								? item.getShort("retroapi:damage") : item.getShort("Damage"));
								// Carry the stack's data components so they survive the strip/restore round-trip
								// (restoreChunkContent reads them back via ComponentNbt.read).
								if (item.contains("retroapi:components")) {
									invEntry.put("retroapi:components", item.getCompound("retroapi:components"));
								}
								inventoryItems.add(invEntry);
								hasModdedItems = true;
							} else {
								filteredItems.add(item);
							}
						}

						if (hasModdedItems) {
							be.put("Items", filteredItems);
						}
					}
					filteredTEs.add(be);
				}
			}

			nbt.put("TileEntities", filteredTEs);
			// Merge in any pending (deferred) block entities that couldn't be restored
			NbtList pending = pendingBlockEntities.remove(chunkKey);
			if (pending != null) {
				for (int i = 0; i < pending.size(); i++) {
					moddedBEs.add(pending.get(i));
				}
			}
			if (moddedBEs.size() > 0) {
				chunkData.put("blockEntities", moddedBEs);
			}
			if (inventoryItems.size() > 0) {
				chunkData.put("inventoryItems", inventoryItems);
			}
		}

		// 2. Filter Entities - strip RetroAPI modded content so vanilla never silently drops it
		NbtList entities = nbt.getList("Entities");
		if (entities != null) {
			NbtList filteredEntities = new NbtList();
			NbtList moddedEntities = new NbtList();   // item-entity carriers of RetroAPI items
			NbtList fullEntities = new NbtList();      // RetroAPI modded full entities (mobs etc.)

			for (int i = 0; i < entities.size(); i++) {
				NbtCompound entity = (NbtCompound) entities.get(i);
				String entityId = entity.getString("id");

				// (a) Item entities carrying RetroAPI items - strip to itemEntities (existing behavior).
				if ("Item".equals(entityId)) {
					NbtCompound itemTag = entity.getCompound("Item");
					if (itemTag != null && itemTag.contains("retroapi:id")) {
						moddedEntities.add(entity);
						continue;
					}
					filteredEntities.add(entity);
					continue;
				}

				// (b) RetroAPI-registered modded full entity - strip the whole compound verbatim.
				//     Vanilla saveSelfNbt wrote id = registration.getId().toString() (e.g. "aether:moa"),
				//     so this round-trips through RetroRegistry. Non-null lookup => it is ours.
				if (entityId != null && !entityId.isEmpty()
						&& RetroRegistry.getEntityByStringId(entityId) != null) {
					fullEntities.add(entity);
					continue;
				}

				// (c) Vanilla entity - leave it in the chunk NBT.
				filteredEntities.add(entity);
			}

			nbt.put("Entities", filteredEntities);
			// Carry forward modded full-entities we couldn't reconstruct (mod removed) so they
			// survive this save and restore when the mod returns. PEEK, do not remove (see field doc).
			NbtList pendingFull = pendingFullEntities.get(chunkKey);
			if (pendingFull != null) {
				for (int i = 0; i < pendingFull.size(); i++) {
					fullEntities.add(pendingFull.get(i));
				}
			}
			if (moddedEntities.size() > 0) {
				chunkData.put("itemEntities", moddedEntities);
			}
			if (fullEntities.size() > 0) {
				chunkData.put("fullEntities", fullEntities);
			}
		}

		chunks.put(chunkKey, chunkData);
		dirty = true;
	}

	/**
	 * Restore modded content from sidecar into a loaded chunk:
	 * - Modded block entities (with block placement verification)
	 * - RetroAPI items in vanilla block entity inventories
	 * - Item entities carrying RetroAPI items
	 */
	public void restoreChunkContent(Chunk chunk, World world) {
		if (!root.contains("chunks")) return;
		NbtCompound chunks = root.getCompound("chunks");
		String chunkKey = chunk.x + "," + chunk.z;

		if (!chunks.contains(chunkKey)) return;
		NbtCompound chunkData = chunks.getCompound(chunkKey);

		ChunkExtendedBlocks extended = ((ExtendedBlocksAccess) chunk).retroapi$getExtendedBlocks();

		// 1. Restore modded block entities
		if (chunkData.contains("blockEntities")) {
			NbtList moddedBEs = chunkData.getList("blockEntities");
			NbtList deferredBEs = new NbtList();

			for (int i = 0; i < moddedBEs.size(); i++) {
				NbtCompound beNbt = (NbtCompound) moddedBEs.get(i);
				int bx = beNbt.getInt("x");
				int by = beNbt.getInt("y");
				int bz = beNbt.getInt("z");
				int localX = bx & 15;
				int localZ = bz & 15;
				int index = ChunkExtendedBlocks.toIndex(localX, by, localZ);

				// Check for existing vanilla block entity at this position
				BlockPos pos = new BlockPos(localX, by, localZ);
				BlockEntity existing = (BlockEntity) chunk.blockEntities.get(pos);
				if (existing != null) {
					// Vanilla block entity here - don't overwrite, keep data for next load
					deferredBEs.add(beNbt);
					// Also remove the extended block entry so vanilla block takes priority
					if (extended.hasEntry(index)) {
						extended.remove(index);
					}
					continue;
				}

				// Ensure the modded block is present at this position
				if (!extended.hasEntry(index)) {
					// Extended block missing - try to restore it from the BE's associated block
					// Skip if we can't determine the block
					deferredBEs.add(beNbt);
					continue;
				}

				// Verify the block has HAS_BLOCK_ENTITY set
				int blockId = extended.getBlockId(index);
				if (blockId > 0 && blockId < Block.BLOCKS.length && Block.BLOCKS[blockId] != null) {
					Block.BLOCKS_WITH_ENTITY[blockId] = true;
				}

				BlockEntity be = BlockEntity.createFromNbt(beNbt);
				if (be != null) {
					chunk.addBlockEntity(be);
				}
			}

			if (deferredBEs.size() > 0) {
				pendingBlockEntities.put(chunkKey, deferredBEs);
			} else {
				pendingBlockEntities.remove(chunkKey);
			}
		}

		// 2. Restore RetroAPI items into vanilla block entity inventories
		if (chunkData.contains("inventoryItems")) {
			NbtList inventoryItems = chunkData.getList("inventoryItems");
			for (int i = 0; i < inventoryItems.size(); i++) {
				NbtCompound entry = (NbtCompound) inventoryItems.get(i);
				int bx = entry.getInt("x");
				int by = entry.getInt("y");
				int bz = entry.getInt("z");
				int slot = entry.getByte("Slot") & 0xFF;
				String stringId = entry.getString("retroapi:id");
				int count = entry.getByte("Count") & 0xFF;
				int damage = entry.getShort("Damage");

				int numericId = resolveNumericId(stringId);
				if (numericId <= 0) continue;

				int localX = bx & 15;
				int localZ = bz & 15;
				BlockEntity be = chunk.getBlockEntity(localX, by, localZ);
				if (!(be instanceof Inventory inv)) continue;

				ItemStack stack = new ItemStack(numericId, count, damage);
				// Restore data components saved alongside id/count/damage.
				com.periut.retroapi.component.ComponentNbt.read(
					(com.periut.retroapi.component.RetroComponentHolder) (Object) stack, entry);

				// Temporarily null out world to prevent markDirty from triggering
				// recursive chunk loading during restore (b1.8+ markDirty accesses world)
				World beWorld = be.world;
				be.world = null;
				try {
					// Try original slot first
					if (slot < inv.size() && inv.getStack(slot) == null) {
						inv.setStack(slot, stack);
					} else {
						// Find next free slot
						boolean placed = false;
						for (int s = 0; s < inv.size(); s++) {
							if (inv.getStack(s) == null) {
								inv.setStack(s, stack);
								placed = true;
								break;
							}
						}
						if (!placed) {
							LOGGER.warn("No free slot for {} at {},{},{} - discarding", stringId, bx, by, bz);
						}
					}
				} finally {
					be.world = beWorld;
				}
			}
		}

		// 3. Restore item entities by creating them directly (not via NBT deserialization)
		if (chunkData.contains("itemEntities")) {
			NbtList itemEntities = chunkData.getList("itemEntities");
			for (int i = 0; i < itemEntities.size(); i++) {
				NbtCompound entityNbt = (NbtCompound) itemEntities.get(i);

				// Resolve the item from retroapi:id
				if (!entityNbt.contains("Item")) continue;
				NbtCompound itemTag = entityNbt.getCompound("Item");
				if (!itemTag.contains("retroapi:id")) continue;

				int numericId = resolveNumericId(itemTag.getString("retroapi:id"));
				if (numericId <= 0) continue;

				int count = itemTag.contains("retroapi:count")
					? (itemTag.getByte("retroapi:count") & 0xFF)
					: (itemTag.getByte("Count") & 0xFF);
				if (count <= 0) count = 1;
				int damage = itemTag.contains("retroapi:damage")
					? itemTag.getShort("retroapi:damage")
					: itemTag.getShort("Damage");

				ItemStack stack = new ItemStack(numericId, count, damage);
				// Restore data components saved with the dropped item.
				com.periut.retroapi.component.ComponentNbt.read(
					(com.periut.retroapi.component.RetroComponentHolder) (Object) stack, itemTag);

				// Read position from entity NBT
				NbtList posList = entityNbt.getList("Pos");
				double ex = ((net.minecraft.nbt.NbtDouble) posList.get(0)).value;
				double ey = ((net.minecraft.nbt.NbtDouble) posList.get(1)).value;
				double ez = ((net.minecraft.nbt.NbtDouble) posList.get(2)).value;

				ItemEntity itemEntity = new ItemEntity(world, ex, ey, ez, stack);

				// Restore motion if present
				if (entityNbt.contains("Motion")) {
					NbtList motionList = entityNbt.getList("Motion");
					itemEntity.velocityX = ((net.minecraft.nbt.NbtDouble) motionList.get(0)).value;
					itemEntity.velocityY = ((net.minecraft.nbt.NbtDouble) motionList.get(1)).value;
					itemEntity.velocityZ = ((net.minecraft.nbt.NbtDouble) motionList.get(2)).value;
				}

				// Restore age and pickup delay
				if (entityNbt.contains("Age")) {
					itemEntity.itemAge = entityNbt.getShort("Age");
				}

				chunk.addEntity(itemEntity);
			}
		}

		// 4. Restore RetroAPI modded full entities via the vanilla NBT reconstructor.
		//    RetroEntities.register inserted the class into EntityRegistry.idToClass keyed by
		//    id.toString(), so getEntityFromNbt resolves it, runs the (World) ctor, and calls
		//    entity.read(nbt) to restore Pos/Motion/Rotation + subclass state.
		if (chunkData.contains("fullEntities")) {
			NbtList fullEntities = chunkData.getList("fullEntities");
			NbtList deferredEntities = new NbtList();

			for (int i = 0; i < fullEntities.size(); i++) {
				NbtCompound entityNbt = (NbtCompound) fullEntities.get(i);
				String entityId = entityNbt.getString("id");

				// Mod absent this session - keep the data in the sidecar so it restores when the
				// mod returns. Never feed an unknown id to getEntityFromNbt (it would just drop it).
				if (entityId == null || entityId.isEmpty()
						|| RetroRegistry.getEntityByStringId(entityId) == null) {
					deferredEntities.add(entityNbt);
					continue;
				}

				Entity entity = EntityRegistry.getEntityFromNbt(entityNbt, world);
				if (entity == null) {
					// Reconstruction failed (no (World) ctor, ctor threw, or read() failed). Keep it
					// for a future successful load instead of dropping it.
					LOGGER.warn("Failed to reconstruct modded entity {} in chunk {},{} - deferring",
						entityId, chunk.x, chunk.z);
					deferredEntities.add(entityNbt);
					continue;
				}
				chunk.addEntity(entity);
			}

			if (deferredEntities.size() > 0) {
				pendingFullEntities.put(chunkKey, deferredEntities);
			} else {
				pendingFullEntities.remove(chunkKey);
			}
		}
	}

	private static int resolveNumericId(String stringId) {
		String[] parts = stringId.split(":", 2);
		if (parts.length != 2) return -1;

		NamespacedIdentifier retroId = NamespacedIdentifiers.from(parts[0], parts[1]);

		BlockRegistration blockReg = RetroRegistry.getBlockById(retroId);
		if (blockReg != null) return blockReg.getBlock().id;

		ItemRegistration itemReg = RetroRegistry.getItemById(retroId);
		if (itemReg != null) return itemReg.getItem().id;

		return -1;
	}
}
