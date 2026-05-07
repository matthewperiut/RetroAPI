package com.periut.retroapi.storage;

import net.ornithemc.osl.core.api.util.NamespacedIdentifier;
import net.ornithemc.osl.core.api.util.NamespacedIdentifiers;
import com.periut.retroapi.registry.BlockRegistration;
import com.periut.retroapi.registry.RetroRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.Map;

public class RegionSidecar {
	private static final Logger LOGGER = LogManager.getLogger("RetroAPI/RegionSidecar");
	private static final int VERSION = 1;

	private final File file;
	private NbtCompound root;
	private boolean dirty = false;

	public RegionSidecar(File file) {
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
			LOGGER.error("Failed to load region sidecar {}", file, e);
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
			LOGGER.error("Failed to save region sidecar {}", file, e);
		}
	}

	public void loadChunkData(int chunkX, int chunkZ, ChunkExtendedBlocks extended) {
		if (!root.contains("chunks")) return;
		NbtCompound chunks = root.getCompound("chunks");
		String key = chunkX + "," + chunkZ;

		if (!chunks.contains(key)) return;
		NbtCompound chunkNbt = chunks.getCompound(key);

		// Positions are encoded as byte array (4 bytes per int, big-endian)
		byte[] posBytes = chunkNbt.getByteArray("positions");
		String idsJoined = chunkNbt.getString("ids");
		byte[] metadata = chunkNbt.getByteArray("metadata");

		if (posBytes.length == 0 || idsJoined.isEmpty()) return;

		int[] positions = bytesToInts(posBytes);
		String[] ids = idsJoined.split("\0");
		if (positions.length != ids.length) {
			LOGGER.warn("Mismatched positions/ids arrays for chunk {},{}", chunkX, chunkZ);
			return;
		}

		for (int i = 0; i < positions.length; i++) {
			String stringId = ids[i];
			String[] parts = stringId.split(":", 2);
			if (parts.length != 2) continue;

			NamespacedIdentifier retroId = NamespacedIdentifiers.from(parts[0], parts[1]);
			BlockRegistration reg = RetroRegistry.getBlockById(retroId);
			if (reg == null) {
				LOGGER.warn("Unknown block {} in sidecar for chunk {},{} - preserving in sidecar", stringId, chunkX, chunkZ);
				continue;
			}

			int meta = (i < metadata.length) ? (metadata[i] & 0xFF) : 0;
			extended.set(positions[i], reg.getBlock().id, meta);
		}
	}

	public void saveChunkData(int chunkX, int chunkZ, ChunkExtendedBlocks extended) {
		if (!root.contains("chunks")) {
			root.putCompound("chunks", new NbtCompound());
		}
		NbtCompound chunks = root.getCompound("chunks");
		String key = chunkX + "," + chunkZ;

		if (extended.isEmpty()) {
			// Store empty entry
			NbtCompound empty = new NbtCompound();
			empty.putByteArray("positions", new byte[0]);
			empty.putString("ids", "");
			empty.putByteArray("metadata", new byte[0]);
			chunks.putCompound(key, empty);
			dirty = true;
			return;
		}

		Map<Integer, Integer> blockIds = extended.getBlockIds();
		int[] positions = new int[blockIds.size()];
		StringBuilder idsBuilder = new StringBuilder();
		byte[] metadata = new byte[blockIds.size()];

		int i = 0;
		for (Map.Entry<Integer, Integer> entry : blockIds.entrySet()) {
			positions[i] = entry.getKey();
			int blockId = entry.getValue();

			String stringId = resolveStringId(blockId);
			if (i > 0) idsBuilder.append('\0');
			idsBuilder.append(stringId);

			metadata[i] = (byte) extended.getMetadata(entry.getKey());
			i++;
		}

		NbtCompound chunkNbt = new NbtCompound();
		chunkNbt.putByteArray("positions", intsToBytes(positions));
		chunkNbt.putString("ids", idsBuilder.toString());
		chunkNbt.putByteArray("metadata", metadata);

		chunks.putCompound(key, chunkNbt);
		dirty = true;
	}

	private String resolveStringId(int blockId) {
		if (blockId > 0 && blockId < net.minecraft.block.Block.BY_ID.length) {
			net.minecraft.block.Block block = net.minecraft.block.Block.BY_ID[blockId];
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
