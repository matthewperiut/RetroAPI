package com.periut.retroapi.testmod.conv;

import com.periut.retroapi.component.RetroComponents;
import com.periut.retroapi.registry.BlockRegistration;
import com.periut.retroapi.registry.ItemRegistration;
import com.periut.retroapi.registry.RetroRegistry;
import com.periut.retroapi.testmod.TestMod;
import com.periut.retroapi.testmod.ZeveEntity;
import net.minecraft.block.Block;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.ServerWorld;

/**
 * Reads the manifest content straight back through a live RetroAPI world (no StationAPI). Run after a
 * reverse conversion to prove the un-flattened McRegion + sidecar world is genuinely usable in plain
 * RetroAPI - the strongest possible "the round-trip didn't just look right on disk, it actually loads"
 * check. Reused as the populate-time self-check too.
 */
public final class ServerVerifier {
	private ServerVerifier() {}

	public static boolean run(MinecraftServer server) {
		ConvManifest m;
		try {
			java.io.File worldDir = com.periut.retroapi.storage.SidecarManager.getWorldDir();
			if (worldDir == null) worldDir = Scenario.worldDir();
			m = ConvManifest.load(worldDir);
		} catch (Exception e) {
			TestMod.LOGGER.error("[conv-verify] cannot load manifest", e);
			return false;
		}
		return verify(server.worlds[0], server, m);
	}

	public static boolean verify(ServerWorld world, MinecraftServer server, ConvManifest m) {
		boolean ok = true;

		// On a playerless server only the spawn region is held loaded; force-load every chunk the
		// manifest content lives in (plus a margin for entities, which only enter world.entities once
		// their chunk loads) so the sidecar restore actually runs before we read it back.
		forceLoadAround(world, m);

		// Blocks
		for (ConvManifest.BlockEntry b : m.blocks) {
			ok &= checkBlock(world, b);
		}

		// Chest items
		if (world.getBlockEntity(m.chestX, m.chestY, m.chestZ) instanceof ChestBlockEntity chest) {
			for (ConvManifest.ItemEntry it : m.chestItems) {
				ok &= checkChestItem(chest, it);
			}
		} else {
			ok = false;
			TestMod.LOGGER.error("[conv-verify] no chest at {},{},{}", m.chestX, m.chestY, m.chestZ);
		}

		// Dropped item entities
		for (ConvManifest.ItemEntry it : m.itemEntities) {
			if (!findItemEntity(world, it)) {
				ok = false;
				TestMod.LOGGER.error("[conv-verify] missing item entity {} x{} (comp={})", it.id, it.count, it.component);
			}
		}

		// Modded block entity (crate) with its modded + component item
		if (m.hasCrate) {
			net.minecraft.block.entity.BlockEntity be = world.getBlockEntity(m.crateX, m.crateY, m.crateZ);
			if (be instanceof com.periut.retroapi.testmod.TestCrateBlockEntity crate) {
				ItemStack s = crate.getStack(m.crateSlot);
				boolean itemOk = s != null && m.crateItem.equals(itemName(s));
				boolean compOk = m.crateComponent < 0
					|| (s != null && Integer.valueOf(m.crateComponent).equals(RetroComponents.get(s, TestMod.TEST_COUNT)));
				if (!itemOk || !compOk) {
					ok = false;
					TestMod.LOGGER.error("[conv-verify] crate item mismatch: expected {} (comp {}) got {}",
						m.crateItem, m.crateComponent, s == null ? "empty" : itemName(s));
				}
			} else {
				ok = false;
				TestMod.LOGGER.error("[conv-verify] no crate block entity at {},{},{} (got {})",
					m.crateX, m.crateY, m.crateZ, be);
			}
		}

		// Full modded entities (the zeve)
		long zeveCount = countZeves(world);
		long expectedZeves = m.entities.stream().filter(s -> s.endsWith(":zeve")).count();
		if (zeveCount < expectedZeves) {
			ok = false;
			TestMod.LOGGER.error("[conv-verify] expected >= {} zeve(s) in overworld, found {}", expectedZeves, zeveCount);
		}

		// Dimension content. Modded dimensions are side-mapped, so the save driver in MinecraftServerMixin
		// must persist them by hand (saveWithLoadingDisplay drives the chunk-save sidecar hook).
		if (m.dimBlock != null) {
			ServerWorld dim = server.getWorld(m.dimId);
			if (dim == null) {
				ok = false;
				TestMod.LOGGER.error("[conv-verify] test dimension {} not loaded", m.dimId);
			} else {
				loadAround(dim, m.dimBlock.x, m.dimBlock.z, new java.util.HashSet<>());
				if (!checkBlock(dim, m.dimBlock)) ok = false;
				if (m.dimEntity != null && countZeves(dim) < 1) {
					ok = false;
					TestMod.LOGGER.error("[conv-verify] missing zeve in dimension {}", m.dimId);
				}
			}
		}

		TestMod.LOGGER.info("[conv-verify] in-engine verification {}", ok ? "PASS" : "FAIL");
		return ok;
	}

	/** Load every chunk the manifest content occupies (with a 1-chunk margin for entities). */
	private static void forceLoadAround(ServerWorld world, ConvManifest m) {
		java.util.Set<Long> done = new java.util.HashSet<>();
		for (ConvManifest.BlockEntry b : m.blocks) loadAround(world, b.x, b.z, done);
		loadAround(world, m.chestX, m.chestZ, done);
	}

	private static void loadAround(ServerWorld world, int x, int z, java.util.Set<Long> done) {
		int cx = x >> 4, cz = z >> 4;
		for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
			int lx = cx + dx, lz = cz + dz;
			long key = ((long) lx << 32) ^ (lz & 0xffffffffL);
			if (done.add(key)) world.chunkCache.loadChunk(lx, lz);
		}
	}

	private static boolean checkBlock(ServerWorld world, ConvManifest.BlockEntry b) {
		int id = world.getBlockId(b.x, b.y, b.z);
		String name = blockName(id);
		int meta = world.getBlockMeta(b.x, b.y, b.z);
		if (!b.id.equals(name) || b.meta != meta) {
			TestMod.LOGGER.error("[conv-verify] block at {},{},{}: expected {}#{} got {}#{}",
				b.x, b.y, b.z, b.id, b.meta, name, meta);
			return false;
		}
		return true;
	}

	private static boolean checkChestItem(ChestBlockEntity chest, ConvManifest.ItemEntry it) {
		ItemStack s = chest.getStack(it.slot);
		if (s == null) {
			TestMod.LOGGER.error("[conv-verify] chest slot {} empty (expected {})", it.slot, it.id);
			return false;
		}
		boolean ok = true;
		if (it.modded) {
			String name = itemName(s);
			if (!it.id.equals(name)) {
				TestMod.LOGGER.error("[conv-verify] chest slot {}: expected {} got {}", it.slot, it.id, name);
				ok = false;
			}
		}
		if (s.count != it.count) {
			TestMod.LOGGER.error("[conv-verify] chest slot {} count: expected {} got {}", it.slot, it.count, s.count);
			ok = false;
		}
		if (it.component >= 0) {
			Integer v = RetroComponents.get(s, TestMod.TEST_COUNT);
			if (v == null || v != it.component) {
				TestMod.LOGGER.error("[conv-verify] chest slot {} component: expected {} got {}", it.slot, it.component, v);
				ok = false;
			}
		}
		return ok;
	}

	private static boolean findItemEntity(ServerWorld world, ConvManifest.ItemEntry it) {
		for (Entity e : world.entities) {
			if (e instanceof ItemEntity ie && ie.stack != null) {
				if (it.id.equals(itemName(ie.stack)) && ie.stack.count == it.count) {
					if (it.component < 0) return true;
					Integer v = RetroComponents.get(ie.stack, TestMod.TEST_COUNT);
					if (v != null && v == it.component) return true;
				}
			}
		}
		return false;
	}

	private static long countZeves(ServerWorld world) {
		long n = 0;
		for (Entity e : world.entities) if (e instanceof ZeveEntity) n++;
		return n;
	}

	private static String blockName(int id) {
		if (id <= 0 || id >= Block.BLOCKS.length || Block.BLOCKS[id] == null) return "minecraft:air";
		BlockRegistration reg = RetroRegistry.getBlockRegistration(Block.BLOCKS[id]);
		return reg != null ? reg.getId().toString() : "minecraft:" + id;
	}

	private static String itemName(ItemStack s) {
		int id = s.itemId;
		// A block-item carries the block's id; resolve it through the block registry first.
		if (id > 0 && id < Block.BLOCKS.length && Block.BLOCKS[id] != null) {
			BlockRegistration breg = RetroRegistry.getBlockRegistration(Block.BLOCKS[id]);
			if (breg != null) return breg.getId().toString();
		}
		if (id >= 0 && id < Item.ITEMS.length && Item.ITEMS[id] != null) {
			ItemRegistration reg = RetroRegistry.getItemRegistration(Item.ITEMS[id]);
			if (reg != null) return reg.getId().toString();
		}
		return "minecraft:" + id;
	}
}
