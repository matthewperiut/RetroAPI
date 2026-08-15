package com.periut.retroapi.storage;

import net.ornithemc.osl.core.api.util.NamespacedIdentifier;
import net.ornithemc.osl.core.api.util.NamespacedIdentifiers;
import com.periut.retroapi.registry.BlockRegistration;
import com.periut.retroapi.registry.RetroRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.chunk.Chunk;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RegionSidecar {
	private static final Logger LOGGER = LogManager.getLogger("RetroAPI/RegionSidecar");
	private static final int VERSION = 1;

	private final File file;
	private NbtCompound root;
	private boolean dirty = false;

	/**
	 * Modded entries that could not be restored at load time: a vanilla session placed a REAL block
	 * at the (invisible) position - the vanilla block wins while it is there - or the block's mod is
	 * currently missing. Kept per chunk and merged back into the chunk's sidecar entry at save so a
	 * save never drops them; re-checked on every load (restored once the position is air again /
	 * the mod returns). Instance field - RegionSidecar instances are per-world (dropped by
	 * SidecarManager.flush), so deferrals cannot leak across worlds.
	 */
	private final Map<String, List<Deferred>> deferred = new HashMap<>();

	/**
	 * The same story for {@link RetroBlockData} block references: a stored reference names its block
	 * by string id, and the mod that owns that block may be absent this session. Dropping the entry
	 * would silently un-clad half a build the first time someone launches without an optional mod, so
	 * unresolvable references are parked here and written back out on save.
	 */
	private final Map<String, List<DeferredData>> deferredData = new HashMap<>();

	private static final class Deferred {
		final int position;
		final String stringId;
		final int meta;

		Deferred(int position, String stringId, int meta) {
			this.position = position;
			this.stringId = stringId;
			this.meta = meta;
		}
	}

	private static final class DeferredData {
		final String dataKey;
		final int position;
		final String stringId;
		final int meta;

		DeferredData(String dataKey, int position, String stringId, int meta) {
			this.dataKey = dataKey;
			this.position = position;
			this.stringId = stringId;
			this.meta = meta;
		}
	}

	/**
	 * And the same story again for cubic biome cells: a cell names its biome by string id, and the mod
	 * that registered that biome may be absent this session. Dropping the cell would silently repaint a
	 * cavern the first time someone launches without an optional cave-biome add-on, so unresolvable
	 * cells are parked here and written back out on save.
	 */
	private final Map<String, List<DeferredCell>> deferredCells = new HashMap<>();

	private static final class DeferredCell {
		final int cellY;
		final String biomeId;

		DeferredCell(int cellY, String biomeId) {
			this.cellY = cellY;
			this.biomeId = biomeId;
		}
	}

	public RegionSidecar(File file) {
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
			LOGGER.error("Failed to load region sidecar {}", file, e);
			root = new NbtCompound();
			root.putInt("version", VERSION);
			root.put("chunks", new NbtCompound());
		}
	}

	/**
	 * A chunk's arbitrary mod data, live: write into it and it is saved with the region.
	 *
	 * <p>Deliberately its own top-level section rather than a corner of the chunk's entry under
	 * {@code chunks}. That entry is rebuilt from scratch every time the chunk saves - from the live
	 * extended-block map, and replaced wholesale with an empty compound for a chunk that has no modded
	 * blocks - so anything parked inside it would be dropped by the next save of an ordinary chunk.
	 * Here nothing else writes, and the region file carries it.
	 *
	 * <p>Marks the region dirty on the way out, because a caller writing into the returned compound has
	 * no way to say so afterwards. The cost is a region file rewritten after a read that changed
	 * nothing; the alternative is data that silently does not save.
	 */
	public NbtCompound customChunkData(int chunkX, int chunkZ) {
		if (!root.contains("custom")) {
			root.put("custom", new NbtCompound());
		}
		NbtCompound custom = root.getCompound("custom");
		String key = chunkX + "," + chunkZ;

		if (!custom.contains(key)) {
			custom.put(key, new NbtCompound());
		}
		dirty = true;
		return custom.getCompound(key);
	}

	public void save() {
		if (!dirty) return;
		try {
			// Serialize on this (main) thread - the tree keeps being mutated by chunk saves -
			// then hand the immutable snapshot to the background gzip+atomic-write thread.
			byte[] payload = SidecarIo.snapshot(root);
			dirty = false;
			SidecarIo.writeAsync(file, payload);
		} catch (IOException e) {
			LOGGER.error("Failed to snapshot region sidecar {}", file, e);
		}
	}

	public void loadChunkData(Chunk chunk, ChunkExtendedBlocks extended) {
		NbtCompound chunkNbt = loadAuxSections(chunk, extended);
		if (chunkNbt == null) {
			return;
		}
		loadBlockIds(chunk, extended, chunkNbt);
	}

	/**
	 * Everything a position carries that is not a block id: secondary meta, {@link RetroBlockData}, cubic
	 * biome cells, out-of-column terrain.
	 *
	 * <p>All of it under StationAPI, which owns block ids and stores them in its own chunk sections - so
	 * the id restore below has nothing to do there, and no vanilla byte array to read while doing it.
	 */
	public void loadChunkAuxData(Chunk chunk, ChunkExtendedBlocks extended) {
		loadAuxSections(chunk, extended);
	}

	/** @return the chunk's saved NBT, or null when it has none. */
	private NbtCompound loadAuxSections(Chunk chunk, ChunkExtendedBlocks extended) {
		int chunkX = chunk.x;
		int chunkZ = chunk.z;
		String key = chunkX + "," + chunkZ;
		// Rebuild this chunk's deferral lists from scratch on every load (re-check semantics).
		deferred.remove(key);
		deferredData.remove(key);
		deferredCells.remove(key);

		if (!root.contains("chunks")) return null;
		NbtCompound chunks = root.getCompound("chunks");

		if (!chunks.contains(key)) return null;
		NbtCompound chunkNbt = chunks.getCompound(key);

		// v3: secondary meta (state index bits 4-11), for modded AND vanilla-stored positions.
		// Loaded before the modded-position check below: a chunk may carry ONLY xmeta.
		byte[] xpos = chunkNbt.getByteArray("xpos");
		byte[] xval = chunkNbt.getByteArray("xval");
		if (xpos.length > 0 && xval.length * 4 == xpos.length) {
			int[] xPositions = bytesToInts(xpos);
			for (int i = 0; i < xPositions.length; i++) {
				extended.setXmeta(xPositions[i], xval[i] & 0xFF);
			}
		}

		// v4: auxiliary per-position data (RetroBlockData). Also loaded before the modded-position
		// check below - a chunk may carry data for vanilla-stored blocks and nothing else.
		loadBlockData(key, chunkNbt, extended);

		// v5: cubic biome cells. Also before the modded-position check - a chunk of ordinary vanilla
		// stone can perfectly well carry cave biome cells and no modded blocks at all.
		loadCubicBiomes(key, chunkNbt, extended);

		// v5: out-of-vanilla-column terrain (RetroWorldHeight). Same story again.
		loadExtendedSections(chunkNbt, extended);

		return chunkNbt;
	}

	/** The modded block ids themselves, restored into the chunk's vanilla arrays. */
	private void loadBlockIds(Chunk chunk, ChunkExtendedBlocks extended, NbtCompound chunkNbt) {
		int chunkX = chunk.x;
		int chunkZ = chunk.z;
		String key = chunkX + "," + chunkZ;

		// Positions are encoded as byte array (4 bytes per int, big-endian)
		byte[] posBytes = chunkNbt.getByteArray("positions");
		byte[] metadata = chunkNbt.getByteArray("metadata");

		if (posBytes.length == 0) return;
		int[] positions = bytesToInts(posBytes);

		String[] ids;
		if (chunkNbt.contains("palette")) {
			// v2: palette of unique ids + per-position palette indices. A whole chunk of modded
			// terrain (e.g. the Aether) has tens of thousands of entries but only a handful of
			// unique blocks, so the string payload stays far below the 64KB NBT-string limit.
			String[] palette = chunkNbt.getString("palette").split("\0");
			byte[] idx = chunkNbt.getByteArray("paletteIdx");
			if (idx.length != positions.length) {
				LOGGER.warn("Mismatched positions/paletteIdx arrays for chunk {},{}", chunkX, chunkZ);
				return;
			}
			ids = new String[idx.length];
			for (int i = 0; i < idx.length; i++) {
				int pi = idx[i] & 0xFF;
				ids[i] = pi < palette.length ? palette[pi] : "unknown:0";
			}
		} else {
			// v1 (legacy): all ids joined into one string
			String idsJoined = chunkNbt.getString("ids");
			if (idsJoined.isEmpty()) return;
			ids = idsJoined.split("\0");
		}
		if (positions.length != ids.length) {
			LOGGER.warn("Mismatched positions/ids arrays for chunk {},{}", chunkX, chunkZ);
			return;
		}

		for (int i = 0; i < positions.length; i++) {
			String stringId = ids[i];
			String[] parts = stringId.split(":", 2);
			if (parts.length != 2) continue;

			int position = positions[i];
			if (position < 0 || position >= chunk.blocks.length) continue;
			int meta = (i < metadata.length) ? (metadata[i] & 0xFF) : 0;

			NamespacedIdentifier retroId = NamespacedIdentifiers.from(parts[0], parts[1]);
			BlockRegistration reg = RetroRegistry.getBlockById(retroId);
			if (reg == null) {
				// Mod currently missing - defer so the next save carries the entry forward.
				defer(key, position, stringId, meta);
				LOGGER.warn("Unknown block {} in sidecar for chunk {},{} - deferred (kept in sidecar)", stringId, chunkX, chunkZ);
				continue;
			}

			// Displacement check: a vanilla session may have placed a REAL block at this
			// (invisible-to-vanilla) position. The vanilla block wins while it is there; the
			// modded entry stays deferred in the sidecar and restores once the position is air.
			if ((chunk.blocks[position] & 0xFF) != 0) {
				defer(key, position, stringId, meta);
				continue;
			}

			extended.set(position, reg.getBlock().id, meta);
			// Apply meta only AFTER the block-match + displacement checks: the sidecar is
			// authoritative for restored positions (the nibble is just the runtime mirror, and a
			// vanilla session may have scribbled on it); vanilla-owned positions keep vanilla meta.
			chunk.meta.set(ChunkExtendedBlocks.indexToX(position), ChunkExtendedBlocks.indexToY(position),
					ChunkExtendedBlocks.indexToZ(position), meta);
		}
	}

	private void defer(String chunkKey, int position, String stringId, int meta) {
		deferred.computeIfAbsent(chunkKey, k -> new ArrayList<>()).add(new Deferred(position, stringId, meta));
	}

	// --- v4: auxiliary per-position data (RetroBlockData) ---------------------------------------

	/**
	 * Reads every data section of a chunk. Raw sections are position/value pairs; block-reference
	 * sections carry a string palette instead of the id, because a runtime block id belongs to the
	 * installed mod set rather than to the world.
	 */
	private void loadBlockData(String chunkKey, NbtCompound chunkNbt, ChunkExtendedBlocks extended) {
		if (!chunkNbt.contains("data")) {
			return;
		}
		NbtCompound data = chunkNbt.getCompound("data");
		for (net.minecraft.nbt.NbtElement element : data.values()) {
			if (!(element instanceof NbtCompound section)) {
				continue;
			}
			String dataKey = element.getKey();
			int[] positions = bytesToInts(section.getByteArray("pos"));
			if (positions.length == 0) {
				continue;
			}

			if (section.contains("palette")) {
				String[] palette = section.getString("palette").split("\0");
				byte[] paletteIdx = section.getByteArray("idx");
				byte[] metas = section.getByteArray("meta");
				if (paletteIdx.length != positions.length) {
					LOGGER.warn("Mismatched pos/idx arrays in data section {} for chunk {}", dataKey, chunkKey);
					continue;
				}
				for (int i = 0; i < positions.length; i++) {
					int pi = paletteIdx[i] & 0xFF;
					String stringId = pi < palette.length ? palette[pi] : "";
					int meta = i < metas.length ? (metas[i] & 0xF) : 0;
					int blockId = resolveBlockRef(stringId);
					if (blockId <= 0) {
						// The mod that owns this block is not here right now. Park the entry so the
						// next save carries it forward instead of silently erasing the reference.
						deferredData.computeIfAbsent(chunkKey, k -> new ArrayList<>())
							.add(new DeferredData(dataKey, positions[i], stringId, meta));
						continue;
					}
					extended.setData(dataKey, positions[i], RetroBlockData.encodeBlockRef(blockId, meta));
				}
			} else {
				int[] values = bytesToInts(section.getByteArray("val"));
				if (values.length != positions.length) {
					LOGGER.warn("Mismatched pos/val arrays in data section {} for chunk {}", dataKey, chunkKey);
					continue;
				}
				for (int i = 0; i < positions.length; i++) {
					extended.setData(dataKey, positions[i], values[i]);
				}
			}
		}
	}

	/** Writes every data section of a chunk, merging back anything that could not be resolved at load. */
	private void writeBlockData(String chunkKey, NbtCompound chunkNbt, ChunkExtendedBlocks extended) {
		Map<String, Map<Integer, Integer>> live = extended.getDataMap();
		List<DeferredData> parked = deferredData.get(chunkKey);
		if (live.isEmpty() && (parked == null || parked.isEmpty())) {
			return;
		}

		// One section per data key, over the union of live keys and parked keys.
		java.util.Set<String> keys = new java.util.LinkedHashSet<>(live.keySet());
		if (parked != null) {
			for (DeferredData d : parked) {
				keys.add(d.dataKey);
			}
		}

		NbtCompound data = new NbtCompound();
		for (String dataKey : keys) {
			Map<Integer, Integer> byPosition = live.getOrDefault(dataKey, java.util.Collections.emptyMap());
			List<DeferredData> kept = new ArrayList<>();
			if (parked != null) {
				for (DeferredData d : parked) {
					// A parked entry whose position now carries a live value has been superseded.
					if (d.dataKey.equals(dataKey) && !byPosition.containsKey(d.position)) {
						kept.add(d);
					}
				}
			}
			int total = byPosition.size() + kept.size();
			if (total == 0) {
				continue;
			}

			RetroBlockDataType type = RetroBlockData.byKey(dataKey);
			NbtCompound section = new NbtCompound();
			int[] positions = new int[total];
			int i = 0;

			// A section is written in block-reference form when its type says so, and ALSO whenever
			// the only thing we have for it is parked references - the type is unregistered exactly
			// when its mod is missing, which is the case those entries exist for.
			boolean blockRef = (type != null && type.isBlockRef()) || (type == null && !kept.isEmpty());
			if (blockRef) {
				byte[] metas = new byte[total];
				byte[] paletteIdx = new byte[total];
				Map<String, Integer> palette = new java.util.LinkedHashMap<>();
				for (Map.Entry<Integer, Integer> entry : byPosition.entrySet()) {
					positions[i] = entry.getKey();
					int value = entry.getValue();
					metas[i] = (byte) RetroBlockData.blockRefMeta(value);
					paletteIdx[i] = (byte) paletteIndex(palette, blockRefStringId(RetroBlockData.blockRefId(value)),
						chunkKey);
					i++;
				}
				for (DeferredData d : kept) {
					positions[i] = d.position;
					metas[i] = (byte) d.meta;
					paletteIdx[i] = (byte) paletteIndex(palette, d.stringId, chunkKey);
					i++;
				}
				StringBuilder paletteBuilder = new StringBuilder();
				for (String id : palette.keySet()) {
					if (paletteBuilder.length() > 0) paletteBuilder.append('\0');
					paletteBuilder.append(id);
				}
				section.putByteArray("pos", intsToBytes(positions));
				section.putString("palette", paletteBuilder.toString());
				section.putByteArray("idx", paletteIdx);
				section.putByteArray("meta", metas);
			} else {
				int[] values = new int[total];
				for (Map.Entry<Integer, Integer> entry : byPosition.entrySet()) {
					positions[i] = entry.getKey();
					values[i] = entry.getValue();
					i++;
				}
				section.putByteArray("pos", intsToBytes(positions));
				section.putByteArray("val", intsToBytes(values));
			}
			data.put(dataKey, section);
		}

		if (!data.values().isEmpty()) {
			chunkNbt.put("data", data);
		}
	}

	// --- v5: cubic biome cells ------------------------------------------------------------------

	/**
	 * Reads a chunk's cubic biome cells. Cells name their biome through a per-chunk string palette, the
	 * same discipline as modded blocks and block references, because a runtime biome id belongs to the
	 * installed mod set rather than to the world.
	 */
	private void loadCubicBiomes(String chunkKey, NbtCompound chunkNbt, ChunkExtendedBlocks extended) {
		if (!chunkNbt.contains("cbio")) {
			return;
		}
		NbtCompound section = chunkNbt.getCompound("cbio");
		int[] cells = bytesToInts(section.getByteArray("cells"));
		if (cells.length == 0) {
			return;
		}
		String[] palette = section.getString("palette").split("\0");
		byte[] paletteIdx = section.getByteArray("idx");
		if (paletteIdx.length != cells.length) {
			LOGGER.warn("Mismatched cells/idx arrays in cbio section for chunk {}", chunkKey);
			return;
		}
		for (int i = 0; i < cells.length; i++) {
			int pi = paletteIdx[i] & 0xFF;
			String biomeId = pi < palette.length ? palette[pi] : "";
			com.periut.retroapi.biome.cubic.RetroCubicBiome biome =
				com.periut.retroapi.biome.cubic.RetroCubicBiomes.byId(biomeId);
			if (biome == null) {
				// The mod that registered this biome is not here right now. Park the cell so the next
				// save carries it forward instead of silently repainting the cavern.
				deferredCells.computeIfAbsent(chunkKey, k -> new ArrayList<>())
					.add(new DeferredCell(cells[i], biomeId));
				continue;
			}
			extended.setCubicBiome(cells[i], biome.getRuntimeId());
		}
	}

	/**
	 * Writes a chunk's cubic biome cells, merging back any cell that could not be resolved at load.
	 * Writes nothing at all when the chunk has no cells, so a chunk that never got a cave biome stays
	 * byte-identical to a v4 file.
	 */
	private void writeCubicBiomes(String chunkKey, NbtCompound chunkNbt, ChunkExtendedBlocks extended) {
		Map<Integer, Integer> live = extended.getCubicBiomeMap();
		List<DeferredCell> parked = deferredCells.get(chunkKey);
		if (live.isEmpty() && (parked == null || parked.isEmpty())) {
			return;
		}

		List<DeferredCell> kept = new ArrayList<>();
		if (parked != null) {
			for (DeferredCell cell : parked) {
				// A parked cell whose slot was assigned this session has been superseded.
				if (!live.containsKey(cell.cellY)) {
					kept.add(cell);
				}
			}
		}

		int total = live.size() + kept.size();
		if (total == 0) {
			return;
		}
		int[] cells = new int[total];
		byte[] paletteIdx = new byte[total];
		Map<String, Integer> palette = new java.util.LinkedHashMap<>();
		int i = 0;
		for (Map.Entry<Integer, Integer> entry : live.entrySet()) {
			com.periut.retroapi.biome.cubic.RetroCubicBiome biome =
				com.periut.retroapi.biome.cubic.RetroCubicBiomes.byRuntimeId(entry.getValue());
			if (biome == null) {
				continue;
			}
			cells[i] = entry.getKey();
			paletteIdx[i] = (byte) paletteIndex(palette, biome.getId().toString(), chunkKey);
			i++;
		}
		for (DeferredCell cell : kept) {
			cells[i] = cell.cellY;
			paletteIdx[i] = (byte) paletteIndex(palette, cell.biomeId, chunkKey);
			i++;
		}
		if (i == 0) {
			return;
		}

		StringBuilder paletteBuilder = new StringBuilder();
		for (String id : palette.keySet()) {
			if (paletteBuilder.length() > 0) paletteBuilder.append('\0');
			paletteBuilder.append(id);
		}

		NbtCompound section = new NbtCompound();
		// i may be below total if a live runtime id went stale mid-session; trim so the arrays agree.
		section.putByteArray("cells", intsToBytes(java.util.Arrays.copyOf(cells, i)));
		section.putString("palette", paletteBuilder.toString());
		section.putByteArray("idx", java.util.Arrays.copyOf(paletteIdx, i));
		chunkNbt.put("cbio", section);
	}

	// --- v5: out-of-vanilla-column terrain (RetroWorldHeight) -----------------------------------

	/**
	 * Reads a chunk's extension sections. Ids are stored numerically for vanilla blocks and through a
	 * per-section string palette for modded ones, so an extension section survives a change in the
	 * installed mod set the same way the modded-block section does. A modded id whose mod is missing
	 * reads back as air <em>in memory</em> but the palette entry is rewritten verbatim on save, so the
	 * block returns when the mod does.
	 */
	private void loadExtendedSections(NbtCompound chunkNbt,
			ChunkExtendedBlocks extended) {
		if (!chunkNbt.contains("vext")) {
			return;
		}
		NbtCompound vext = chunkNbt.getCompound("vext");
		com.periut.retroapi.world.height.RetroExtendedSections sections = extended.getExtendedSections();
		for (net.minecraft.nbt.NbtElement element : vext.values()) {
			if (!(element instanceof NbtCompound sectionNbt)) {
				continue;
			}
			int sectionIndex;
			try {
				sectionIndex = Integer.parseInt(element.getKey());
			} catch (NumberFormatException e) {
				LOGGER.warn("Bad vext section key {}", element.getKey());
				continue;
			}
			byte[] raw = sectionNbt.getByteArray("ids");
			byte[] meta = sectionNbt.getByteArray("meta");
			int cells = com.periut.retroapi.world.height.RetroExtendedSections.SECTION_SIZE;
			if (raw.length != cells * 2) {
				LOGGER.warn("Bad vext ids length {} in section {}", raw.length, sectionIndex);
				continue;
			}
			short[] ids = new short[cells];
			for (int i = 0; i < cells; i++) {
				ids[i] = (short) (((raw[i * 2] & 0xFF) << 8) | (raw[i * 2 + 1] & 0xFF));
			}
			// Remap palette-stored modded ids back to this session's runtime ids.
			if (sectionNbt.contains("palette")) {
				String[] palette = sectionNbt.getString("palette").split("\0");
				int[] resolved = new int[palette.length];
				for (int i = 0; i < palette.length; i++) {
					resolved[i] = resolveBlockRef(palette[i]);
				}
				for (int i = 0; i < cells; i++) {
					int stored = ids[i] & 0xFFFF;
					// Palette references are stored as (0x8000 | paletteIndex) so they cannot collide
					// with a numerically stored vanilla id.
					if ((stored & 0x8000) != 0) {
						int pi = stored & 0x7FFF;
						ids[i] = (short) (pi < resolved.length ? resolved[pi] : 0);
					}
				}
			}
			sections.putSection(sectionIndex, ids, meta);
		}
		// A freshly loaded chunk is not "changed"; only later writes should mark it dirty.
		sections.clearDirty();
	}

	/**
	 * Writes a chunk's extension sections, skipping any that turned out to be entirely air so an
	 * unextended world keeps writing v4-identical files.
	 */
	private void writeExtendedSections(NbtCompound chunkNbt, ChunkExtendedBlocks extended) {
		com.periut.retroapi.world.height.RetroExtendedSections sections = extended.getExtendedSections();
		sections.prune();
		if (sections.isEmpty()) {
			return;
		}
		NbtCompound vext = new NbtCompound();
		for (Map.Entry<Integer, com.periut.retroapi.world.height.RetroExtendedSections.Section> entry
				: sections.getSections().entrySet()) {
			short[] ids = entry.getValue().ids();
			byte[] meta = entry.getValue().meta();
			int cells = ids.length;
			byte[] raw = new byte[cells * 2];
			Map<String, Integer> palette = new java.util.LinkedHashMap<>();
			for (int i = 0; i < cells; i++) {
				int id = ids[i] & 0xFFFF;
				int stored;
				if (id >= 256) {
					// Modded: store a palette reference with bit 15 set as the tag. That bit is free
					// because RetroIds.BLOCK_ID_CAPACITY is 32000, so every valid runtime id is below
					// 0x8000 and a tagged value can never be mistaken for one.
					int pi = palette.computeIfAbsent(blockRefStringId(id), k -> palette.size());
					stored = 0x8000 | (pi & 0x7FFF);
				} else {
					stored = id;
				}
				raw[i * 2] = (byte) (stored >> 8);
				raw[i * 2 + 1] = (byte) stored;
			}
			NbtCompound sectionNbt = new NbtCompound();
			sectionNbt.putByteArray("ids", raw);
			sectionNbt.putByteArray("meta", meta.clone());
			if (!palette.isEmpty()) {
				StringBuilder paletteBuilder = new StringBuilder();
				for (String id : palette.keySet()) {
					if (paletteBuilder.length() > 0) paletteBuilder.append('\0');
					paletteBuilder.append(id);
				}
				sectionNbt.putString("palette", paletteBuilder.toString());
			}
			vext.put(Integer.toString(entry.getKey()), sectionNbt);
		}
		chunkNbt.put("vext", vext);
	}

	/**
	 * The world-stable name of a block referenced by data. Vanilla ids are fixed for all time, so they
	 * are written numerically ({@code "#20"}); modded ids are a per-install allocation and are written
	 * as the RetroAPI string id.
	 */
	private static String blockRefStringId(int blockId) {
		if (blockId < 256) {
			return "#" + blockId;
		}
		if (blockId < net.minecraft.block.Block.BLOCKS.length) {
			net.minecraft.block.Block block = net.minecraft.block.Block.BLOCKS[blockId];
			if (block != null) {
				BlockRegistration reg = RetroRegistry.getBlockRegistration(block);
				if (reg != null) {
					return reg.getId().toString();
				}
			}
		}
		return "unknown:" + blockId;
	}

	/** The runtime block id for a name written by {@link #blockRefStringId}, or 0 when unresolvable. */
	private static int resolveBlockRef(String stringId) {
		if (stringId.isEmpty()) {
			return 0;
		}
		if (stringId.charAt(0) == '#') {
			try {
				int id = Integer.parseInt(stringId.substring(1));
				return (id > 0 && id < 256) ? id : 0;
			} catch (NumberFormatException e) {
				return 0;
			}
		}
		String[] parts = stringId.split(":", 2);
		if (parts.length != 2) {
			return 0;
		}
		BlockRegistration reg = RetroRegistry.getBlockById(NamespacedIdentifiers.from(parts[0], parts[1]));
		return reg == null ? 0 : reg.getBlock().id;
	}

	private static int paletteIndex(Map<String, Integer> palette, String stringId, String chunkKey) {
		Integer pi = palette.get(stringId);
		if (pi == null) {
			pi = palette.size();
			if (pi > 255) {
				LOGGER.error("More than 256 unique block references in chunk {} - dropping {}", chunkKey, stringId);
				return 0;
			}
			palette.put(stringId, pi);
		}
		return pi;
	}

	public void saveChunkData(int chunkX, int chunkZ, ChunkExtendedBlocks extended) {
		if (!root.contains("chunks")) {
			root.put("chunks", new NbtCompound());
		}
		NbtCompound chunks = root.getCompound("chunks");
		String key = chunkX + "," + chunkZ;

		// Deferred entries (displaced by a vanilla session, or owned by a missing mod) are merged
		// back in so a save NEVER drops them - previously the save rebuilt purely from the live
		// extended map, silently losing anything that couldn't be restored at load. An entry whose
		// position got re-occupied by a live modded block this session is superseded and dropped.
		List<Deferred> kept = new ArrayList<>();
		List<Deferred> chunkDeferred = deferred.get(key);
		if (chunkDeferred != null) {
			for (Deferred d : chunkDeferred) {
				if (!extended.hasEntry(d.position)) {
					kept.add(d);
				}
			}
		}

		Map<Integer, Integer> blockIds = extended.getBlockIds();
		int total = blockIds.size() + kept.size();
		if (total == 0) {
			// Store empty entry (it may still carry xmeta or auxiliary data for vanilla-stored blocks)
			NbtCompound empty = new NbtCompound();
			empty.putByteArray("positions", new byte[0]);
			empty.putString("ids", "");
			empty.putByteArray("metadata", new byte[0]);
			writeXmeta(empty, extended);
			writeBlockData(key, empty, extended);
			writeCubicBiomes(key, empty, extended);
			writeExtendedSections(empty, extended);
			chunks.put(key, empty);
			dirty = true;
			return;
		}

		int[] positions = new int[total];
		byte[] paletteIdx = new byte[total];
		byte[] metadata = new byte[total];
		java.util.Map<String, Integer> palette = new java.util.LinkedHashMap<>();

		int i = 0;
		for (Map.Entry<Integer, Integer> entry : blockIds.entrySet()) {
			positions[i] = entry.getKey();
			String stringId = resolveStringId(entry.getValue());
			paletteIdx[i] = (byte) paletteIndex(palette, stringId, chunkX, chunkZ);
			metadata[i] = (byte) extended.getMetadata(entry.getKey());
			i++;
		}
		for (Deferred d : kept) {
			positions[i] = d.position;
			paletteIdx[i] = (byte) paletteIndex(palette, d.stringId, chunkX, chunkZ);
			metadata[i] = (byte) d.meta;
			i++;
		}

		StringBuilder paletteBuilder = new StringBuilder();
		for (String id : palette.keySet()) {
			if (paletteBuilder.length() > 0) paletteBuilder.append('\0');
			paletteBuilder.append(id);
		}

		NbtCompound chunkNbt = new NbtCompound();
		chunkNbt.putByteArray("positions", intsToBytes(positions));
		chunkNbt.putString("palette", paletteBuilder.toString());
		chunkNbt.putByteArray("paletteIdx", paletteIdx);
		chunkNbt.putByteArray("metadata", metadata);
		writeXmeta(chunkNbt, extended);
		writeBlockData(key, chunkNbt, extended);
		writeCubicBiomes(key, chunkNbt, extended);
		writeExtendedSections(chunkNbt, extended);

		chunks.put(key, chunkNbt);
		dirty = true;
	}

	/**
	 * v3 section: secondary meta (state index bits 4-11) by position, omitted entirely when
	 * no position has high bits, which keeps such chunks byte-identical to v2 files.
	 */
	private static void writeXmeta(NbtCompound chunkNbt, ChunkExtendedBlocks extended) {
		Map<Integer, Integer> xmeta = extended.getXmetaMap();
		if (xmeta.isEmpty()) {
			return;
		}
		int[] xPositions = new int[xmeta.size()];
		byte[] xValues = new byte[xmeta.size()];
		int i = 0;
		for (Map.Entry<Integer, Integer> entry : xmeta.entrySet()) {
			xPositions[i] = entry.getKey();
			xValues[i] = (byte) (entry.getValue() & 0xFF);
			i++;
		}
		chunkNbt.putByteArray("xpos", intsToBytes(xPositions));
		chunkNbt.putByteArray("xval", xValues);
	}

	private static int paletteIndex(java.util.Map<String, Integer> palette, String stringId, int chunkX, int chunkZ) {
		Integer pi = palette.get(stringId);
		if (pi == null) {
			pi = palette.size();
			if (pi > 255) {
				LOGGER.error("More than 256 unique modded blocks in chunk {},{} - dropping {}", chunkX, chunkZ, stringId);
				return 0;
			}
			palette.put(stringId, pi);
		}
		return pi;
	}

	private String resolveStringId(int blockId) {
		if (blockId > 0 && blockId < net.minecraft.block.Block.BLOCKS.length) {
			net.minecraft.block.Block block = net.minecraft.block.Block.BLOCKS[blockId];
			if (block != null) {
				BlockRegistration reg = RetroRegistry.getBlockRegistration(block);
				if (reg != null) {
					return reg.getId().toString();
				}
			}
		}
		return "unknown:" + blockId;
	}

	private static byte[] intsToBytes(int[] ints) {
		ByteBuffer buf = ByteBuffer.allocate(ints.length * 4);
		for (int v : ints) {
			buf.putInt(v);
		}
		return buf.array();
	}

	private static int[] bytesToInts(byte[] bytes) {
		ByteBuffer buf = ByteBuffer.wrap(bytes);
		int[] ints = new int[bytes.length / 4];
		for (int i = 0; i < ints.length; i++) {
			ints[i] = buf.getInt();
		}
		return ints;
	}
}
