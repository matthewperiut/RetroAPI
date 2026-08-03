package com.periut.retroapi.storage;

import java.util.HashMap;
import java.util.Map;

public class ChunkExtendedBlocks {
	private final Map<Integer, Integer> blockIds = new HashMap<>();
	private final Map<Integer, Integer> metadata = new HashMap<>();
	/**
	 * Secondary meta (flattened state index bits 4-11) by position, for ANY block at the
	 * position (modded or vanilla-stored); persisted in the sidecar's v3 xmeta section.
	 */
	private final Map<Integer, Integer> xmeta = new HashMap<>();
	/**
	 * Auxiliary per-position data, keyed by data-type id then position; persisted in the
	 * sidecar's v4 data section and carried on the chunk packet. See
	 * {@link com.periut.retroapi.storage.RetroBlockData}.
	 */
	private final Map<String, Map<Integer, Integer>> data = new HashMap<>();
	/**
	 * Cubic biome cells by cellY, persisted in the sidecar's v5 cbio section. A chunk is exactly one
	 * cell wide and one deep, so a 16-cube cell is addressed by its vertical index alone. Absent means
	 * <em>unassigned</em>, which is not a biome and not a default - see
	 * {@link com.periut.retroapi.biome.cubic.RetroCubicBiomes}.
	 */
	private final Map<Integer, Integer> cubicBiomes = new HashMap<>();
	/**
	 * Blocks outside vanilla's 0-127 column, when the dimension is extended; persisted in the sidecar's
	 * v5 vext section. Allocates nothing until something is actually stored outside the column, so a
	 * chunk in an unextended dimension carries only this empty holder. See
	 * {@link com.periut.retroapi.world.height.RetroWorldHeight}.
	 */
	private final com.periut.retroapi.world.height.RetroExtendedSections extendedSections =
		com.periut.retroapi.world.height.RetroExtendedSections.create();
	private boolean dirty = false;

	public boolean hasEntry(int index) {
		return blockIds.containsKey(index);
	}

	public int getBlockId(int index) {
		return blockIds.getOrDefault(index, 0);
	}

	public int getMetadata(int index) {
		return metadata.getOrDefault(index, 0);
	}

	public void set(int index, int blockId, int meta) {
		if (blockId == 0) {
			blockIds.remove(index);
			metadata.remove(index);
		} else {
			blockIds.put(index, blockId);
			if (meta != 0) {
				metadata.put(index, meta);
			} else {
				metadata.remove(index);
			}
		}
		dirty = true;
	}

	public void remove(int index) {
		blockIds.remove(index);
		metadata.remove(index);
		xmeta.remove(index);
		removeAllData(index);
		dirty = true;
	}

	/** Secondary meta (state index bits 4-11) at a position, 0 when unset. */
	public int getXmeta(int index) {
		return xmeta.getOrDefault(index, 0);
	}

	public void setXmeta(int index, int value) {
		if (value == 0) {
			xmeta.remove(index);
		} else {
			xmeta.put(index, value & 0xFF);
		}
		dirty = true;
	}

	public Map<Integer, Integer> getXmetaMap() {
		return xmeta;
	}

	// --- auxiliary per-position data (RetroBlockData) ------------------------------------------

	/** The value stored for a data type at a position, or 0 when nothing is stored. */
	public int getData(String key, int index) {
		Map<Integer, Integer> byPosition = data.get(key);
		return byPosition == null ? 0 : byPosition.getOrDefault(index, 0);
	}

	/** True when a value is present (as opposed to reading 0 because nothing is stored). */
	public boolean hasData(String key, int index) {
		Map<Integer, Integer> byPosition = data.get(key);
		return byPosition != null && byPosition.containsKey(index);
	}

	/**
	 * Stores a value, or removes the entry when it is 0. Zero is the "absent" value on purpose:
	 * the maps are sparse (one entry per position that actually carries data, not one per block
	 * in the chunk), which is what keeps a chunk of ordinary terrain costing nothing.
	 */
	public void setData(String key, int index, int value) {
		if (value == 0) {
			Map<Integer, Integer> byPosition = data.get(key);
			if (byPosition == null || byPosition.remove(index) == null) {
				return;
			}
			if (byPosition.isEmpty()) {
				data.remove(key);
			}
		} else {
			data.computeIfAbsent(key, k -> new HashMap<>()).put(index, value);
		}
		dirty = true;
	}

	/** Drops every data type's value at one position - what a block change at that position means. */
	public void removeAllData(int index) {
		if (data.isEmpty()) {
			return;
		}
		java.util.Iterator<Map.Entry<String, Map<Integer, Integer>>> it = data.entrySet().iterator();
		while (it.hasNext()) {
			Map<Integer, Integer> byPosition = it.next().getValue();
			if (byPosition.remove(index) != null) {
				dirty = true;
				if (byPosition.isEmpty()) {
					it.remove();
				}
			}
		}
	}

	/** The whole data table, keyed by data-type id then position. Read by the sidecar and the chunk packet. */
	public Map<String, Map<Integer, Integer>> getDataMap() {
		return data;
	}

	/** True when any data type has any entry in this chunk. */
	public boolean hasAnyData() {
		return !data.isEmpty();
	}

	// --- cubic biome cells --------------------------------------------------------------------

	/** The runtime biome id for a cell, or 0 when the cell is unassigned. */
	public int getCubicBiome(int cellY) {
		return cubicBiomes.getOrDefault(cellY, 0);
	}

	/** True when the cell has been assigned a biome (as opposed to reading 0 because it has none). */
	public boolean hasCubicBiome(int cellY) {
		return cubicBiomes.containsKey(cellY);
	}

	/**
	 * Assigns a cell, or clears it when {@code biomeId} is 0. Clearing is a real operation, not a
	 * no-op: an unassigned cell must be distinguishable from an assigned one, because the sidecar
	 * writes no section at all for a chunk whose cells are all unassigned.
	 */
	public void setCubicBiome(int cellY, int biomeId) {
		if (biomeId == 0) {
			if (cubicBiomes.remove(cellY) == null) {
				return;
			}
		} else {
			cubicBiomes.put(cellY, biomeId);
		}
		dirty = true;
	}

	/** The whole cell table, keyed by cellY. Read by the sidecar and the sync packet. */
	public Map<Integer, Integer> getCubicBiomeMap() {
		return cubicBiomes;
	}

	/** True when any cell in this chunk carries a biome. */
	public boolean hasAnyCubicBiome() {
		return !cubicBiomes.isEmpty();
	}

	// --- extended (out-of-vanilla-column) blocks ------------------------------------------------

	/** This chunk's blocks above y127 / below y0. Never null; empty unless the dimension is extended. */
	public com.periut.retroapi.world.height.RetroExtendedSections getExtendedSections() {
		return extendedSections;
	}

	public boolean isDirty() {
		return dirty || extendedSections.isDirty();
	}

	public void clearDirty() {
		dirty = false;
		extendedSections.clearDirty();
	}

	public boolean isEmpty() {
		return blockIds.isEmpty() && xmeta.isEmpty() && data.isEmpty() && cubicBiomes.isEmpty()
			&& extendedSections.isEmpty();
	}

	public Map<Integer, Integer> getBlockIds() {
		return blockIds;
	}

	public Map<Integer, Integer> getMetadataMap() {
		return metadata;
	}

	// b1.7.3 chunk layout: 16x128x16, index = x << 11 | z << 7 | y
	public static int toIndex(int x, int y, int z) {
		return (x << 11) | (z << 7) | y;
	}

	public static int indexToX(int index) {
		return (index >> 11) & 0xF;
	}

	public static int indexToY(int index) {
		return index & 0x7F;
	}

	public static int indexToZ(int index) {
		return (index >> 7) & 0xF;
	}
}
