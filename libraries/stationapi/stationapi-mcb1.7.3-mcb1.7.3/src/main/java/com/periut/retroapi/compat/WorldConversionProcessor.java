package com.periut.retroapi.compat;

import net.ornithemc.osl.core.api.util.NamespacedIdentifier;
import com.periut.retroapi.registry.BlockRegistration;
import com.periut.retroapi.registry.IdMap;
import com.periut.retroapi.registry.ItemRegistration;
import com.periut.retroapi.registry.RetroRegistry;
import com.periut.retroapi.storage.ChunkExtendedBlocks;
import com.periut.retroapi.storage.RegionSidecar;
import com.periut.retroapi.storage.InventorySidecar;
import net.minecraft.nbt.*;
import net.minecraft.world.chunk.storage.RegionFile;
import net.modificationstation.stationapi.api.nbt.StationNbtCompound;
import net.modificationstation.stationapi.api.util.collection.PackedIntegerArray;
import net.modificationstation.stationapi.api.vanillafix.datafixer.schema.StationFlatteningItemStackSchema;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * Second-pass processor that injects RetroAPI sidecar data into StationAPI-converted chunks.
 * Runs after FlattenedWorldStorage.convertChunks() completes.
 */
public class WorldConversionProcessor {
	private static final Logger LOGGER = LogManager.getLogger("RetroAPI/ConversionProcessor");
	private static final String SECTIONS_KEY = "stationapi:sections";
	private static final String STATION_ID = "stationapi:id";

	public static void processSidecars(File worldDir) {
		File idMapFile = new File(worldDir, "retroapi/id_map.dat");
		if (!idMapFile.exists()) {
			LOGGER.info("No retroapi/id_map.dat found, skipping sidecar processing");
			return;
		}

		File sidecarChunksDir = new File(worldDir, "retroapi/chunks");
		File sidecarInventoriesDir = new File(worldDir, "retroapi/inventories");
		boolean hasChunkSidecars = sidecarChunksDir.exists() && sidecarChunksDir.isDirectory();
		boolean hasInventorySidecars = sidecarInventoriesDir.exists() && sidecarInventoriesDir.isDirectory();

		if (!hasChunkSidecars && !hasInventorySidecars) {
			LOGGER.info("No sidecar data found, skipping sidecar processing");
			return;
		}

		LOGGER.info("Processing RetroAPI sidecars for world: {}", worldDir);

		// Scan all region files in the world
		File regionDir = new File(worldDir, "region");
		if (!regionDir.exists()) {
			LOGGER.warn("No region directory found in {}", worldDir);
			return;
		}

		File[] regionFiles = regionDir.listFiles((dir, name) -> name.endsWith(".mcr"));
		if (regionFiles == null || regionFiles.length == 0) {
			LOGGER.warn("No region files found in {}", regionDir);
			return;
		}

		int processedChunks = 0;
		int modifiedChunks = 0;

		for (File regionFile : regionFiles) {
			// Parse region coordinates from filename: r.X.Z.mcr
			String name = regionFile.getName();
			String[] parts = name.split("\\.");
			if (parts.length != 4) continue;
			int regionX, regionZ;
			try {
				regionX = Integer.parseInt(parts[1]);
				regionZ = Integer.parseInt(parts[2]);
			} catch (NumberFormatException e) {
				continue;
			}

			// Load sidecars for this region
			File chunkSidecarFile = new File(worldDir, "retroapi/chunks/r." + regionX + "." + regionZ + ".dat");
			File invSidecarFile = new File(worldDir, "retroapi/inventories/r." + regionX + "." + regionZ + ".dat");

			NbtCompound chunkSidecarRoot = loadSidecarNbt(chunkSidecarFile);
			NbtCompound invSidecarRoot = loadSidecarNbt(invSidecarFile);

			if (chunkSidecarRoot == null && invSidecarRoot == null) continue;

			RegionFile region = new RegionFile(regionFile);
			try {
				// Iterate all chunks in the region
				for (int localX = 0; localX < 32; localX++) {
					for (int localZ = 0; localZ < 32; localZ++) {
						int chunkX = regionX * 32 + localX;
						int chunkZ = regionZ * 32 + localZ;

						if (!region.hasChunkData(localX, localZ)) continue;

						DataInputStream dis = region.getChunkInputStream(localX, localZ);
						if (dis == null) continue;

						NbtCompound chunkTag;
						try {
							chunkTag = NbtIo.read(dis);
							dis.close();
						} catch (IOException e) {
							LOGGER.error("Failed to read chunk {},{}", chunkX, chunkZ, e);
							continue;
						}

						processedChunks++;
						boolean modified = false;

						NbtCompound level = chunkTag.getCompound("Level");
						if (level == null) continue;

						// 1. Inject extended blocks from block sidecar
						if (chunkSidecarRoot != null) {
							modified |= injectExtendedBlocks(level, chunkX, chunkZ, chunkSidecarRoot);
						}

						// 2. Fix retroapi:id items in existing TileEntities and Entities
						modified |= fixRetroApiItems(level);

						// 3. Inject inventory sidecar data
						if (invSidecarRoot != null) {
							modified |= injectInventorySidecar(level, chunkX, chunkZ, invSidecarRoot);
						}

						if (modified) {
							modifiedChunks++;
							try (DataOutputStream dos = region.getChunkOutputStream(localX, localZ)) {
								NbtIo.write(chunkTag, dos);
							} catch (IOException e) {
								LOGGER.error("Failed to write chunk {},{}", chunkX, chunkZ, e);
							}
						}
					}
				}
			} finally {
				try {
					region.close();
				} catch (IOException e) {
					LOGGER.error("Failed to close region file {}", regionFile, e);
				}
			}
		}

		LOGGER.info("Sidecar processing complete: processed {} chunks, modified {}", processedChunks, modifiedChunks);
	}

	/**
	 * Fix retroapi:id tagged items in level.dat player inventory.
	 */
	public static void fixLevelDat(File worldDir) {
		File levelDat = new File(worldDir, "level.dat");
		if (!levelDat.exists()) return;

		try {
			NbtCompound root = NbtIo.readCompressed(new FileInputStream(levelDat));
			NbtCompound data = root.getCompound("Data");
			if (data == null) return;

			NbtCompound player = data.getCompound("Player");
			if (player == null) return;

			boolean modified = false;

			// Fix player inventory
			if (player.contains("Inventory")) {
				NbtList inventory = player.getList("Inventory");
				if (inventory != null) {
					modified |= fixItemList(inventory);
				}
			}

			if (modified) {
				LOGGER.info("Fixed retroapi:id items in level.dat player inventory");
				NbtIo.writeCompressed(root, new FileOutputStream(levelDat));
			}
		} catch (IOException e) {
			LOGGER.error("Failed to process level.dat", e);
		}
	}

	private static NbtCompound loadSidecarNbt(File file) {
		if (!file.exists()) return null;
		try (FileInputStream fis = new FileInputStream(file)) {
			return NbtIo.readCompressed(fis);
		} catch (IOException e) {
			LOGGER.error("Failed to load sidecar {}", file, e);
			return null;
		}
	}

	/**
	 * Inject extended blocks from the block sidecar into the converted chunk sections.
	 */
	private static boolean injectExtendedBlocks(NbtCompound level, int chunkX, int chunkZ,
												NbtCompound sidecarRoot) {
		if (!sidecarRoot.contains("chunks")) return false;
		NbtCompound chunks = sidecarRoot.getCompound("chunks");
		String chunkKey = chunkX + "," + chunkZ;
		if (!chunks.contains(chunkKey)) return false;

		NbtCompound chunkNbt = chunks.getCompound(chunkKey);
		byte[] posBytes = chunkNbt.getByteArray("positions");
		String idsJoined = chunkNbt.getString("ids");
		byte[] metadata = chunkNbt.getByteArray("metadata");

		if (posBytes.length == 0 || idsJoined.isEmpty()) return false;

		int[] positions = bytesToInts(posBytes);
		String[] ids = idsJoined.split("\0");
		if (positions.length != ids.length) {
			LOGGER.warn("Mismatched positions/ids for chunk {},{}", chunkX, chunkZ);
			return false;
		}

		// Group blocks by section
		Map<Integer, List<int[]>> blocksBySection = new HashMap<>();
		// int[]: {localX, localY, localZ, idIndex}
		for (int i = 0; i < positions.length; i++) {
			int index = positions[i];
			// ChunkExtendedBlocks index = x << 11 | z << 7 | y (same as vanilla chunk byte[] indexing)
			int worldY = index & 0x7F;
			int z = (index >> 7) & 0xF;
			int x = (index >> 11) & 0xF;
			int sectionY = worldY >> 4;
			int localY = worldY & 0xF;

			blocksBySection.computeIfAbsent(sectionY, k -> new ArrayList<>())
				.add(new int[]{x, localY, z, i});
		}

		if (blocksBySection.isEmpty()) return false;

		// Get or create sections list
		NbtList sections;
		if (level.contains(SECTIONS_KEY)) {
			sections = level.getList(SECTIONS_KEY);
		} else {
			sections = new NbtList();
			level.put(SECTIONS_KEY, sections);
		}

		// Index existing sections by Y
		Map<Integer, Integer> sectionIndexByY = new HashMap<>();
		for (int i = 0; i < sections.size(); i++) {
			NbtCompound section = (NbtCompound) sections.get(i);
			int y = section.getByte("y");
			sectionIndexByY.put(y, i);
		}

		boolean modified = false;

		for (Map.Entry<Integer, List<int[]>> entry : blocksBySection.entrySet()) {
			int sectionY = entry.getKey();
			List<int[]> blocks = entry.getValue();

			NbtCompound section;
			Integer existingIdx = sectionIndexByY.get(sectionY);

			if (existingIdx != null) {
				section = (NbtCompound) sections.get(existingIdx);
			} else {
				// Create a new section
				section = new NbtCompound();
				section.putByte("y", (byte) sectionY);
				sections.addElement(section);
			}

			// Get or create palette and block data
			NbtCompound blockStates;
			NbtList palette;
			long[] packedData;
			int currentBits;

			if (section.contains("block_states")) {
				blockStates = section.getCompound("block_states");
				palette = blockStates.getList("palette");
				packedData = ((StationNbtCompound) blockStates).getLongArray("data");
				currentBits = Math.max(4, ceilLog2(palette.size()));
			} else {
				blockStates = new NbtCompound();
				palette = new NbtList();
				// Add air as first palette entry
				NbtCompound air = new NbtCompound();
				air.putString("Name", "minecraft:air");
				palette.addElement(air);
				packedData = null;
				currentBits = 4;
				section.putCompound("block_states", blockStates);
			}

			// Build a map of existing palette entries for lookup
			Map<String, Integer> paletteMap = new HashMap<>();
			for (int i = 0; i < palette.size(); i++) {
				NbtCompound pe = (NbtCompound) palette.get(i);
				paletteMap.put(pe.getString("Name"), i);
			}

			// Decode existing packed data
			PackedIntegerArray array;
			if (packedData != null && packedData.length > 0) {
				array = new PackedIntegerArray(currentBits, 4096, packedData);
			} else {
				array = new PackedIntegerArray(4, 4096);
			}

			// Read current states into an int array so we can re-encode with new bit size
			int[] stateIndices = new int[4096];
			for (int i = 0; i < 4096; i++) {
				stateIndices[i] = array.get(i);
			}

			// Get or create metadata nibble array
			byte[] metaNibble;
			if (section.contains("data")) {
				metaNibble = section.getByteArray("data");
				if (metaNibble.length < 2048) {
					metaNibble = Arrays.copyOf(metaNibble, 2048);
				}
			} else {
				metaNibble = new byte[2048];
			}

			// Insert extended blocks
			for (int[] block : blocks) {
				int x = block[0], localY = block[1], z = block[2], idIdx = block[3];
				String blockId = ids[idIdx];
				int meta = (idIdx < metadata.length) ? (metadata[idIdx] & 0xFF) : 0;

				// Add to palette if not present
				int paletteIdx;
				if (paletteMap.containsKey(blockId)) {
					paletteIdx = paletteMap.get(blockId);
				} else {
					NbtCompound pe = new NbtCompound();
					pe.putString("Name", blockId);
					palette.addElement(pe);
					paletteIdx = palette.size() - 1;
					paletteMap.put(blockId, paletteIdx);
				}

				// Section index: (y << 4 | z) << 4 | x
				int sectionIdx = (localY << 4 | z) << 4 | x;
				stateIndices[sectionIdx] = paletteIdx;

				// Metadata nibble: x << 8 | y << 4 | z
				int nibbleIdx = x << 8 | localY << 4 | z;
				int byteIdx = nibbleIdx >> 1;
				if ((nibbleIdx & 1) == 0) {
					metaNibble[byteIdx] = (byte) ((metaNibble[byteIdx] & 0xF0) | (meta & 0x0F));
				} else {
					metaNibble[byteIdx] = (byte) ((metaNibble[byteIdx] & 0x0F) | ((meta & 0x0F) << 4));
				}
			}

			// Re-encode with potentially new bit size
			int newBits = Math.max(4, ceilLog2(palette.size()));
			PackedIntegerArray newArray = new PackedIntegerArray(newBits, 4096, stateIndices);

			blockStates.put("palette", palette);
			((StationNbtCompound) blockStates).put("data", newArray.getData());
			section.putByteArray("data", metaNibble);

			modified = true;
		}

		return modified;
	}

	/**
	 * Fix retroapi:id tagged items in TileEntities and Entities within a chunk.
	 */
	private static boolean fixRetroApiItems(NbtCompound level) {
		boolean modified = false;

		// Fix items in TileEntities
		if (level.contains("TileEntities")) {
			NbtList tileEntities = level.getList("TileEntities");
			if (tileEntities != null) {
				for (int i = 0; i < tileEntities.size(); i++) {
					NbtCompound te = (NbtCompound) tileEntities.get(i);
					if (te.contains("Items")) {
						modified |= fixItemList(te.getList("Items"));
					}
				}
			}
		}

		// Fix item entities
		if (level.contains("Entities")) {
			NbtList entities = level.getList("Entities");
			if (entities != null) {
				for (int i = 0; i < entities.size(); i++) {
					NbtCompound entity = (NbtCompound) entities.get(i);
					if ("Item".equals(entity.getString("id")) && entity.contains("Item")) {
						NbtCompound item = entity.getCompound("Item");
						if (item != null) {
							modified |= fixSingleItem(item);
						}
					}
				}
			}
		}

		return modified;
	}

	/**
	 * Fix all retroapi:id tagged items in an NbtList of item stacks.
	 */
	private static boolean fixItemList(NbtList items) {
		boolean modified = false;
		for (int i = 0; i < items.size(); i++) {
			NbtCompound item = (NbtCompound) items.get(i);
			modified |= fixSingleItem(item);
		}
		return modified;
	}

	/**
	 * Fix a single item stack: convert retroapi:id to stationapi:id and restore count/damage.
	 */
	private static boolean fixSingleItem(NbtCompound item) {
		if (!item.contains("retroapi:id")) return false;

		String retroId = item.getString("retroapi:id");
		// Set stationapi:id to the retroapi identifier
		item.putString(STATION_ID, retroId);

		// Restore count from retroapi:count if present
		if (item.contains("retroapi:count")) {
			item.putByte("Count", item.getByte("retroapi:count"));
		}

		// Restore damage from retroapi:damage if present
		if (item.contains("retroapi:damage")) {
			item.putShort("Damage", item.getShort("retroapi:damage"));
		}

		// Note: retroapi:* tags are left in place since NbtCompound has no remove()
		// in b1.7.3. They are harmless dead data as ItemStackMixin is disabled with StationAPI.
		return true;
	}

	/**
	 * Inject inventory sidecar data (block entities, inventory items, item entities) into the chunk.
	 */
	private static boolean injectInventorySidecar(NbtCompound level, int chunkX, int chunkZ,
												  NbtCompound sidecarRoot) {
		if (!sidecarRoot.contains("chunks")) return false;
		NbtCompound chunks = sidecarRoot.getCompound("chunks");
		String chunkKey = chunkX + "," + chunkZ;
		if (!chunks.contains(chunkKey)) return false;

		NbtCompound chunkData = chunks.getCompound(chunkKey);
		boolean modified = false;

		// 1. Inject modded block entities
		if (chunkData.contains("blockEntities")) {
			NbtList moddedBEs = chunkData.getList("blockEntities");
			NbtList tileEntities = level.contains("TileEntities") ? level.getList("TileEntities") : new NbtList();

			for (int i = 0; i < moddedBEs.size(); i++) {
				NbtCompound be = (NbtCompound) moddedBEs.get(i);
				// Convert any retroapi:id items within the block entity's inventory
				if (be.contains("Items")) {
					fixItemList(be.getList("Items"));
				}
				tileEntities.addElement(be);
			}

			level.put("TileEntities", tileEntities);
			modified = true;
			LOGGER.debug("Injected {} modded block entities into chunk {},{}", moddedBEs.size(), chunkX, chunkZ);
		}

		// 2. Inject modded items into existing block entity inventories
		if (chunkData.contains("inventoryItems")) {
			NbtList inventoryItems = chunkData.getList("inventoryItems");
			NbtList tileEntities = level.contains("TileEntities") ? level.getList("TileEntities") : new NbtList();

			// Index TileEntities by position
			Map<String, NbtCompound> teByPos = new HashMap<>();
			for (int i = 0; i < tileEntities.size(); i++) {
				NbtCompound te = (NbtCompound) tileEntities.get(i);
				String posKey = te.getInt("x") + "," + te.getInt("y") + "," + te.getInt("z");
				teByPos.put(posKey, te);
			}

			for (int i = 0; i < inventoryItems.size(); i++) {
				NbtCompound entry = (NbtCompound) inventoryItems.get(i);
				String retroId = entry.getString("retroapi:id");
				int count = entry.getByte("Count") & 0xFF;
				int damage = entry.getShort("Damage");
				int slot = entry.getByte("Slot") & 0xFF;

				// Create the item in stationapi format
				NbtCompound item = new NbtCompound();
				item.putString(STATION_ID, retroId);
				item.putByte("Count", (byte) count);
				item.putShort("Damage", (short) damage);
				item.putByte("Slot", (byte) slot);

				// Find the target block entity
				String posKey = entry.getInt("x") + "," + entry.getInt("y") + "," + entry.getInt("z");
				NbtCompound targetTE = teByPos.get(posKey);
				if (targetTE != null && targetTE.contains("Items")) {
					targetTE.getList("Items").addElement(item);
				} else {
					LOGGER.warn("No block entity found at {} for inventory item {}", posKey, retroId);
				}
			}

			modified = true;
			LOGGER.debug("Injected {} modded inventory items into chunk {},{}", inventoryItems.size(), chunkX, chunkZ);
		}

		// 3. Inject modded item entities
		if (chunkData.contains("itemEntities")) {
			NbtList moddedEntities = chunkData.getList("itemEntities");
			NbtList entities = level.contains("Entities") ? level.getList("Entities") : new NbtList();

			for (int i = 0; i < moddedEntities.size(); i++) {
				NbtCompound entity = (NbtCompound) moddedEntities.get(i);
				// Fix the Item compound to use stationapi:id
				if (entity.contains("Item")) {
					NbtCompound itemTag = entity.getCompound("Item");
					if (itemTag != null) {
						fixSingleItem(itemTag);
					}
				}
				entities.addElement(entity);
			}

			level.put("Entities", entities);
			modified = true;
			LOGGER.debug("Injected {} modded item entities into chunk {},{}", moddedEntities.size(), chunkX, chunkZ);
		}

		return modified;
	}

	/**
	 * Reconstruct block sidecars from StationAPI's flattened section format.
	 * Must run BEFORE the damager converts sections to byte[], so we can read palette entries.
	 */
	public static void reconstructBlockSidecarsFromFlattened(File worldDir) {
		IdMap idMap = new IdMap();
		File idMapFile = new File(worldDir, "retroapi/id_map.dat");
		if (!idMapFile.exists()) {
			LOGGER.warn("No id_map.dat found, cannot reconstruct block sidecars");
			return;
		}
		idMap.load(idMapFile);

		// Build set of RetroAPI block identifiers
		Set<String> retroBlockIds = new HashSet<>();
		for (NamespacedIdentifier id : idMap.getBlockIds().keySet()) {
			retroBlockIds.add(id.toString());
		}

		if (retroBlockIds.isEmpty()) {
			LOGGER.info("No RetroAPI blocks in id_map, skipping block sidecar reconstruction");
			return;
		}

		LOGGER.info("Reconstructing block sidecars from flattened world (known blocks: {})", retroBlockIds.size());

		File regionDir = new File(worldDir, "region");
		if (!regionDir.exists()) return;

		File[] regionFiles = regionDir.listFiles((dir, name) -> name.endsWith(".mcr"));
		if (regionFiles == null) return;

		for (File regionFile : regionFiles) {
			String name = regionFile.getName();
			String[] parts = name.split("\\.");
			if (parts.length != 4) continue;
			int regionX, regionZ;
			try {
				regionX = Integer.parseInt(parts[1]);
				regionZ = Integer.parseInt(parts[2]);
			} catch (NumberFormatException e) {
				continue;
			}

			NbtCompound sidecarRoot = new NbtCompound();
			sidecarRoot.putInt("version", 1);
			NbtCompound sidecarChunks = new NbtCompound();
			boolean hasData = false;

			RegionFile region = new RegionFile(regionFile);
			try {
				for (int localX = 0; localX < 32; localX++) {
					for (int localZ = 0; localZ < 32; localZ++) {
						if (!region.hasChunkData(localX, localZ)) continue;

						DataInputStream dis = region.getChunkInputStream(localX, localZ);
						if (dis == null) continue;

						NbtCompound chunkTag;
						try {
							chunkTag = NbtIo.read(dis);
							dis.close();
						} catch (IOException e) {
							continue;
						}

						NbtCompound level = chunkTag.getCompound("Level");
						if (level == null || !level.contains(SECTIONS_KEY)) continue;

						int chunkX = level.getInt("xPos");
						int chunkZ = level.getInt("zPos");

						List<Integer> positions = new ArrayList<>();
						List<String> ids = new ArrayList<>();
						List<Integer> metas = new ArrayList<>();

						NbtList sections = level.getList(SECTIONS_KEY);
						for (int s = 0; s < sections.size(); s++) {
							NbtCompound section = (NbtCompound) sections.get(s);
							int sectionY = section.getByte("y");

							if (!section.contains("block_states")) continue;
							NbtCompound blockStates = section.getCompound("block_states");
							if (!blockStates.contains("palette")) continue;

							NbtList palette = blockStates.getList("palette");

							// Check which palette entries are RetroAPI blocks
							Map<Integer, String> retroPaletteIndices = new HashMap<>();
							for (int p = 0; p < palette.size(); p++) {
								NbtCompound pe = (NbtCompound) palette.get(p);
								String blockName = pe.getString("Name");
								if (retroBlockIds.contains(blockName)) {
									retroPaletteIndices.put(p, blockName);
								}
							}

							if (retroPaletteIndices.isEmpty()) continue;

							// Decode packed data
							long[] packedData = ((StationNbtCompound) blockStates).getLongArray("data");
							if (packedData == null || packedData.length == 0) {
								// Single-entry palette, all 4096 blocks are that entry
								if (palette.size() == 1 && retroPaletteIndices.containsKey(0)) {
									String blockId = retroPaletteIndices.get(0);
									for (int idx = 0; idx < 4096; idx++) {
										int x = idx & 0xF;
										int z = (idx >> 4) & 0xF;
										int localY = (idx >> 8) & 0xF;
										int worldY = sectionY * 16 + localY;
										int chunkIndex = (x << 11) | (z << 7) | worldY;
										positions.add(chunkIndex);
										ids.add(blockId);
										metas.add(0);
									}
								}
								continue;
							}

							int bits = Math.max(4, ceilLog2(palette.size()));
							PackedIntegerArray array = new PackedIntegerArray(bits, 4096, packedData);

							// Read metadata nibble array
							byte[] metaNibble = section.contains("data") ? section.getByteArray("data") : null;

							for (int idx = 0; idx < 4096; idx++) {
								int paletteIdx = array.get(idx);
								if (retroPaletteIndices.containsKey(paletteIdx)) {
									String blockId = retroPaletteIndices.get(paletteIdx);
									// Section index: (y << 4 | z) << 4 | x
									int x = idx & 0xF;
									int z = (idx >> 4) & 0xF;
									int localY = (idx >> 8) & 0xF;
									int worldY = sectionY * 16 + localY;
									int chunkIndex = (x << 11) | (z << 7) | worldY;

									// Get metadata from nibble array
									int meta = 0;
									if (metaNibble != null && metaNibble.length >= 2048) {
										int nibbleIdx = x << 8 | localY << 4 | z;
										int byteIdx = nibbleIdx >> 1;
										if ((nibbleIdx & 1) == 0) {
											meta = metaNibble[byteIdx] & 0x0F;
										} else {
											meta = (metaNibble[byteIdx] >> 4) & 0x0F;
										}
									}

									positions.add(chunkIndex);
									ids.add(blockId);
									metas.add(meta);
								}
							}
						}

						if (!positions.isEmpty()) {
							NbtCompound chunkNbt = new NbtCompound();
							int[] posArray = new int[positions.size()];
							byte[] metaArray = new byte[positions.size()];
							StringBuilder idsBuilder = new StringBuilder();

							for (int i = 0; i < positions.size(); i++) {
								posArray[i] = positions.get(i);
								if (i > 0) idsBuilder.append('\0');
								idsBuilder.append(ids.get(i));
								metaArray[i] = metas.get(i).byteValue();
							}

							chunkNbt.putByteArray("positions", intsToBytes(posArray));
							chunkNbt.putString("ids", idsBuilder.toString());
							chunkNbt.putByteArray("metadata", metaArray);

							String chunkKey = chunkX + "," + chunkZ;
							sidecarChunks.putCompound(chunkKey, chunkNbt);
							hasData = true;
						}
					}
				}
			} finally {
				try { region.close(); } catch (IOException e) { /* ignore */ }
			}

			if (hasData) {
				sidecarRoot.putCompound("chunks", sidecarChunks);
				File sidecarFile = new File(worldDir, "retroapi/chunks/r." + regionX + "." + regionZ + ".dat");
				sidecarFile.getParentFile().mkdirs();
				try (FileOutputStream fos = new FileOutputStream(sidecarFile)) {
					NbtIo.writeCompressed(sidecarRoot, fos);
					LOGGER.info("Reconstructed block sidecar for region {},{}", regionX, regionZ);
				} catch (IOException e) {
					LOGGER.error("Failed to write block sidecar for region {},{}", regionX, regionZ, e);
				}
			}
		}
	}

	/**
	 * Reconstruct inventory sidecars from vanilla-format chunks after the damager has run.
	 * Strips modded block entities, RetroAPI items, and item entities from vanilla chunks.
	 */
	public static void reconstructInventorySidecarsFromVanilla(File worldDir) {
		IdMap idMap = new IdMap();
		File idMapFile = new File(worldDir, "retroapi/id_map.dat");
		if (!idMapFile.exists()) {
			LOGGER.warn("No id_map.dat found, cannot reconstruct inventory sidecars");
			return;
		}
		idMap.load(idMapFile);

		// Build reverse maps: numeric ID → string ID
		Map<Integer, String> reverseBlockMap = new HashMap<>();
		for (Map.Entry<NamespacedIdentifier, Integer> entry : idMap.getBlockIds().entrySet()) {
			reverseBlockMap.put(entry.getValue(), entry.getKey().toString());
		}
		Map<Integer, String> reverseItemMap = new HashMap<>();
		for (Map.Entry<NamespacedIdentifier, Integer> entry : idMap.getItemIds().entrySet()) {
			reverseItemMap.put(entry.getValue(), entry.getKey().toString());
		}

		LOGGER.info("Reconstructing inventory sidecars from vanilla chunks (blocks: {}, items: {})",
			reverseBlockMap.size(), reverseItemMap.size());

		File regionDir = new File(worldDir, "region");
		if (!regionDir.exists()) return;

		File[] regionFiles = regionDir.listFiles((dir, name) -> name.endsWith(".mcr"));
		if (regionFiles == null) return;

		for (File regionFile : regionFiles) {
			String name = regionFile.getName();
			String[] parts = name.split("\\.");
			if (parts.length != 4) continue;
			int regionX, regionZ;
			try {
				regionX = Integer.parseInt(parts[1]);
				regionZ = Integer.parseInt(parts[2]);
			} catch (NumberFormatException e) {
				continue;
			}

			// Load block sidecar to identify modded block positions
			File blockSidecarFile = new File(worldDir, "retroapi/chunks/r." + regionX + "." + regionZ + ".dat");
			NbtCompound blockSidecarRoot = loadSidecarNbt(blockSidecarFile);

			NbtCompound invSidecarRoot = new NbtCompound();
			invSidecarRoot.putInt("version", 1);
			NbtCompound invSidecarChunks = new NbtCompound();
			boolean hasData = false;

			RegionFile region = new RegionFile(regionFile);
			try {
				for (int localX = 0; localX < 32; localX++) {
					for (int localZ = 0; localZ < 32; localZ++) {
						if (!region.hasChunkData(localX, localZ)) continue;

						DataInputStream dis = region.getChunkInputStream(localX, localZ);
						if (dis == null) continue;

						NbtCompound chunkTag;
						try {
							chunkTag = NbtIo.read(dis);
							dis.close();
						} catch (IOException e) {
							continue;
						}

						NbtCompound level = chunkTag.getCompound("Level");
						if (level == null) continue;

						int chunkX = level.getInt("xPos");
						int chunkZ = level.getInt("zPos");
						String chunkKey = chunkX + "," + chunkZ;

						// Get modded block positions from block sidecar to identify modded BEs
						Set<String> moddedPositions = new HashSet<>();
						if (blockSidecarRoot != null && blockSidecarRoot.contains("chunks")) {
							NbtCompound bsChunks = blockSidecarRoot.getCompound("chunks");
							if (bsChunks.contains(chunkKey)) {
								NbtCompound bsChunk = bsChunks.getCompound(chunkKey);
								byte[] posBytes = bsChunk.getByteArray("positions");
								int[] blockPositions = bytesToInts(posBytes);
								for (int pos : blockPositions) {
									int x = ChunkExtendedBlocks.indexToX(pos);
									int y = ChunkExtendedBlocks.indexToY(pos);
									int z = ChunkExtendedBlocks.indexToZ(pos);
									int worldX = chunkX * 16 + x;
									int worldZ = chunkZ * 16 + z;
									moddedPositions.add(worldX + "," + y + "," + worldZ);
								}
							}
						}

						NbtCompound chunkData = new NbtCompound();
						boolean chunkModified = false;

						// 1. Filter TileEntities
						if (level.contains("TileEntities")) {
							NbtList tileEntities = level.getList("TileEntities");
							NbtList filteredTEs = new NbtList();
							NbtList moddedBEs = new NbtList();
							NbtList inventoryItems = new NbtList();

							for (int i = 0; i < tileEntities.size(); i++) {
								NbtCompound be = (NbtCompound) tileEntities.get(i);
								int bx = be.getInt("x");
								int by = be.getInt("y");
								int bz = be.getInt("z");
								String posKey = bx + "," + by + "," + bz;

								if (moddedPositions.contains(posKey)) {
									// Modded block entity - convert vanilla stationapi:id items to
									// numeric IDs, then tag RetroAPI items with retroapi:id, and strip
									if (be.contains("Items")) {
										convertStationApiItemsToNumeric(be.getList("Items"));
										tagItemsWithRetroId(be.getList("Items"), reverseBlockMap, reverseItemMap);
									}
									moddedBEs.addElement(be);
								} else {
									// Vanilla block entity - check for RetroAPI items in inventory
									if (be.contains("Items")) {
										NbtList items = be.getList("Items");
										NbtList filteredItems = new NbtList();

										for (int j = 0; j < items.size(); j++) {
											NbtCompound item = (NbtCompound) items.get(j);
											String retroId = resolveRetroId(item, reverseBlockMap, reverseItemMap);
											if (retroId != null) {
												NbtCompound invEntry = new NbtCompound();
												invEntry.putInt("x", bx);
												invEntry.putInt("y", by);
												invEntry.putInt("z", bz);
												invEntry.putByte("Slot", item.getByte("Slot"));
												invEntry.putString("retroapi:id", retroId);
												invEntry.putByte("Count", item.getByte("Count"));
												invEntry.putShort("Damage", item.getShort("Damage"));
												inventoryItems.addElement(invEntry);
											} else {
												filteredItems.addElement(item);
											}
										}

										be.put("Items", filteredItems);
									}
									filteredTEs.addElement(be);
								}
							}

							if (moddedBEs.size() > 0 || inventoryItems.size() > 0) {
								level.put("TileEntities", filteredTEs);
								if (moddedBEs.size() > 0) chunkData.put("blockEntities", moddedBEs);
								if (inventoryItems.size() > 0) chunkData.put("inventoryItems", inventoryItems);
								chunkModified = true;
							}
						}

						// 2. Filter Entities - strip item entities carrying RetroAPI items
						if (level.contains("Entities")) {
							NbtList entities = level.getList("Entities");
							NbtList filteredEntities = new NbtList();
							NbtList moddedItemEntities = new NbtList();

							for (int i = 0; i < entities.size(); i++) {
								NbtCompound entity = (NbtCompound) entities.get(i);
								if ("Item".equals(entity.getString("id"))) {
									NbtCompound itemTag = entity.getCompound("Item");
									if (itemTag != null) {
										String retroId = resolveRetroId(itemTag, reverseBlockMap, reverseItemMap);
										if (retroId != null) {
											// Tag the item for sidecar
											itemTag.putString("retroapi:id", retroId);
											itemTag.putByte("retroapi:count", itemTag.getByte("Count"));
											itemTag.putShort("retroapi:damage", itemTag.getShort("Damage"));
											itemTag.putShort("id", (short) 0);
											itemTag.putByte("Count", (byte) 0);
											moddedItemEntities.addElement(entity);
											continue;
										}
									}
								}
								filteredEntities.addElement(entity);
							}

							if (moddedItemEntities.size() > 0) {
								level.put("Entities", filteredEntities);
								chunkData.put("itemEntities", moddedItemEntities);
								chunkModified = true;
							}
						}

						if (chunkModified) {
							invSidecarChunks.putCompound(chunkKey, chunkData);
							hasData = true;

							// Write modified chunk back
							try (DataOutputStream dos = region.getChunkOutputStream(localX, localZ)) {
								NbtIo.write(chunkTag, dos);
							} catch (IOException e) {
								LOGGER.error("Failed to write chunk {},{}", chunkX, chunkZ, e);
							}
						}
					}
				}
			} finally {
				try { region.close(); } catch (IOException e) { /* ignore */ }
			}

			if (hasData) {
				invSidecarRoot.putCompound("chunks", invSidecarChunks);
				File invSidecarFile = new File(worldDir, "retroapi/inventories/r." + regionX + "." + regionZ + ".dat");
				invSidecarFile.getParentFile().mkdirs();
				try (FileOutputStream fos = new FileOutputStream(invSidecarFile)) {
					NbtIo.writeCompressed(invSidecarRoot, fos);
					LOGGER.info("Reconstructed inventory sidecar for region {},{}", regionX, regionZ);
				} catch (IOException e) {
					LOGGER.error("Failed to write inventory sidecar for region {},{}", regionX, regionZ, e);
				}
			}
		}
	}

	/**
	 * Fix player inventory in level.dat for RetroAPI format after un-conversion.
	 * Tags RetroAPI items with retroapi:id and clamps their vanilla fields.
	 */
	public static void fixLevelDatForRetroApi(File worldDir) {
		File levelDat = new File(worldDir, "level.dat");
		if (!levelDat.exists()) return;

		IdMap idMap = new IdMap();
		File idMapFile = new File(worldDir, "retroapi/id_map.dat");
		if (!idMapFile.exists()) return;
		idMap.load(idMapFile);

		Map<Integer, String> reverseBlockMap = new HashMap<>();
		for (Map.Entry<NamespacedIdentifier, Integer> entry : idMap.getBlockIds().entrySet()) {
			reverseBlockMap.put(entry.getValue(), entry.getKey().toString());
		}
		Map<Integer, String> reverseItemMap = new HashMap<>();
		for (Map.Entry<NamespacedIdentifier, Integer> entry : idMap.getItemIds().entrySet()) {
			reverseItemMap.put(entry.getValue(), entry.getKey().toString());
		}

		try {
			NbtCompound root = NbtIo.readCompressed(new FileInputStream(levelDat));
			NbtCompound data = root.getCompound("Data");
			if (data == null) return;

			NbtCompound player = data.getCompound("Player");
			if (player == null) return;

			boolean modified = false;

			if (player.contains("Inventory")) {
				NbtList inventory = player.getList("Inventory");
				if (inventory != null) {
					modified |= tagItemsWithRetroId(inventory, reverseBlockMap, reverseItemMap);
				}
			}

			if (modified) {
				LOGGER.info("Fixed player inventory in level.dat for RetroAPI");
				NbtIo.writeCompressed(root, new FileOutputStream(levelDat));
			}
		} catch (IOException e) {
			LOGGER.error("Failed to process level.dat for RetroAPI", e);
		}
	}

	/**
	 * Resolve a RetroAPI string ID from an item's NBT, checking stationapi:id first,
	 * then falling back to numeric ID lookup via the reverse map.
	 */
	private static String resolveRetroId(NbtCompound item, Map<Integer, String> reverseBlockMap,
										  Map<Integer, String> reverseItemMap) {
		// Check stationapi:id first (damager may have left it)
		if (item.contains(STATION_ID)) {
			String stationId = item.getString(STATION_ID);
			if (!stationId.isEmpty() && !stationId.startsWith("minecraft:")) {
				return stationId;
			}
		}
		// Check retroapi:id (already tagged)
		if (item.contains("retroapi:id")) {
			return item.getString("retroapi:id");
		}
		// Fall back to numeric ID lookup
		int numericId = item.getShort("id");
		String retroId = reverseBlockMap.get(numericId);
		if (retroId != null) return retroId;
		return reverseItemMap.get(numericId);
	}

	/**
	 * Tag all RetroAPI items in a list with retroapi:id and clamp vanilla fields.
	 * Returns true if any items were tagged.
	 */
	private static boolean tagItemsWithRetroId(NbtList items, Map<Integer, String> reverseBlockMap,
												Map<Integer, String> reverseItemMap) {
		boolean modified = false;
		for (int i = 0; i < items.size(); i++) {
			NbtCompound item = (NbtCompound) items.get(i);
			String retroId = resolveRetroId(item, reverseBlockMap, reverseItemMap);
			if (retroId != null) {
				item.putString("retroapi:id", retroId);
				item.putByte("retroapi:count", item.getByte("Count"));
				item.putShort("retroapi:damage", item.getShort("Damage"));
				item.putShort("id", (short) 0);
				item.putByte("Count", (byte) 0);
				modified = true;
			}
		}
		return modified;
	}

	/**
	 * Convert stationapi:id items back to numeric IDs using StationAPI's reverse lookup.
	 * This is needed for vanilla items inside modded BEs (e.g. water bucket in a freezer)
	 * that the damager didn't process. Without this, they'd have id=0 causing NPE.
	 */
	private static void convertStationApiItemsToNumeric(NbtList items) {
		for (int i = 0; i < items.size(); i++) {
			NbtCompound item = (NbtCompound) items.get(i);
			if (item.contains(STATION_ID)) {
				String stationId = item.getString(STATION_ID);
				if (stationId.startsWith("minecraft:")) {
					int numericId = StationFlatteningItemStackSchema.lookupOldItemId(stationId);
					if (numericId != 0) {
						item.putShort("id", (short) numericId);
					}
				}
			}
		}
	}

	private static int ceilLog2(int n) {
		if (n <= 1) return 0;
		return 32 - Integer.numberOfLeadingZeros(n - 1);
	}

	private static int[] bytesToInts(byte[] bytes) {
		ByteBuffer buf = ByteBuffer.wrap(bytes);
		int[] ints = new int[bytes.length / 4];
		for (int i = 0; i < ints.length; i++) {
			ints[i] = buf.getInt();
		}
		return ints;
	}

	private static byte[] intsToBytes(int[] ints) {
		ByteBuffer buf = ByteBuffer.allocate(ints.length * 4);
		for (int v : ints) {
			buf.putInt(v);
		}
		return buf.array();
	}
}
