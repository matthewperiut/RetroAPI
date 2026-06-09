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
		int chunkX = chunk.x;
		int chunkZ = chunk.z;
		String key = chunkX + "," + chunkZ;
		// Rebuild this chunk's deferral list from scratch on every load (re-check semantics).
		deferred.remove(key);

		if (!root.contains("chunks")) return;
		NbtCompound chunks = root.getCompound("chunks");

		if (!chunks.contains(key)) return;
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
			// Store empty entry (it may still carry xmeta for vanilla-stored blocks)
			NbtCompound empty = new NbtCompound();
			empty.putByteArray("positions", new byte[0]);
			empty.putString("ids", "");
			empty.putByteArray("metadata", new byte[0]);
			writeXmeta(empty, extended);
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
