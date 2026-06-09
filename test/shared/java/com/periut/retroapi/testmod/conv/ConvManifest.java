package com.periut.retroapi.testmod.conv;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * The ground-truth record of exactly what the {@code populate} scenario placed and where, written
 * next to the world so the {@code roundtrip} verifier knows precisely what to assert at each stage
 * of the vanilla&lt;-&gt;StationAPI conversion. Stores absolute coordinates (computed from the world
 * spawn at populate time) so the verifier never has to rediscover the spawn.
 *
 * <p>All ids are RetroAPI string identifiers ({@code "retroapi_test:test_block"}) - the same form
 * blocks/items/entities take in both the McRegion sidecar and the StationAPI flattened palette - so a
 * single expected value is comparable in either world format.
 */
public final class ConvManifest {

	public static final class BlockEntry {
		public final int x, y, z, meta;
		/** Full RetroAPI flattened-state index (meta | xmeta&lt;&lt;4), or -1 for a plain meta block. */
		public final int state;
		public final String id;
		public BlockEntry(int x, int y, int z, String id, int meta) {
			this(x, y, z, id, meta, -1);
		}
		public BlockEntry(int x, int y, int z, String id, int meta, int state) {
			this.x = x; this.y = y; this.z = z; this.id = id; this.meta = meta; this.state = state;
		}
	}

	public static final class ItemEntry {
		public final int slot, count, damage, component; // component = expected TEST_COUNT value, or -1 if none
		public final String id;
		public final boolean modded;
		public ItemEntry(int slot, String id, int count, int damage, int component, boolean modded) {
			this.slot = slot; this.id = id; this.count = count; this.damage = damage;
			this.component = component; this.modded = modded;
		}
	}

	/** Block coordinates of the vanilla chest holding modded + component items. */
	public int chestX, chestY, chestZ;
	public final List<BlockEntry> blocks = new ArrayList<>();
	public final List<ItemEntry> chestItems = new ArrayList<>();
	/** Dropped modded item entities, expected near the placement region. */
	public final List<ItemEntry> itemEntities = new ArrayList<>();
	/** Modded full entities (e.g. the zeve mob) by string id. */
	public final List<String> entities = new ArrayList<>();
	/** Modded block entity (crate): its position + one modded item it holds (with optional component). */
	public boolean hasCrate;
	public int crateX, crateY, crateZ, crateSlot, crateComponent = -1;
	public String crateId, crateItem;

	/** Dimension content: a single extended block placed in the test dimension, or null if none. */
	public int dimId = Integer.MIN_VALUE;
	public BlockEntry dimBlock;
	public String dimEntity;

	public void addBlock(int x, int y, int z, String id, int meta) {
		blocks.add(new BlockEntry(x, y, z, id, meta));
	}

	public void save(File worldDir) throws IOException {
		NbtCompound root = new NbtCompound();

		NbtList blockList = new NbtList();
		for (BlockEntry b : blocks) blockList.add(blockNbt(b));
		root.put("blocks", blockList);

		root.putInt("chestX", chestX);
		root.putInt("chestY", chestY);
		root.putInt("chestZ", chestZ);
		NbtList chestList = new NbtList();
		for (ItemEntry it : chestItems) chestList.add(itemNbt(it));
		root.put("chestItems", chestList);

		NbtList itemEntityList = new NbtList();
		for (ItemEntry it : itemEntities) itemEntityList.add(itemNbt(it));
		root.put("itemEntities", itemEntityList);

		NbtList entityList = new NbtList();
		for (String id : entities) {
			NbtCompound e = new NbtCompound();
			e.putString("id", id);
			entityList.add(e);
		}
		root.put("entities", entityList);

		if (hasCrate) {
			NbtCompound c = new NbtCompound();
			c.putInt("x", crateX); c.putInt("y", crateY); c.putInt("z", crateZ);
			c.putInt("slot", crateSlot); c.putInt("component", crateComponent);
			c.putString("id", crateId); c.putString("item", crateItem);
			root.put("crate", c);
		}

		if (dimBlock != null) {
			root.putInt("dimId", dimId);
			root.put("dimBlock", blockNbt(dimBlock));
			if (dimEntity != null) root.putString("dimEntity", dimEntity);
		}

		File f = new File(worldDir, Scenario.MANIFEST_FILE);
		try (FileOutputStream fos = new FileOutputStream(f)) {
			NbtIo.writeCompressed(root, fos);
		}
	}

	public static ConvManifest load(File worldDir) throws IOException {
		File f = new File(worldDir, Scenario.MANIFEST_FILE);
		ConvManifest m = new ConvManifest();
		try (FileInputStream fis = new FileInputStream(f)) {
			NbtCompound root = NbtIo.readCompressed(fis);

			NbtList blockList = root.getList("blocks");
			for (int i = 0; i < blockList.size(); i++) m.blocks.add(blockFrom((NbtCompound) blockList.get(i)));

			m.chestX = root.getInt("chestX");
			m.chestY = root.getInt("chestY");
			m.chestZ = root.getInt("chestZ");
			NbtList chestList = root.getList("chestItems");
			for (int i = 0; i < chestList.size(); i++) m.chestItems.add(itemFrom((NbtCompound) chestList.get(i)));

			NbtList itemEntityList = root.getList("itemEntities");
			for (int i = 0; i < itemEntityList.size(); i++) m.itemEntities.add(itemFrom((NbtCompound) itemEntityList.get(i)));

			NbtList entityList = root.getList("entities");
			for (int i = 0; i < entityList.size(); i++) m.entities.add(((NbtCompound) entityList.get(i)).getString("id"));

			if (root.contains("crate")) {
				NbtCompound c = root.getCompound("crate");
				m.hasCrate = true;
				m.crateX = c.getInt("x"); m.crateY = c.getInt("y"); m.crateZ = c.getInt("z");
				m.crateSlot = c.getInt("slot"); m.crateComponent = c.getInt("component");
				m.crateId = c.getString("id"); m.crateItem = c.getString("item");
			}

			if (root.contains("dimBlock")) {
				m.dimId = root.getInt("dimId");
				m.dimBlock = blockFrom(root.getCompound("dimBlock"));
				if (root.contains("dimEntity")) m.dimEntity = root.getString("dimEntity");
			}
		}
		return m;
	}

	private static NbtCompound blockNbt(BlockEntry b) {
		NbtCompound c = new NbtCompound();
		c.putInt("x", b.x); c.putInt("y", b.y); c.putInt("z", b.z);
		c.putString("id", b.id); c.putInt("meta", b.meta); c.putInt("state", b.state);
		return c;
	}

	private static BlockEntry blockFrom(NbtCompound c) {
		return new BlockEntry(c.getInt("x"), c.getInt("y"), c.getInt("z"), c.getString("id"), c.getInt("meta"),
			c.contains("state") ? c.getInt("state") : -1);
	}

	private static NbtCompound itemNbt(ItemEntry it) {
		NbtCompound c = new NbtCompound();
		c.putInt("slot", it.slot);
		c.putString("id", it.id);
		c.putInt("count", it.count);
		c.putInt("damage", it.damage);
		c.putInt("component", it.component);
		c.putBoolean("modded", it.modded);
		return c;
	}

	private static ItemEntry itemFrom(NbtCompound c) {
		return new ItemEntry(c.getInt("slot"), c.getString("id"), c.getInt("count"),
			c.getInt("damage"), c.getInt("component"), c.getBoolean("modded"));
	}
}
