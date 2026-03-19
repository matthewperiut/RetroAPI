package com.periut.retroapi.registry;

import net.ornithemc.osl.core.api.util.NamespacedIdentifier;
import net.ornithemc.osl.core.api.util.NamespacedIdentifiers;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.*;

public class IdMap {
	private static final Logger LOGGER = LogManager.getLogger("RetroAPI/IdMap");

	private final Map<NamespacedIdentifier, Integer> blockIds = new HashMap<>();
	private final Map<NamespacedIdentifier, Integer> itemIds = new HashMap<>();

	public void load(File file) {
		if (!file.exists()) {
			return;
		}
		try (FileInputStream fis = new FileInputStream(file)) {
			NbtCompound root = NbtIo.readCompressed(fis);

			// NbtCompound has no key iteration API in b1.7.3, so use manual NBT key extraction
			loadFromNbt(root);

			LOGGER.info("Loaded ID map with {} blocks and {} items", blockIds.size(), itemIds.size());
		} catch (IOException e) {
			LOGGER.error("Failed to load ID map", e);
		}
	}

	private void loadFromNbt(NbtCompound root) {
		if (root.contains("blocks")) {
			NbtCompound blocks = root.getCompound("blocks");
			for (String key : getKeys(blocks)) {
				int id = blocks.getInt(key);
				String[] parts = key.split(":", 2);
				if (parts.length == 2) {
					blockIds.put(NamespacedIdentifiers.from(parts[0], parts[1]), id);
				}
			}
		}
		if (root.contains("items")) {
			NbtCompound items = root.getCompound("items");
			for (String key : getKeys(items)) {
				int id = items.getInt(key);
				String[] parts = key.split(":", 2);
				if (parts.length == 2) {
					itemIds.put(NamespacedIdentifiers.from(parts[0], parts[1]), id);
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	private static java.util.Set<String> getKeys(NbtCompound compound) {
		// NbtCompound.getValues() returns Collection<NbtElement>, but the internal map
		// has String keys. We need to write and re-read to get keys, or use reflection.
		// For simplicity, write to byte array and re-read with key tracking.
		java.util.Set<String> keys = new java.util.HashSet<>();
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream dos = new DataOutputStream(baos);
			NbtCompound wrapper = new NbtCompound();
			wrapper.putCompound("d", compound);
			NbtIo.write(wrapper, dos);
			dos.flush();

			DataInputStream dis = new DataInputStream(new ByteArrayInputStream(baos.toByteArray()));
			// Read root compound tag
			byte rootType = dis.readByte(); // 10 = compound
			dis.readUTF(); // root name

			// Read inner compound "d"
			byte innerType = dis.readByte(); // 10 = compound
			dis.readUTF(); // "d"

			// Now read all entries in the inner compound
			while (true) {
				byte type = dis.readByte();
				if (type == 0) break; // end tag
				String name = dis.readUTF();
				keys.add(name);
				skipNbtValue(dis, type);
			}
		} catch (IOException e) {
			// Should not happen with byte arrays
		}
		return keys;
	}

	private static void skipNbtValue(DataInputStream dis, byte type) throws IOException {
		switch (type) {
			case 1 -> dis.readByte();
			case 2 -> dis.readShort();
			case 3 -> dis.readInt();
			case 4 -> dis.readLong();
			case 5 -> dis.readFloat();
			case 6 -> dis.readDouble();
			case 7 -> { int len = dis.readInt(); dis.skipBytes(len); }
			case 8 -> dis.readUTF();
			case 9 -> {
				byte listType = dis.readByte();
				int count = dis.readInt();
				for (int i = 0; i < count; i++) {
					skipNbtValue(dis, listType);
				}
			}
			case 10 -> {
				while (true) {
					byte t = dis.readByte();
					if (t == 0) break;
					dis.readUTF(); // key
					skipNbtValue(dis, t);
				}
			}
		}
	}

	public void save(File file) {
		NbtCompound root = new NbtCompound();

		NbtCompound blocks = new NbtCompound();
		for (Map.Entry<NamespacedIdentifier, Integer> entry : blockIds.entrySet()) {
			blocks.putInt(entry.getKey().toString(), entry.getValue());
		}
		root.putCompound("blocks", blocks);

		NbtCompound items = new NbtCompound();
		for (Map.Entry<NamespacedIdentifier, Integer> entry : itemIds.entrySet()) {
			items.putInt(entry.getKey().toString(), entry.getValue());
		}
		root.putCompound("items", items);

		file.getParentFile().mkdirs();
		try (FileOutputStream fos = new FileOutputStream(file)) {
			NbtIo.writeCompressed(root, fos);
			LOGGER.info("Saved ID map with {} blocks and {} items", blockIds.size(), itemIds.size());
		} catch (IOException e) {
			LOGGER.error("Failed to save ID map", e);
		}
	}

	public Integer getBlockId(NamespacedIdentifier id) {
		return blockIds.get(id);
	}

	public Integer getItemId(NamespacedIdentifier id) {
		return itemIds.get(id);
	}

	public void putBlockId(NamespacedIdentifier id, int numericId) {
		blockIds.put(id, numericId);
	}

	public void putItemId(NamespacedIdentifier id, int numericId) {
		itemIds.put(id, numericId);
	}

	public Map<NamespacedIdentifier, Integer> getBlockIds() {
		return blockIds;
	}

	public Map<NamespacedIdentifier, Integer> getItemIds() {
		return itemIds;
	}

	public void purgeStaleEntries(List<BlockRegistration> blocks, List<ItemRegistration> items) {
		Set<NamespacedIdentifier> activeBlocks = new HashSet<>();
		for (BlockRegistration reg : blocks) {
			activeBlocks.add(reg.getId());
		}
		Set<NamespacedIdentifier> activeItems = new HashSet<>();
		for (ItemRegistration reg : items) {
			activeItems.add(reg.getId());
		}

		int removedBlocks = 0, removedItems = 0;
		Iterator<NamespacedIdentifier> it = blockIds.keySet().iterator();
		while (it.hasNext()) {
			if (!activeBlocks.contains(it.next())) {
				it.remove();
				removedBlocks++;
			}
		}
		it = itemIds.keySet().iterator();
		while (it.hasNext()) {
			if (!activeItems.contains(it.next())) {
				it.remove();
				removedItems++;
			}
		}

		if (removedBlocks > 0 || removedItems > 0) {
			LOGGER.info("Purged {} stale block and {} stale item entries from ID map", removedBlocks, removedItems);
		}
	}
}
