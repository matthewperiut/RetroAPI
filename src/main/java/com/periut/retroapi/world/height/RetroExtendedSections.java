package com.periut.retroapi.world.height;

import com.periut.retroapi.storage.ChunkExtendedBlocks;
import com.periut.retroapi.storage.ExtendedBlocksAccess;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A chunk's blocks outside vanilla's 0-127 column: dense 16x16x16 sections keyed by section index, held
 * on the chunk and persisted in the sidecar's {@code vext} section.
 *
 * <p><b>Dense, not sparse</b>, and that is the opposite choice from every other thing RetroAPI attaches
 * to a chunk. The extended-block overlay, the xmeta map and the block-data maps are all sparse because
 * they hold exceptions - a few hundred modded blocks in a chunk of vanilla terrain. An extension section
 * holds <em>terrain</em>: 4096 cells, nearly all of them stone. A sparse map of 4096 boxed entries would
 * cost roughly 50x a flat array and be slower to walk, and the gzip the sidecar already applies squashes
 * a stone-filled array to almost nothing anyway. Sections that are entirely air are never allocated and
 * never written, so a world with no extension content still costs zero.
 *
 * <p>Block ids are stored as 16-bit values so modded ids (&ge;256) live in the same array as vanilla ones
 * - there is no vanilla byte array here to be limited by, so the split RetroAPI needs at y0-127 simply
 * does not apply above or below it.
 */
public final class RetroExtendedSections {

	public static final int SECTION_SIZE = 16 * 16 * 16;

	/** Section index -> that section's blocks. Absent means "entirely air". */
	private final Map<Integer, Section> sections = new LinkedHashMap<>();
	private boolean dirty;

	/** One 16-cube of extension terrain. */
	public static final class Section {
		/** Block ids, 0 = air. Indexed {@link #index}. */
		final short[] ids = new short[SECTION_SIZE];
		/** Metadata nibbles, one byte per cell for simplicity (the array is small and gzips away). */
		final byte[] meta = new byte[SECTION_SIZE];

		public short[] ids() {
			return ids;
		}

		public byte[] meta() {
			return meta;
		}

		boolean isEmpty() {
			for (short id : ids) {
				if (id != 0) {
					return false;
				}
			}
			return true;
		}
	}

	/** Cell index inside a section. Same bit order as the vanilla chunk layout, one section tall. */
	public static int index(int localX, int localY, int localZ) {
		return localX << 8 | localZ << 4 | localY;
	}

	/** The section index a world Y belongs to. Floor division, so correct for negative Y. */
	public static int sectionOf(int y) {
		return y >> 4;
	}

	// --- access -------------------------------------------------------------------------------

	/** The extension sections of the chunk containing a position, or null when it is not loaded. */
	public static RetroExtendedSections of(BlockView view, int x, int z) {
		World world = com.periut.retroapi.world.RetroWorlds.unwrap(view);
		if (world == null) {
			return null;
		}
		Chunk chunk = world.getChunk(x >> 4, z >> 4);
		return chunk == null ? null : of(chunk);
	}

	/** The extension sections attached to a chunk. Never null - the holder is created with the chunk. */
	public static RetroExtendedSections of(Chunk chunk) {
		return ((ExtendedBlocksAccess) chunk).retroapi$getExtendedBlocks().getExtendedSections();
	}

	/** The block id at a chunk-local X/Z and world Y, or 0 when nothing is stored there. */
	public int getBlock(int localX, int y, int localZ) {
		Section section = sections.get(sectionOf(y));
		return section == null ? 0 : section.ids[index(localX, y & 15, localZ)] & 0xFFFF;
	}

	/** The metadata at a chunk-local X/Z and world Y. */
	public int getMeta(int localX, int y, int localZ) {
		Section section = sections.get(sectionOf(y));
		return section == null ? 0 : section.meta[index(localX, y & 15, localZ)] & 0xF;
	}

	/**
	 * Stores a block. Writing air into a section that does not exist is free and allocates nothing,
	 * which is what keeps an untouched extension range costing zero.
	 */
	public void setBlock(int localX, int y, int localZ, int blockId, int meta) {
		int sectionIndex = sectionOf(y);
		Section section = sections.get(sectionIndex);
		if (section == null) {
			if (blockId == 0) {
				return;
			}
			section = new Section();
			sections.put(sectionIndex, section);
		}
		int cell = index(localX, y & 15, localZ);
		section.ids[cell] = (short) blockId;
		section.meta[cell] = (byte) (meta & 0xF);
		dirty = true;
	}

	/** The section for an index, creating it if needed. For bulk generation writes. */
	public Section getOrCreateSection(int sectionIndex) {
		Section section = sections.computeIfAbsent(sectionIndex, k -> new Section());
		dirty = true;
		return section;
	}

	/** The section for an index, or null when it holds nothing. */
	public Section getSection(int sectionIndex) {
		return sections.get(sectionIndex);
	}

	/** Every stored section, keyed by section index. Read by the sidecar. */
	public Map<Integer, Section> getSections() {
		return sections;
	}

	/** Drops sections that ended up entirely air, so they are never written. */
	public void prune() {
		sections.entrySet().removeIf(entry -> entry.getValue().isEmpty());
	}

	public boolean isEmpty() {
		return sections.isEmpty();
	}

	public boolean isDirty() {
		return dirty;
	}

	public void clearDirty() {
		dirty = false;
	}

	/** Used by the sidecar loader to fill a section wholesale. */
	public void putSection(int sectionIndex, short[] ids, byte[] meta) {
		Section section = getOrCreateSection(sectionIndex);
		System.arraycopy(ids, 0, section.ids, 0, Math.min(ids.length, SECTION_SIZE));
		System.arraycopy(meta, 0, section.meta, 0, Math.min(meta.length, SECTION_SIZE));
	}

	/** Convenience for {@link ChunkExtendedBlocks}, which owns the instance. */
	public static RetroExtendedSections create() {
		return new RetroExtendedSections();
	}
}
