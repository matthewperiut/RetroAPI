package com.periut.retroapi.storage;

import net.ornithemc.osl.core.api.util.NamespacedIdentifier;
import net.ornithemc.osl.core.api.util.NamespacedIdentifiers;
import com.periut.retroapi.registry.BlockRegistration;
import com.periut.retroapi.registry.ItemRegistration;
import com.periut.retroapi.registry.RetroRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.BlockWithBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entities;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
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
			root.putCompound("chunks", new NbtCompound());
			return;
		}
		try (FileInputStream fis = new FileInputStream(file)) {
			root = NbtIo.readCompressed(fis);
		} catch (IOException e) {
			LOGGER.error("Failed to load inventory sidecar {}", file, e);
			root = new NbtCompound();
			root.putInt("version", VERSION);
			root.putCompound("chunks", new NbtCompound());
		}
	}

	public void save() {
		if (!dirty) return;
		file.getParentFile().mkdirs();
		try (FileOutputStream fos = new FileOutputStream(file)) {
			NbtIo.writeCompressed(root, fos);
			dirty = false;
		} catch (IOException e) {
			LOGGER.error("Failed to save inventory sidecar {}", file, e);
		}
	}

	/**
	 * Filter the chunk NBT to remove all modded content:
	 * - Modded block entities (at positions with extended blocks) → saved to sidecar
	 * - RetroAPI items in vanilla block entity inventories → saved to sidecar
	 * - Item entities carrying RetroAPI items → saved to sidecar
	 */
	public void filterAndSave(int chunkX, int chunkZ, NbtCompound nbt, ChunkExtendedBlocks extended) {
		if (!root.contains("chunks")) {
			root.putCompound("chunks", new NbtCompound());
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
					moddedBEs.addElement(be);
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
								inventoryItems.addElement(invEntry);
								hasModdedItems = true;
							} else {
								filteredItems.addElement(item);
							}
						}

						if (hasModdedItems) {
							be.put("Items", filteredItems);
						}
					}
					filteredTEs.addElement(be);
				}
			}

			nbt.put("TileEntities", filteredTEs);
			// Merge in any pending (deferred) block entities that couldn't be restored
			NbtList pending = pendingBlockEntities.remove(chunkKey);
			if (pending != null) {
				for (int i = 0; i < pending.size(); i++) {
					moddedBEs.addElement(pending.get(i));
				}
			}
			if (moddedBEs.size() > 0) {
				chunkData.put("blockEntities", moddedBEs);
			}
			if (inventoryItems.size() > 0) {
				chunkData.put("inventoryItems", inventoryItems);
			}
		}

		// 2. Filter Entities - remove item entities carrying RetroAPI items
		NbtList entities = nbt.getList("Entities");
		if (entities != null) {
			NbtList filteredEntities = new NbtList();
			NbtList moddedEntities = new NbtList();

			for (int i = 0; i < entities.size(); i++) {
				NbtCompound entity = (NbtCompound) entities.get(i);
				if ("Item".equals(entity.getString("id"))) {
					NbtCompound itemTag = entity.getCompound("Item");
					if (itemTag != null && itemTag.contains("retroapi:id")) {
						moddedEntities.addElement(entity);
						continue;
					}
				}
				filteredEntities.addElement(entity);
			}

			nbt.put("Entities", filteredEntities);
			if (moddedEntities.size() > 0) {
				chunkData.put("itemEntities", moddedEntities);
			}
		}

		chunks.putCompound(chunkKey, chunkData);
		dirty = true;
	}

	/**
	 * Restore modded content from sidecar into a loaded chunk:
	 * - Modded block entities (with block placement verification)
	 * - RetroAPI items in vanilla block entity inventories
	 * - Item entities carrying RetroAPI items
	 */
	public void restoreChunkContent(WorldChunk chunk, World world) {
		if (!root.contains("chunks")) return;
		NbtCompound chunks = root.getCompound("chunks");
		String chunkKey = chunk.chunkX + "," + chunk.chunkZ;

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
					deferredBEs.addElement(beNbt);
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
					deferredBEs.addElement(beNbt);
					continue;
				}

				// Verify the block has HAS_BLOCK_ENTITY set
				int blockId = extended.getBlockId(index);
				if (blockId > 0 && blockId < Block.BY_ID.length && Block.BY_ID[blockId] != null) {
					Block.HAS_BLOCK_ENTITY[blockId] = true;
				}

				BlockEntity be = BlockEntity.fromNbt(beNbt);
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
				BlockEntity be = chunk.getBlockEntityAt(localX, by, localZ);
				if (!(be instanceof Inventory inv)) continue;

				ItemStack stack = new ItemStack(numericId, count, damage);

				// Temporarily null out world to prevent markDirty from triggering
				// recursive chunk loading during restore (b1.8+ markDirty accesses world)
				World beWorld = be.world;
				be.world = null;
				try {
					// Try original slot first
					if (slot < inv.getSize() && inv.getItem(slot) == null) {
						inv.setItem(slot, stack);
					} else {
						// Find next free slot
						boolean placed = false;
						for (int s = 0; s < inv.getSize(); s++) {
							if (inv.getItem(s) == null) {
								inv.setItem(s, stack);
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
					itemEntity.age = entityNbt.getShort("Age");
				}

				chunk.addEntity(itemEntity);
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
