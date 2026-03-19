package com.periut.retroapi.registry;

import net.ornithemc.osl.core.api.util.NamespacedIdentifier;
import net.ornithemc.osl.core.api.util.NamespacedIdentifiers;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.ornithemc.osl.networking.api.PacketBuffer;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class IdAssigner {
	private static final Logger LOGGER = LogManager.getLogger("RetroAPI/IdAssigner");

	public static void assignIds(File worldDir) {
		File idMapFile = new File(worldDir, "retroapi/id_map.dat");
		IdMap idMap = new IdMap();
		idMap.load(idMapFile);

		// Purge stale entries for blocks/items no longer registered
		idMap.purgeStaleEntries(RetroRegistry.getBlocks(), RetroRegistry.getItems());

		assignBlockIds(idMap);
		assignItemIds(idMap);

		idMap.save(idMapFile);
	}

#if MC_B1_6_OR_LATER
	public static void saveCurrentIds(File worldDir) {
		File idMapFile = new File(worldDir, "retroapi/id_map.dat");
		IdMap idMap = new IdMap();

		for (BlockRegistration reg : RetroRegistry.getBlocks()) {
			idMap.putBlockId(reg.getId(), reg.getBlock().id);
		}
		for (ItemRegistration reg : RetroRegistry.getItems()) {
			idMap.putItemId(reg.getId(), reg.getItem().id);
		}

		idMap.save(idMapFile);
		LOGGER.info("Saved current ID map for {} blocks and {} items",
			RetroRegistry.getBlocks().size(), RetroRegistry.getItems().size());
	}
#endif

	private static void assignBlockIds(IdMap idMap) {
		Block[] byId = Block.BY_ID;
		Set<Integer> usedIds = new HashSet<>();

		// Collect all currently used vanilla block IDs
		for (int i = 0; i < byId.length; i++) {
			if (byId[i] != null) {
				boolean isRetroBlock = false;
				for (BlockRegistration reg : RetroRegistry.getBlocks()) {
					if (reg.getBlock() == byId[i]) {
						isRetroBlock = true;
						break;
					}
				}
				if (!isRetroBlock) {
					usedIds.add(i);
				}
			}
		}

		// Also mark IDs already assigned in the map as used
		for (int id : idMap.getBlockIds().values()) {
			usedIds.add(id);
		}

		for (BlockRegistration reg : RetroRegistry.getBlocks()) {
			Block block = reg.getBlock();
			int currentId = block.id;
			Integer mappedId = idMap.getBlockId(reg.getId());

			int targetId;
			if (mappedId != null) {
				targetId = mappedId;
			} else {
				// Assign new ID
				targetId = findFreeBlockId(usedIds);
				idMap.putBlockId(reg.getId(), targetId);
				usedIds.add(targetId);
			}

			if (currentId != targetId) {
				remapBlock(reg, currentId, targetId);
			}
		}
	}

	private static void assignItemIds(IdMap idMap) {
		Item[] byId = Item.BY_ID;
		Set<Integer> usedIds = new HashSet<>();

		// Collect all currently used vanilla item IDs (raw, including the +256 offset)
		for (int i = 256; i < byId.length; i++) {
			if (byId[i] != null) {
				boolean isRetroItem = false;
				for (ItemRegistration reg : RetroRegistry.getItems()) {
					if (reg.getItem() == byId[i]) {
						isRetroItem = true;
						break;
					}
				}
				if (!isRetroItem) {
					usedIds.add(i);
				}
			}
		}

		for (int id : idMap.getItemIds().values()) {
			usedIds.add(id);
		}

		for (ItemRegistration reg : RetroRegistry.getItems()) {
			Item item = reg.getItem();
			int currentId = item.id;
			Integer mappedId = idMap.getItemId(reg.getId());

			int targetId;
			if (mappedId != null) {
				targetId = mappedId;
			} else {
				targetId = findFreeItemId(usedIds);
				idMap.putItemId(reg.getId(), targetId);
				usedIds.add(targetId);
			}

			if (currentId != targetId) {
				remapItem(item, currentId, targetId);
			}

			LOGGER.info("Assigned item {} -> ID {}", reg.getId(), targetId);
		}
	}

	private static int findFreeBlockId(Set<Integer> usedIds) {
		Item[] itemById = Item.BY_ID;
		// Start at 256 to keep all RetroAPI blocks in extended storage range
		for (int i = 256; i < Block.BY_ID.length; i++) {
			if (!usedIds.contains(i) && (i >= itemById.length || itemById[i] == null || itemById[i] instanceof BlockItem)) {
				return i;
			}
		}
		throw new RuntimeException("No free block IDs available");
	}

	private static int findFreeItemId(Set<Integer> usedIds) {
		// Item IDs stored with +256 offset
		for (int i = 2256; i < 32000; i++) {
			if (!usedIds.contains(i)) {
				return i;
			}
		}
		for (int i = 256; i < 2256; i++) {
			if (!usedIds.contains(i)) {
				return i;
			}
		}
		throw new RuntimeException("No free item IDs available");
	}

	public static void growBlockArraysIfNeeded(int minSize) {
		if (minSize < Block.BY_ID.length) return;
		int newSize = Block.BY_ID.length;
		while (newSize <= minSize) newSize *= 2;
		Block.BY_ID = Arrays.copyOf(Block.BY_ID, newSize);
#if MC_B1_6_OR_LATER
		Block.IS_SOLID_RENDER = Arrays.copyOf(Block.IS_SOLID_RENDER, newSize);
#else
		Block.IS_SOLID = Arrays.copyOf(Block.IS_SOLID, newSize);
#endif
		Block.OPACITIES = Arrays.copyOf(Block.OPACITIES, newSize);
		Block.IS_TRANSLUCENT = Arrays.copyOf(Block.IS_TRANSLUCENT, newSize);
		Block.HAS_BLOCK_ENTITY = Arrays.copyOf(Block.HAS_BLOCK_ENTITY, newSize);
		Block.TICKS_RANDOMLY = Arrays.copyOf(Block.TICKS_RANDOMLY, newSize);
		Block.LIGHT = Arrays.copyOf(Block.LIGHT, newSize);
#if MC_HAS_UPDATE_CLIENTS
		Block.UPDATE_CLIENTS = Arrays.copyOf(Block.UPDATE_CLIENTS, newSize);
#endif
		LOGGER.info("Grew block arrays to size {}", newSize);
	}

	private static void remapBlock(BlockRegistration reg, int oldId, int newId) {
		Block block = reg.getBlock();
		growBlockArraysIfNeeded(newId);
		Block[] byId = Block.BY_ID;

		// Save values before clearing
		int lightValue = (oldId >= 0 && oldId < Block.LIGHT.length) ? Block.LIGHT[oldId] : 0;
		boolean hasBlockEntity = (oldId >= 0 && oldId < Block.HAS_BLOCK_ENTITY.length) && Block.HAS_BLOCK_ENTITY[oldId];
		boolean ticksRandomly = (oldId >= 0 && oldId < Block.TICKS_RANDOMLY.length) && Block.TICKS_RANDOMLY[oldId];

		// Clear old slot only if it's actually this block
		if (oldId >= 0 && oldId < byId.length && byId[oldId] == block) {
			byId[oldId] = null;
#if MC_B1_6_OR_LATER
			Block.IS_SOLID_RENDER[oldId] = false;
#else
			Block.IS_SOLID[oldId] = false;
#endif
			Block.OPACITIES[oldId] = 0;
			Block.IS_TRANSLUCENT[oldId] = false;
			Block.HAS_BLOCK_ENTITY[oldId] = false;
			Block.TICKS_RANDOMLY[oldId] = false;
			Block.LIGHT[oldId] = 0;
#if MC_HAS_UPDATE_CLIENTS
			Block.UPDATE_CLIENTS[oldId] = false;
#endif
		}

		// Set new slot
		byId[newId] = block;
#if MC_B1_6_OR_LATER
		Block.IS_SOLID_RENDER[newId] = block.isSolidRender();
		Block.OPACITIES[newId] = block.isSolidRender() ? 255 : 0;
#else
		Block.IS_SOLID[newId] = block.isSolid();
		Block.OPACITIES[newId] = block.isSolid() ? 255 : 0;
#endif
		Block.IS_TRANSLUCENT[newId] = !block.material.isOpaque();
		Block.HAS_BLOCK_ENTITY[newId] = hasBlockEntity;
		Block.TICKS_RANDOMLY[newId] = ticksRandomly;
		Block.LIGHT[newId] = lightValue;

		// Update the block's id field
		block.id = newId;

		// Remap the corresponding BlockItem using the registration's stored reference
		BlockItem blockItem = reg.getBlockItem();
		if (blockItem != null) {
			// Clear old Item.BY_ID slot only if it still points to this block's item
			Item[] itemById = Item.BY_ID;
			if (oldId >= 0 && oldId < itemById.length && itemById[oldId] == blockItem) {
				itemById[oldId] = null;
			}
			itemById[newId] = blockItem;
			blockItem.id = newId;
			blockItem.block = newId;
		}

		LOGGER.debug("Remapped block {} from {} to {}", reg.getId(), oldId, newId);
	}

	private static void remapItem(Item item, int oldId, int newId) {
		Item[] byId = Item.BY_ID;

		// Clear old slot
		if (oldId >= 0 && oldId < byId.length && byId[oldId] == item) {
			byId[oldId] = null;
		}

		// Set new slot
		byId[newId] = item;

		// Update the item's id field
		item.id = newId;

		LOGGER.debug("Remapped item from {} to {}", oldId, newId);
	}

	public static void applyFromNetwork(PacketBuffer buffer) {
		int blockCount = buffer.readVarInt();
		for (int i = 0; i < blockCount; i++) {
			String identifier = buffer.readString();
			int numericId = buffer.readVarInt();

			String[] parts = identifier.split(":", 2);
			if (parts.length != 2) {
				LOGGER.warn("Invalid block identifier from server: {}", identifier);
				continue;
			}
			NamespacedIdentifier retroId = NamespacedIdentifiers.from(parts[0], parts[1]);
			BlockRegistration reg = RetroRegistry.getBlockById(retroId);
			if (reg == null) {
				LOGGER.warn("Server has unknown block: {} (id {}), skipping", identifier, numericId);
				continue;
			}

			Block block = reg.getBlock();
			int currentId = block.id;
			growBlockArraysIfNeeded(numericId);
			if (currentId != numericId) {
				remapBlock(reg, currentId, numericId);
				LOGGER.info("Synced block {} -> ID {} (was {})", identifier, numericId, currentId);
			}
		}

		int itemCount = buffer.readVarInt();
		for (int i = 0; i < itemCount; i++) {
			String identifier = buffer.readString();
			int numericId = buffer.readVarInt();

			String[] parts = identifier.split(":", 2);
			if (parts.length != 2) {
				LOGGER.warn("Invalid item identifier from server: {}", identifier);
				continue;
			}
			NamespacedIdentifier retroId = NamespacedIdentifiers.from(parts[0], parts[1]);
			ItemRegistration reg = RetroRegistry.getItemById(retroId);
			if (reg == null) {
				LOGGER.warn("Server has unknown item: {} (id {}), skipping", identifier, numericId);
				continue;
			}

			Item item = reg.getItem();
			int currentId = item.id;
			if (currentId != numericId) {
				remapItem(item, currentId, numericId);
				LOGGER.info("Synced item {} -> ID {} (was {})", identifier, numericId, currentId);
			}
		}
	}
}
