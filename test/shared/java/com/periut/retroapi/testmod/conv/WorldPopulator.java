package com.periut.retroapi.testmod.conv;

import com.periut.retroapi.component.RetroComponents;
import com.periut.retroapi.storage.SidecarManager;
import com.periut.retroapi.testmod.TestMod;
import com.periut.retroapi.testmod.ZeveEntity;
import net.minecraft.block.Block;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.ServerWorld;

import java.io.File;

/**
 * Places a fixed, content-rich set of modded data into a fresh world so the conversion round-trip
 * has something concrete to preserve. Every kind of modded persistence is represented:
 * <ul>
 *   <li>extended blocks (id &ge; 256) with and without metadata - the block sidecar path;</li>
 *   <li>modded + component-bearing items inside a vanilla chest, plus a vanilla item that must not be
 *       collateral-damaged - the inventory sidecar path;</li>
 *   <li>dropped modded item entities - the item-entity sidecar path;</li>
 *   <li>a persistent modded mob - the full-entity sidecar path;</li>
 *   <li>an extended block and a mob in the test dimension - the dimension-scoped sidecar path.</li>
 * </ul>
 * Everything is placed at absolute coordinates derived from the world spawn (so it lands in
 * already-loaded chunks) and recorded in a {@link ConvManifest}. Placement is then read straight back
 * through the live world to catch any populate-time failure before the world is even saved.
 */
public final class WorldPopulator {
	private WorldPopulator() {}

	private static String rid(String name) {
		return "retroapi_test:" + name;
	}

	public static boolean run(MinecraftServer server) {
		ServerWorld world = server.worlds[0];
		if (world == null) {
			TestMod.LOGGER.error("[conv-populate] overworld is null");
			return false;
		}

		Vec3i spawn = world.getSpawnPos();
		int bx = spawn.x + 2;
		int py = Math.min(120, Math.max(4, spawn.y + 2));
		int bz = spawn.z;

		ConvManifest manifest = new ConvManifest();

		// --- Extended blocks (id >= 256) ---
		placeBlock(world, manifest, bx,     py, bz, TestMod.TEST_BLOCK, "test_block", 0);
		placeBlock(world, manifest, bx + 1, py, bz, TestMod.COLOR_BLOCK, "color_block", 3); // meta retention
		placeBlock(world, manifest, bx + 2, py, bz, TestMod.PIPE_BLOCK, "pipe", 0);
		placeBlock(world, manifest, bx + 3, py, bz, TestMod.BLOCKS[0], "block_0", 0);
		placeBlock(world, manifest, bx + 4, py, bz, TestMod.BLOCKS[199], "block_199", 0);
		// A RetroAPI flattened-state block: state index 17 > 15 exercises the xmeta high-bits path,
		// which lives in RetroAPI's own sidecar (not StationAPI's blockstate model) and must survive
		// the conversion round-trip losslessly.
		placeStateBlock(world, manifest, bx + 5, py, bz, TestMod.LAMP, "lamp", 17);

		// --- Vanilla chest holding modded + component-bearing items (+ one vanilla item) ---
		int cx = bx, cy = py, cz = bz + 2;
		world.setBlock(cx, cy, cz, Block.CHEST.id);
		manifest.chestX = cx; manifest.chestY = cy; manifest.chestZ = cz;
		if (world.getBlockEntity(cx, cy, cz) instanceof ChestBlockEntity chest) {
			setChestItem(chest, manifest, 0, new ItemStack(TestMod.TEST_ITEM, 32), rid("test_item"), 32, 0, -1, true);
			setChestItem(chest, manifest, 1, new ItemStack(TestMod.ITEMS[5], 64), rid("item_5"), 64, 0, -1, true);
			setChestItem(chest, manifest, 2, new ItemStack(TestMod.BLOCKS[0], 16), rid("block_0"), 16, 0, -1, true);
			setChestItem(chest, manifest, 3, new ItemStack(Item.DIAMOND, 5), "minecraft:diamond", 5, 0, -1, false);
			ItemStack comp = new ItemStack(TestMod.TEST_ITEM, 1);
			RetroComponents.set(comp, TestMod.TEST_COUNT, 42);
			setChestItem(chest, manifest, 4, comp, rid("test_item"), 1, 0, 42, true);
		} else {
			TestMod.LOGGER.error("[conv-populate] failed to create chest at {},{},{}", cx, cy, cz);
		}

		// --- Modded block entity (crate) with a modded + component-bearing item inside ---
		int crx = bx, cry = py, crz = bz + 3;
		Block.BLOCKS_WITH_ENTITY[TestMod.CRATE.id] = true;
		world.setBlock(crx, cry, crz, TestMod.CRATE.id);
		com.periut.retroapi.testmod.TestCrateBlockEntity crate = new com.periut.retroapi.testmod.TestCrateBlockEntity();
		crate.x = crx; crate.y = cry; crate.z = crz;
		world.setBlockEntity(crx, cry, crz, crate);
		ItemStack crateStack = new ItemStack(TestMod.TEST_ITEM, 5);
		RetroComponents.set(crateStack, TestMod.TEST_COUNT, 7);
		crate.setStack(0, crateStack);
		manifest.hasCrate = true;
		manifest.crateX = crx; manifest.crateY = cry; manifest.crateZ = crz;
		manifest.crateSlot = 0; manifest.crateComponent = 7;
		manifest.crateId = rid("crate"); manifest.crateItem = rid("test_item");

		// --- Dropped modded item entities ---
		dropItem(world, manifest, bx, py + 1, bz + 4, new ItemStack(TestMod.TEST_ITEM, 10), rid("test_item"), 10, -1);
		ItemStack compDrop = new ItemStack(TestMod.ITEMS[3], 7);
		RetroComponents.set(compDrop, TestMod.TEST_COUNT, 99);
		dropItem(world, manifest, bx, py + 1, bz + 4, compDrop, rid("item_3"), 7, 99);

		// --- Persistent modded mob ---
		ZeveEntity zeve = new ZeveEntity(world);
		zeve.setPositionAndAngles(bx + 1 + 0.5, py, bz + 6 + 0.5, 0.0F, 0.0F);
		world.spawnEntity(zeve);
		manifest.entities.add(rid("zeve"));

		// --- Test-dimension content (modded dimension, DIM<serialId>) ---
		int dimSerial = TestMod.TEST_DIMENSION.getSerialId();
		ServerWorld dim = server.getWorld(dimSerial);
		if (dim != null) {
			Vec3i dimSpawn = dim.getSpawnPos();
			int dx = dimSpawn.x + 2;
			int dy = Math.min(120, Math.max(4, dimSpawn.y + 2));
			int dz = dimSpawn.z;
			boolean placed = dim.setBlock(dx, dy, dz, TestMod.TEST_BLOCK.id);
			int readBack = dim.getBlockId(dx, dy, dz);
			TestMod.LOGGER.info("[conv-populate] dim block at {},{},{}: setBlock={} readBack={} (expected {})",
				dx, dy, dz, placed, readBack, TestMod.TEST_BLOCK.id);
			manifest.dimId = dimSerial;
			manifest.dimBlock = new ConvManifest.BlockEntry(dx, dy, dz, rid("test_block"), 0);

			ZeveEntity dimZeve = new ZeveEntity(dim);
			dimZeve.setPositionAndAngles(dx + 1 + 0.5, dy, dz + 0.5, 0.0F, 0.0F);
			dim.spawnEntity(dimZeve);
			manifest.dimEntity = rid("zeve");
		} else {
			TestMod.LOGGER.warn("[conv-populate] test dimension (serial {}) not available; skipping dim content", dimSerial);
		}

		// --- Self-verify the placement in-engine before saving ---
		boolean ok = selfVerify(world, manifest);

		// --- Persist: save chunks (drives the sidecar hook) + flush level, then flush the sidecars ---
		world.saveWithLoadingDisplay(true, null);
		world.forceSave();
		if (dim != null) {
			dim.saveWithLoadingDisplay(true, null);
			dim.forceSave();
		}
		SidecarManager.saveAll();

		File worldDir = SidecarManager.getWorldDir();
		if (worldDir == null) worldDir = Scenario.worldDir();
		try {
			manifest.save(worldDir);
			TestMod.LOGGER.info("[conv-populate] wrote manifest to {}", new File(worldDir, Scenario.MANIFEST_FILE));
		} catch (Exception e) {
			TestMod.LOGGER.error("[conv-populate] failed to write manifest", e);
			ok = false;
		}

		TestMod.LOGGER.info("[conv-populate] placement {} ({} blocks, {} chest items, {} item entities, {} entities, dim={})",
			ok ? "PASS" : "FAIL", manifest.blocks.size(), manifest.chestItems.size(),
			manifest.itemEntities.size(), manifest.entities.size(), manifest.dimBlock != null);
		return ok;
	}

	private static void placeBlock(ServerWorld world, ConvManifest m, int x, int y, int z, Block block, String name, int meta) {
		if (block == null) { TestMod.LOGGER.error("[conv-populate] null block {}", name); return; }
		if (meta != 0) world.setBlock(x, y, z, block.id, meta);
		else world.setBlock(x, y, z, block.id);
		m.addBlock(x, y, z, rid(name), meta);
	}

	/** Place a RetroAPI flattened-state block, writing its full state index via RetroStates (meta + xmeta). */
	private static void placeStateBlock(ServerWorld world, ConvManifest m, int x, int y, int z, Block block, String name, int stateIndex) {
		if (block == null) { TestMod.LOGGER.error("[conv-populate] null state block {}", name); return; }
		world.setBlock(x, y, z, block.id);
		com.periut.retroapi.state.RetroStates.set(world, x, y, z,
			com.periut.retroapi.state.RetroStates.fromIndex(block, stateIndex));
		int meta = world.getBlockMeta(x, y, z);
		m.blocks.add(new ConvManifest.BlockEntry(x, y, z, rid(name), meta, stateIndex));
	}

	private static void setChestItem(ChestBlockEntity chest, ConvManifest m, int slot, ItemStack stack,
									 String id, int count, int damage, int component, boolean modded) {
		chest.setStack(slot, stack);
		m.chestItems.add(new ConvManifest.ItemEntry(slot, id, count, damage, component, modded));
	}

	private static void dropItem(ServerWorld world, ConvManifest m, double x, double y, double z, ItemStack stack,
								 String id, int count, int component) {
		ItemEntity ie = new ItemEntity(world, x + 0.5, y, z + 0.5, stack);
		world.spawnEntity(ie);
		m.itemEntities.add(new ConvManifest.ItemEntry(0, id, count, 0, component, true));
	}

	/** Read every placed block/chest item straight back through the live world. */
	private static boolean selfVerify(ServerWorld world, ConvManifest m) {
		boolean ok = true;
		for (ConvManifest.BlockEntry b : m.blocks) {
			int gotId = world.getBlockId(b.x, b.y, b.z);
			String gotName = nameOfBlock(gotId);
			boolean idOk = b.id.equals(gotName);
			boolean metaOk = b.meta == world.getBlockMeta(b.x, b.y, b.z);
			if (!idOk || !metaOk) {
				ok = false;
				TestMod.LOGGER.error("[conv-populate] block mismatch at {},{},{}: expected {}#{} got {}#{}",
					b.x, b.y, b.z, b.id, b.meta, gotName, world.getBlockMeta(b.x, b.y, b.z));
			}
		}
		if (world.getBlockEntity(m.chestX, m.chestY, m.chestZ) instanceof ChestBlockEntity chest) {
			for (ConvManifest.ItemEntry it : m.chestItems) {
				ItemStack s = chest.getStack(it.slot);
				if (s == null) { ok = false; TestMod.LOGGER.error("[conv-populate] chest slot {} empty", it.slot); continue; }
				if (it.component >= 0) {
					Integer v = RetroComponents.get(s, TestMod.TEST_COUNT);
					if (v == null || v != it.component) {
						ok = false;
						TestMod.LOGGER.error("[conv-populate] chest slot {} component expected {} got {}", it.slot, it.component, v);
					}
				}
			}
		} else {
			ok = false;
			TestMod.LOGGER.error("[conv-populate] no chest at manifest position");
		}
		return ok;
	}

	private static String nameOfBlock(int id) {
		if (id <= 0 || id >= Block.BLOCKS.length || Block.BLOCKS[id] == null) return "minecraft:air";
		Block block = Block.BLOCKS[id];
		var reg = com.periut.retroapi.registry.RetroRegistry.getBlockRegistration(block);
		return reg != null ? reg.getId().toString() : "minecraft:" + id;
	}
}
