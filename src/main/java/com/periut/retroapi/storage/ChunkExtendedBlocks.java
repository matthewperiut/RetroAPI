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

	public boolean isDirty() {
		return dirty;
	}

	public void clearDirty() {
		dirty = false;
	}

	public boolean isEmpty() {
		return blockIds.isEmpty() && xmeta.isEmpty();
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
