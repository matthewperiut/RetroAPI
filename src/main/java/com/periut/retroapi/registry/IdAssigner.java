package com.periut.retroapi.registry;

import com.periut.retroapi.dimension.DimensionRegistration;
import com.periut.retroapi.dimension.RetroDimensionRegistry;
import com.periut.retroapi.mixin.register.FireBlockAccessor;
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
		assignDimensionIds(idMap);

		idMap.save(idMapFile);
	}

	public static void saveCurrentIds(File worldDir) {
		File idMapFile = new File(worldDir, "retroapi/id_map.dat");
		// Load first so the existing (stable) dimension serial ids are preserved across reopen - the
		// DIM<n>/ folders and player-NBT ids depend on them. Block/item ids are then refreshed from the
		// current (StationAPI-managed) registry state.
		IdMap idMap = new IdMap();
		idMap.load(idMapFile);

		for (BlockRegistration reg : RetroRegistry.getBlocks()) {
			idMap.putBlockId(reg.getId(), reg.getBlock().id);
		}
		for (ItemRegistration reg : RetroRegistry.getItems()) {
			idMap.putItemId(reg.getId(), reg.getItem().id);
		}
		assignDimensionIds(idMap);

		idMap.save(idMapFile);
		LOGGER.info("Saved current ID map for {} blocks, {} items and {} dimensions",
			RetroRegistry.getBlocks().size(), RetroRegistry.getItems().size(), RetroDimensionRegistry.getAll().size());
	}

	/**
	 * Assign each registered modded dimension a STABLE serial id (persisted in the id_map "dimensions"
	 * section, reused across reopen) and write it back onto the {@link DimensionRegistration}. Serial
	 * ids start at 2 (vanilla -1/0/1 reserved). Stale (mod-removed) entries are purged. This runs in
	 * both the StationAPI and non-StationAPI save paths so the dimension identifier->id map always
	 * exists on disk for datafixer world conversions.
	 */
	private static void assignDimensionIds(IdMap idMap) {
		java.util.List<DimensionRegistration> dimensions = RetroDimensionRegistry.getAll();
		if (dimensions.isEmpty()) return;

		Set<NamespacedIdentifier> active = new HashSet<>();
		for (DimensionRegistration reg : dimensions) {
			active.add(reg.getId());
		}
		idMap.getDimensionIds().keySet().removeIf(id -> !active.contains(id));

		Set<Integer> used = new HashSet<>(idMap.getDimensionIds().values());
		for (DimensionRegistration reg : dimensions) {
			Integer existing = idMap.getDimensionId(reg.getId());
			int serialId;
			if (existing != null) {
				serialId = existing;
			} else {
				serialId = findFreeDimensionId(used);
				used.add(serialId);
				idMap.putDimensionId(reg.getId(), serialId);
			}
			reg.setSerialId(serialId);
		}
	}

	private static int findFreeDimensionId(Set<Integer> used) {
		int id = 2;
		while (used.contains(id)) id++;
		return id;
	}

	private static void assignBlockIds(IdMap idMap) {
		Block[] byId = Block.BLOCKS;
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
		Item[] byId = Item.ITEMS;
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
		Item[] itemById = Item.ITEMS;
		// Start at 256 to keep all RetroAPI blocks in extended storage range
		for (int i = 256; i < Block.BLOCKS.length; i++) {
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
		if (minSize < Block.BLOCKS.length) return;
		int newSize = Block.BLOCKS.length;
		while (newSize <= minSize) newSize *= 2;
		Block.BLOCKS = Arrays.copyOf(Block.BLOCKS, newSize);
		Block.BLOCKS_OPAQUE = Arrays.copyOf(Block.BLOCKS_OPAQUE, newSize);
		Block.BLOCKS_LIGHT_OPACITY = Arrays.copyOf(Block.BLOCKS_LIGHT_OPACITY, newSize);
		Block.BLOCKS_ALLOW_VISION = Arrays.copyOf(Block.BLOCKS_ALLOW_VISION, newSize);
		Block.BLOCKS_WITH_ENTITY = Arrays.copyOf(Block.BLOCKS_WITH_ENTITY, newSize);
		Block.BLOCKS_RANDOM_TICK = Arrays.copyOf(Block.BLOCKS_RANDOM_TICK, newSize);
		Block.BLOCKS_LIGHT_LUMINANCE = Arrays.copyOf(Block.BLOCKS_LIGHT_LUMINANCE, newSize);
		Block.BLOCKS_IGNORE_META_UPDATE = Arrays.copyOf(Block.BLOCKS_IGNORE_META_UPDATE, newSize);
		LOGGER.info("Grew block arrays to size {}", newSize);
	}

	private static void remapBlock(BlockRegistration reg, int oldId, int newId) {
		Block block = reg.getBlock();
		growBlockArraysIfNeeded(newId);
		Block[] byId = Block.BLOCKS;

		// Save values before clearing
		int lightValue = (oldId >= 0 && oldId < Block.BLOCKS_LIGHT_LUMINANCE.length) ? Block.BLOCKS_LIGHT_LUMINANCE[oldId] : 0;
		boolean hasBlockEntity = (oldId >= 0 && oldId < Block.BLOCKS_WITH_ENTITY.length) && Block.BLOCKS_WITH_ENTITY[oldId];
		boolean ticksRandomly = (oldId >= 0 && oldId < Block.BLOCKS_RANDOM_TICK.length) && Block.BLOCKS_RANDOM_TICK[oldId];
		int[] burnChances = ((FireBlockAccessor) Block.FIRE).retroapi$getBurnChances();
		int[] spreadChances = ((FireBlockAccessor) Block.FIRE).retroapi$getSpreadChances();
		int burnChance = (oldId >= 0 && oldId < burnChances.length) ? burnChances[oldId] : 0;
		int spreadChance = (oldId >= 0 && oldId < spreadChances.length) ? spreadChances[oldId] : 0;

		// Clear old slot only if it's actually this block
		if (oldId >= 0 && oldId < byId.length && byId[oldId] == block) {
			byId[oldId] = null;
			Block.BLOCKS_OPAQUE[oldId] = false;
			Block.BLOCKS_LIGHT_OPACITY[oldId] = 0;
			Block.BLOCKS_ALLOW_VISION[oldId] = false;
			Block.BLOCKS_WITH_ENTITY[oldId] = false;
			Block.BLOCKS_RANDOM_TICK[oldId] = false;
			Block.BLOCKS_LIGHT_LUMINANCE[oldId] = 0;
			Block.BLOCKS_IGNORE_META_UPDATE[oldId] = false;
			if (oldId < burnChances.length) burnChances[oldId] = 0;
			if (oldId < spreadChances.length) spreadChances[oldId] = 0;
		}

		// Set new slot
		byId[newId] = block;
		Block.BLOCKS_OPAQUE[newId] = block.isOpaque();
		Block.BLOCKS_LIGHT_OPACITY[newId] = block.isOpaque() ? 255 : 0;
		Block.BLOCKS_ALLOW_VISION[newId] = !block.material.blocksVision();
		Block.BLOCKS_WITH_ENTITY[newId] = hasBlockEntity;
		Block.BLOCKS_RANDOM_TICK[newId] = ticksRandomly;
		Block.BLOCKS_LIGHT_LUMINANCE[newId] = lightValue;
		// Fire flammability registered via RetroFlammability follows the block to its new id
		if (newId < burnChances.length) burnChances[newId] = burnChance;
		if (newId < spreadChances.length) spreadChances[newId] = spreadChance;

		// Update the block's id field
		block.id = newId;

		// Remap the corresponding BlockItem using the registration's stored reference
		BlockItem blockItem = reg.getBlockItem();
		if (blockItem != null) {
			// Clear old Item.BY_ID slot only if it still points to this block's item
			Item[] itemById = Item.ITEMS;
			if (oldId >= 0 && oldId < itemById.length && itemById[oldId] == blockItem) {
				itemById[oldId] = null;
			}
			itemById[newId] = blockItem;
			blockItem.id = newId;
			blockItem.blockId = newId;
		}

		LOGGER.debug("Remapped block {} from {} to {}", reg.getId(), oldId, newId);
	}

	private static void remapItem(Item item, int oldId, int newId) {
		Item[] byId = Item.ITEMS;

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

