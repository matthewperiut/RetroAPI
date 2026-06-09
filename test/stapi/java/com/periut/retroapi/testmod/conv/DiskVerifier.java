package com.periut.retroapi.testmod.conv;

import com.periut.retroapi.storage.ChunkExtendedBlocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.world.chunk.storage.RegionFile;
import net.modificationstation.stationapi.api.nbt.StationNbtCompound;
import net.modificationstation.stationapi.api.util.collection.PackedIntegerArray;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads a world straight off disk and asserts that every piece of manifest content survived a
 * conversion - in whichever on-disk form that stage of the round-trip should hold it:
 *
 * <ul>
 *   <li><b>McRegion + sidecar</b> (after populate, after reverse): modded blocks live in the
 *       {@code retroapi/chunks} sidecar, modded inventory items / dropped items / full entities in the
 *       {@code retroapi/inventories} sidecar (the vanilla chunk byte array shows air/empty).</li>
 *   <li><b>StationAPI flattened</b> (after forward): modded blocks live in the {@code stationapi:sections}
 *       palettes, modded items in the chunk's {@code TileEntities}/{@code Entities} as {@code stationapi:id}.</li>
 * </ul>
 *
 * Each category is reported independently so a partial datafixer (e.g. blocks survive but modded mobs
 * don't) produces a precise diagnosis rather than a single opaque failure.
 */
public final class DiskVerifier {

	/** Accumulates per-category verdicts so the round-trip leaves a precise audit trail. */
	public static final class Report {
		public final List<String> lines = new ArrayList<>();
		private boolean ok = true;
		public void check(String category, boolean pass, String detail) {
			ok &= pass;
			lines.add(String.format("  [%s] %-22s %s", pass ? "PASS" : "FAIL", category, detail == null ? "" : detail));
		}
		/** A check that is recorded but does NOT affect the gate (known-limitation coverage). */
		public void note(String category, boolean pass, String detail) {
			lines.add(String.format("  [%s] %-22s %s", pass ? "PASS" : "KNOWN", category, detail == null ? "" : detail));
		}
		public boolean pass() { return ok; }
	}

	private DiskVerifier() {}

	// ----------------------------------------------------------------- McRegion + sidecar --------

	public static Report verifyMcRegion(File worldDir, ConvManifest m) {
		Report r = new Report();

		// Blocks: each must appear in the block sidecar with the right id + metadata.
		for (ConvManifest.BlockEntry b : m.blocks) {
			r.check("block:" + shortId(b.id), sidecarHasBlock(worldDir, "retroapi", b), b.id + " @ " + xyz(b.x, b.y, b.z));
		}

		// Chest items: modded items are stripped to the inventory sidecar's inventoryItems list.
		for (ConvManifest.ItemEntry it : m.chestItems) {
			if (!it.modded) continue; // vanilla items stay in the vanilla chunk; not a sidecar concern
			boolean[] found = sidecarChestItem(worldDir, "retroapi", m, it);
			r.check("chestItem:" + shortId(it.id), found[0], "slot " + it.slot + " x" + it.count
				+ (it.component >= 0 ? (found[1] ? " (+component)" : " (component LOST)") : ""));
			if (it.component >= 0) r.check("chestComp:" + shortId(it.id), found[1], "component " + it.component);
		}

		// Dropped item entities: in the inventory sidecar's itemEntities list.
		for (ConvManifest.ItemEntry it : m.itemEntities) {
			r.check("itemEntity:" + shortId(it.id), sidecarHasItemEntity(worldDir, "retroapi", it),
				it.id + " x" + it.count);
		}

		// Full modded entities (the zeve): in the inventory sidecar's fullEntities list.
		for (String id : m.entities) {
			r.check("fullEntity:" + shortId(id), sidecarHasFullEntity(worldDir, "retroapi", id), id);
		}

		// Modded block entity (crate): in the inventory sidecar's blockEntities list, with its item.
		if (m.hasCrate) {
			boolean[] c = sidecarHasCrate(worldDir, m);
			r.check("blockEntity:" + shortId(m.crateId), c[0], m.crateId + " holding " + m.crateItem);
			r.check("beComp:" + shortId(m.crateItem), c[1], "component " + m.crateComponent);
		}

		// Dimension content (DIM<n> sidecar).
		if (m.dimBlock != null) {
			String dimNs = "retroapi/DIM" + m.dimId;
			r.check("dimBlock:" + shortId(m.dimBlock.id), sidecarHasBlock(worldDir, dimNs, m.dimBlock),
				m.dimBlock.id + " in DIM" + m.dimId);
			if (m.dimEntity != null) {
				r.check("dimEntity:" + shortId(m.dimEntity), sidecarHasFullEntity(worldDir, dimNs, m.dimEntity),
					m.dimEntity + " in DIM" + m.dimId);
			}
		}
		return r;
	}

	private static boolean sidecarHasBlock(File worldDir, String ns, ConvManifest.BlockEntry b) {
		int cx = b.x >> 4, cz = b.z >> 4;
		NbtCompound chunk = sidecarChunk(new File(worldDir, ns + "/chunks/r." + (cx >> 5) + "." + (cz >> 5) + ".dat"), cx, cz);
		if (chunk == null) return false;
		int wantIndex = ChunkExtendedBlocks.toIndex(b.x & 15, b.y, b.z & 15);
		int[] positions = bytesToInts(chunk.getByteArray("positions"));
		String[] ids = decodeSidecarIds(chunk);
		byte[] metadata = chunk.getByteArray("metadata");
		for (int i = 0; i < positions.length && i < ids.length; i++) {
			if (positions[i] == wantIndex) {
				int meta = i < metadata.length ? (metadata[i] & 0xFF) : 0;
				if (!b.id.equals(ids[i]) || meta != b.meta) return false;
				// Flattened-state block: the high state bits live in the sidecar's xmeta section and
				// must have survived the round-trip, so the full RetroAPI state index is recoverable.
				if (b.state >= 0) {
					int fullState = meta | (sidecarXmeta(chunk, wantIndex) << 4);
					return fullState == b.state;
				}
				return true;
			}
		}
		return false;
	}

	/** @return [found, componentOk]. */
	private static boolean[] sidecarChestItem(File worldDir, String ns, ConvManifest m, ConvManifest.ItemEntry it) {
		int cx = m.chestX >> 4, cz = m.chestZ >> 4;
		NbtCompound chunk = sidecarChunk(new File(worldDir, ns + "/inventories/r." + (cx >> 5) + "." + (cz >> 5) + ".dat"), cx, cz);
		if (chunk == null || !chunk.contains("inventoryItems")) return new boolean[]{false, false};
		NbtList items = chunk.getList("inventoryItems");
		for (int i = 0; i < items.size(); i++) {
			NbtCompound e = (NbtCompound) items.get(i);
			if (e.getInt("x") == m.chestX && e.getInt("y") == m.chestY && e.getInt("z") == m.chestZ
				&& (e.getByte("Slot") & 0xFF) == it.slot && it.id.equals(e.getString("retroapi:id"))) {
				boolean compOk = it.component < 0 || componentMatches(e, it.component);
				return new boolean[]{(e.getByte("Count") & 0xFF) == it.count, compOk};
			}
		}
		return new boolean[]{false, false};
	}

	private static boolean sidecarHasItemEntity(File worldDir, String ns, ConvManifest.ItemEntry it) {
		for (File f : sidecarFiles(new File(worldDir, ns + "/inventories"))) {
			NbtCompound root = readCompressed(f);
			if (root == null || !root.contains("chunks")) continue;
			NbtCompound chunks = root.getCompound("chunks");
			for (String key : nbtKeys(chunks)) {
				NbtCompound chunk = chunks.getCompound(key);
				if (!chunk.contains("itemEntities")) continue;
				NbtList list = chunk.getList("itemEntities");
				for (int i = 0; i < list.size(); i++) {
					NbtCompound ent = (NbtCompound) list.get(i);
					NbtCompound item = ent.getCompound("Item");
					if (item != null && it.id.equals(item.getString("retroapi:id"))) {
						int count = item.contains("retroapi:count") ? (item.getByte("retroapi:count") & 0xFF) : (item.getByte("Count") & 0xFF);
						if (count == it.count) return true;
					}
				}
			}
		}
		return false;
	}

	/** @return [crateWithItemFound, componentOk] from the inventory sidecar's blockEntities list. */
	private static boolean[] sidecarHasCrate(File worldDir, ConvManifest m) {
		int cx = m.crateX >> 4, cz = m.crateZ >> 4;
		NbtCompound chunk = sidecarChunk(new File(worldDir, "retroapi/inventories/r." + (cx >> 5) + "." + (cz >> 5) + ".dat"), cx, cz);
		if (chunk == null || !chunk.contains("blockEntities")) return new boolean[]{false, false};
		NbtList bes = chunk.getList("blockEntities");
		for (int i = 0; i < bes.size(); i++) {
			NbtCompound be = (NbtCompound) bes.get(i);
			if (!m.crateId.equals(be.getString("id")) || be.getInt("x") != m.crateX
				|| be.getInt("y") != m.crateY || be.getInt("z") != m.crateZ) continue;
			return crateItemMatch(be, m, "retroapi:id");
		}
		return new boolean[]{false, false};
	}

	private static boolean[] flattenedCrate(NbtCompound crateTe, ConvManifest m) {
		if (crateTe == null || !m.crateId.equals(crateTe.getString("id"))) return new boolean[]{false, false};
		return crateItemMatch(crateTe, m, "stationapi:id");
	}

	private static boolean[] crateItemMatch(NbtCompound be, ConvManifest m, String idKey) {
		if (!be.contains("Items")) return new boolean[]{false, false};
		NbtList items = be.getList("Items");
		for (int i = 0; i < items.size(); i++) {
			NbtCompound item = (NbtCompound) items.get(i);
			if ((item.getByte("Slot") & 0xFF) == m.crateSlot && m.crateItem.equals(item.getString(idKey))) {
				boolean compOk = m.crateComponent < 0 || componentMatches(item, m.crateComponent);
				return new boolean[]{true, compOk};
			}
		}
		return new boolean[]{false, false};
	}

	private static boolean sidecarHasFullEntity(File worldDir, String ns, String id) {
		for (File f : sidecarFiles(new File(worldDir, ns + "/inventories"))) {
			NbtCompound root = readCompressed(f);
			if (root == null || !root.contains("chunks")) continue;
			NbtCompound chunks = root.getCompound("chunks");
			for (String key : nbtKeys(chunks)) {
				NbtCompound chunk = chunks.getCompound(key);
				if (!chunk.contains("fullEntities")) continue;
				NbtList list = chunk.getList("fullEntities");
				for (int i = 0; i < list.size(); i++) {
					if (id.equals(((NbtCompound) list.get(i)).getString("id"))) return true;
				}
			}
		}
		return false;
	}

	// --------------------------------------------------------------------- flattened ------------

	public static Report verifyFlattened(File worldDir, ConvManifest m) {
		Report r = new Report();

		for (ConvManifest.BlockEntry b : m.blocks) {
			r.check("block:" + shortId(b.id), flattenedBlockName(worldDir, "region", b.x, b.y, b.z).equals(b.id),
				b.id + " @ " + xyz(b.x, b.y, b.z) + " (got " + flattenedBlockName(worldDir, "region", b.x, b.y, b.z) + ")");
		}

		// Chest items now live in the flattened chunk's TileEntities as stationapi:id.
		NbtCompound chestTe = flattenedTileEntity(worldDir, "region", m.chestX, m.chestY, m.chestZ);
		for (ConvManifest.ItemEntry it : m.chestItems) {
			if (!it.modded) continue;
			boolean[] f = flattenedChestItem(chestTe, it);
			r.check("chestItem:" + shortId(it.id), f[0], "slot " + it.slot + " x" + it.count
				+ (it.component >= 0 ? (f[1] ? " (+component)" : " (component LOST)") : ""));
			if (it.component >= 0) r.check("chestComp:" + shortId(it.id), f[1], "component " + it.component);
		}

		for (ConvManifest.ItemEntry it : m.itemEntities) {
			r.check("itemEntity:" + shortId(it.id), flattenedHasItemEntity(worldDir, "region", it), it.id + " x" + it.count);
		}
		for (String id : m.entities) {
			r.check("fullEntity:" + shortId(id), flattenedHasEntity(worldDir, "region", id), id);
		}
		if (m.hasCrate) {
			NbtCompound crateTe = flattenedTileEntity(worldDir, "region", m.crateX, m.crateY, m.crateZ);
			boolean[] c = flattenedCrate(crateTe, m);
			r.check("blockEntity:" + shortId(m.crateId), c[0], m.crateId + " holding " + m.crateItem);
			r.check("beComp:" + shortId(m.crateItem), c[1], "component " + m.crateComponent);
		}
		if (m.dimBlock != null) {
			String dimRegion = "DIM" + m.dimId + "/region";
			r.check("dimBlock:" + shortId(m.dimBlock.id),
				flattenedBlockName(worldDir, dimRegion, m.dimBlock.x, m.dimBlock.y, m.dimBlock.z).equals(m.dimBlock.id),
				m.dimBlock.id + " in DIM" + m.dimId);
			if (m.dimEntity != null) {
				r.check("dimEntity:" + shortId(m.dimEntity), flattenedHasEntity(worldDir, dimRegion, m.dimEntity),
					m.dimEntity + " in DIM" + m.dimId);
			}
		}
		return r;
	}

	static boolean DEBUG = Boolean.getBoolean("retroapi.test.convdebug");

	private static String flattenedBlockName(File worldDir, String regionDir, int x, int y, int z) {
		NbtCompound level = flattenedLevel(worldDir, regionDir, x >> 4, z >> 4);
		if (level == null) { if (DEBUG) dbg("no chunk at " + (x>>4) + "," + (z>>4) + " in " + regionDir); return "minecraft:air"; }
		if (!level.contains("stationapi:sections")) {
			if (DEBUG) dbg("chunk " + (x>>4) + "," + (z>>4) + " has no stationapi:sections; keys=" + nbtKeys(level));
			return "minecraft:air";
		}
		int sectionY = y >> 4;
		int localY = y & 15, localX = x & 15, localZ = z & 15;
		int sectionIndex = (localY << 4 | localZ) << 4 | localX;
		NbtList sections = level.getList("stationapi:sections");
		for (int s = 0; s < sections.size(); s++) {
			NbtCompound section = (NbtCompound) sections.get(s);
			if (section.getByte("y") != sectionY || !section.contains("block_states")) continue;
			NbtCompound blockStates = section.getCompound("block_states");
			NbtList palette = blockStates.getList("palette");
			if (palette == null || palette.size() == 0) continue;
			long[] data = ((StationNbtCompound) blockStates).getLongArray("data");
			int paletteIdx;
			if (data == null || data.length == 0) {
				paletteIdx = 0; // single-entry palette fills the whole section
			} else {
				int bits = Math.max(4, ceilLog2(palette.size()));
				paletteIdx = new PackedIntegerArray(bits, 4096, data).get(sectionIndex);
			}
			if (DEBUG) {
				StringBuilder pn = new StringBuilder();
				for (int p = 0; p < palette.size(); p++) pn.append(((NbtCompound) palette.get(p)).getString("Name")).append(",");
				dbg("blk " + x + "," + y + "," + z + " secY=" + sectionY + " idx=" + sectionIndex
					+ " palette[" + palette.size() + "]={" + pn + "} dataLen=" + (data == null ? 0 : data.length)
					+ " -> paletteIdx=" + paletteIdx);
			}
			if (paletteIdx < palette.size()) {
				return ((NbtCompound) palette.get(paletteIdx)).getString("Name");
			}
		}
		if (DEBUG) dbg("blk " + x + "," + y + "," + z + ": no section y=" + sectionY + " (sections=" + sections.size() + ")");
		return "minecraft:air";
	}

	private static void dbg(String s) { com.periut.retroapi.testmod.TestMod.LOGGER.info("[conv-dbg] {}", s); }

	private static NbtCompound flattenedTileEntity(File worldDir, String regionDir, int x, int y, int z) {
		NbtCompound level = flattenedLevel(worldDir, regionDir, x >> 4, z >> 4);
		if (level == null || !level.contains("TileEntities")) return null;
		NbtList tes = level.getList("TileEntities");
		for (int i = 0; i < tes.size(); i++) {
			NbtCompound te = (NbtCompound) tes.get(i);
			if (te.getInt("x") == x && te.getInt("y") == y && te.getInt("z") == z) return te;
		}
		return null;
	}

	private static boolean[] flattenedChestItem(NbtCompound chestTe, ConvManifest.ItemEntry it) {
		if (chestTe == null || !chestTe.contains("Items")) return new boolean[]{false, false};
		NbtList items = chestTe.getList("Items");
		for (int i = 0; i < items.size(); i++) {
			NbtCompound item = (NbtCompound) items.get(i);
			if ((item.getByte("Slot") & 0xFF) == it.slot && it.id.equals(item.getString("stationapi:id"))) {
				boolean compOk = it.component < 0 || componentMatches(item, it.component);
				return new boolean[]{(item.getByte("Count") & 0xFF) == it.count, compOk};
			}
		}
		return new boolean[]{false, false};
	}

	private static boolean flattenedHasItemEntity(File worldDir, String regionDir, ConvManifest.ItemEntry it) {
		for (NbtCompound level : flattenedLevels(worldDir, regionDir)) {
			if (!level.contains("Entities")) continue;
			NbtList ents = level.getList("Entities");
			for (int i = 0; i < ents.size(); i++) {
				NbtCompound e = (NbtCompound) ents.get(i);
				if ("Item".equals(e.getString("id"))) {
					NbtCompound item = e.getCompound("Item");
					if (item != null && it.id.equals(item.getString("stationapi:id"))
						&& (item.getByte("Count") & 0xFF) == it.count) return true;
				}
			}
		}
		return false;
	}

	private static boolean flattenedHasEntity(File worldDir, String regionDir, String id) {
		for (NbtCompound level : flattenedLevels(worldDir, regionDir)) {
			if (!level.contains("Entities")) continue;
			NbtList ents = level.getList("Entities");
			for (int i = 0; i < ents.size(); i++) {
				if (id.equals(((NbtCompound) ents.get(i)).getString("id"))) return true;
			}
		}
		return false;
	}

	// -------------------------------------------------------------------- low-level IO ----------

	/** All Level compounds across every chunk of every region file in {@code worldDir/regionDir}. */
	private static List<NbtCompound> flattenedLevels(File worldDir, String regionDir) {
		List<NbtCompound> out = new ArrayList<>();
		File dir = new File(worldDir, regionDir);
		File[] files = dir.listFiles((d, n) -> n.endsWith(".mcr"));
		if (files == null) return out;
		for (File f : files) {
			RegionFile region = new RegionFile(f);
			try {
				for (int lx = 0; lx < 32; lx++) for (int lz = 0; lz < 32; lz++) {
					if (!region.hasChunkData(lx, lz)) continue;
					DataInputStream dis = region.getChunkInputStream(lx, lz);
					if (dis == null) continue;
					NbtCompound tag = NbtIo.read(dis);
					dis.close();
					NbtCompound level = tag.getCompound("Level");
					if (level != null) out.add(level);
				}
			} catch (Exception ignored) {
			} finally {
				try { region.close(); } catch (Exception ignored) {}
			}
		}
		return out;
	}

	private static NbtCompound flattenedLevel(File worldDir, String regionDir, int cx, int cz) {
		File f = new File(new File(worldDir, regionDir), "r." + (cx >> 5) + "." + (cz >> 5) + ".mcr");
		if (!f.exists()) return null;
		RegionFile region = new RegionFile(f);
		try {
			int lx = cx & 31, lz = cz & 31;
			if (!region.hasChunkData(lx, lz)) return null;
			DataInputStream dis = region.getChunkInputStream(lx, lz);
			if (dis == null) return null;
			NbtCompound tag = NbtIo.read(dis);
			dis.close();
			return tag.getCompound("Level");
		} catch (Exception e) {
			return null;
		} finally {
			try { region.close(); } catch (Exception ignored) {}
		}
	}

	private static NbtCompound sidecarChunk(File sidecarFile, int cx, int cz) {
		NbtCompound root = readCompressed(sidecarFile);
		if (root == null || !root.contains("chunks")) return null;
		NbtCompound chunks = root.getCompound("chunks");
		String key = cx + "," + cz;
		return chunks.contains(key) ? chunks.getCompound(key) : null;
	}

	private static File[] sidecarFiles(File dir) {
		File[] files = dir.listFiles((d, n) -> n.endsWith(".dat"));
		return files == null ? new File[0] : files;
	}

	private static NbtCompound readCompressed(File f) {
		if (!f.exists()) return null;
		try (FileInputStream fis = new FileInputStream(f)) {
			return NbtIo.readCompressed(fis);
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Whether a saved item carries the test {@code retroapi:components} blob with the expected value.
	 * Reads the NBT directly (not via the {@code RetroComponentHolder} interface on ItemStack - that
	 * interface mixin is disabled under StationAPI, so the cast would throw there). The INT component
	 * serializer stores the value under its identifier key.
	 */
	private static boolean componentMatches(NbtCompound itemOrEntry, int expected) {
		if (!itemOrEntry.contains("retroapi:components")) return false;
		NbtCompound comps = itemOrEntry.getCompound("retroapi:components");
		String key = com.periut.retroapi.testmod.TestMod.TEST_COUNT.getId().toString();
		return comps.contains(key) && comps.getInt(key) == expected;
	}

	/** Read the sidecar chunk's secondary meta (xmeta) at a chunk position, 0 if absent. */
	private static int sidecarXmeta(NbtCompound chunk, int position) {
		byte[] xpos = chunk.getByteArray("xpos");
		byte[] xval = chunk.getByteArray("xval");
		if (xpos.length == 0 || xval.length * 4 != xpos.length) return 0;
		int[] positions = bytesToInts(xpos);
		for (int i = 0; i < positions.length && i < xval.length; i++) {
			if (positions[i] == position) return xval[i] & 0xFF;
		}
		return 0;
	}

	/** Decode a block-sidecar chunk's per-position ids, handling both the v2 palette and legacy v1 forms. */
	private static String[] decodeSidecarIds(NbtCompound chunk) {
		if (chunk.contains("palette")) {
			String[] palette = chunk.getString("palette").split("\0");
			byte[] idx = chunk.getByteArray("paletteIdx");
			String[] ids = new String[idx.length];
			for (int i = 0; i < idx.length; i++) {
				int pi = idx[i] & 0xFF;
				ids[i] = pi < palette.length ? palette[pi] : "unknown:0";
			}
			return ids;
		}
		String idsJoined = chunk.getString("ids");
		return idsJoined.isEmpty() ? new String[0] : idsJoined.split("\0");
	}

	private static int[] bytesToInts(byte[] bytes) {
		ByteBuffer buf = ByteBuffer.wrap(bytes);
		int[] ints = new int[bytes.length / 4];
		for (int i = 0; i < ints.length; i++) ints[i] = buf.getInt();
		return ints;
	}

	private static int ceilLog2(int n) {
		if (n <= 1) return 0;
		return 32 - Integer.numberOfLeadingZeros(n - 1);
	}

	private static String shortId(String id) {
		int c = id.indexOf(':');
		return c >= 0 ? id.substring(c + 1) : id;
	}

	private static String xyz(int x, int y, int z) {
		return x + "," + y + "," + z;
	}

	/** NbtCompound has no key iteration in b1.7.3; round-trip through binary NBT to recover keys. */
	private static List<String> nbtKeys(NbtCompound compound) {
		List<String> keys = new ArrayList<>();
		try {
			java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
			java.io.DataOutputStream dos = new java.io.DataOutputStream(baos);
			NbtCompound wrapper = new NbtCompound();
			wrapper.put("d", compound);
			NbtIo.write(wrapper, dos);
			dos.flush();
			DataInputStream dis = new DataInputStream(new java.io.ByteArrayInputStream(baos.toByteArray()));
			dis.readByte(); dis.readUTF();          // root compound tag + name
			dis.readByte(); dis.readUTF();          // inner "d" compound tag + name
			while (true) {
				byte type = dis.readByte();
				if (type == 0) break;
				keys.add(dis.readUTF());
				skip(dis, type);
			}
		} catch (Exception ignored) {
		}
		return keys;
	}

	private static void skip(DataInputStream dis, byte type) throws java.io.IOException {
		switch (type) {
			case 1 -> dis.readByte();
			case 2 -> dis.readShort();
			case 3 -> dis.readInt();
			case 4 -> dis.readLong();
			case 5 -> dis.readFloat();
			case 6 -> dis.readDouble();
			case 7 -> { int len = dis.readInt(); dis.skipBytes(len); }
			case 8 -> dis.readUTF();
			case 9 -> { byte lt = dis.readByte(); int c = dis.readInt(); for (int i = 0; i < c; i++) skip(dis, lt); }
			case 10 -> { while (true) { byte t = dis.readByte(); if (t == 0) break; dis.readUTF(); skip(dis, t); } }
			case 11 -> { int len = dis.readInt(); dis.skipBytes(len * 4); }
			case 12 -> { int len = dis.readInt(); dis.skipBytes(len * 8); }
		}
	}
}
