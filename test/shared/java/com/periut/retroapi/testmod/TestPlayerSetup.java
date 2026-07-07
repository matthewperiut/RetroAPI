package com.periut.retroapi.testmod;

import com.periut.retroapi.achievement.RetroAchievements;
import net.minecraft.block.Block;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * One-shot per-player test setup: starter items, the numbered-content chests, the root achievement,
 * and a zeve mob spawned at the player's feet. Called every tick from BOTH hooks - {@code
 * PlayerEntity.tick} (single-player; see {@code GiveItemsMixin}) and {@code ServerPlayerEntity.tick}
 * (dedicated server, driven by {@code world.tickEntities()} with no client-packet dependency; see
 * {@code ServerGiveItemsMixin}) - so the once-guard lives HERE, keyed weakly per player instance
 * (a fresh ServerPlayerEntity per login means every join/world load re-runs, intentionally).
 */
public final class TestPlayerSetup {
	private TestPlayerSetup() {}

	private static final Set<PlayerEntity> DONE = Collections.newSetFromMap(new WeakHashMap<>());

	private static void safeAddItem(PlayerInventory inv, ItemStack stack) {
		if (stack.itemId >= 0 && stack.itemId < Item.ITEMS.length && Item.ITEMS[stack.itemId] != null) {
			inv.addStack(stack);
		} else {
			TestMod.LOGGER.error("Cannot add item with id {} - no Item.BY_ID entry!", stack.itemId);
		}
	}

	/** Idempotent per player instance; server-side only (no-op on remote worlds). */
	public static void run(PlayerEntity self) {
		World world = self.world;
		if (world == null || world.isRemote || !DONE.add(self)) return;
		PlayerInventory inventory = self.inventory;

		TestMod.LOGGER.info("Setting up test player {} ({})", self.name, self.getClass().getSimpleName());

		if (TestMod.TEST_BLOCK != null) {
			safeAddItem(inventory, new ItemStack(TestMod.TEST_BLOCK, 64));
			safeAddItem(inventory, new ItemStack(TestMod.COLOR_BLOCK, 64));
			safeAddItem(inventory, new ItemStack(TestMod.PIPE_BLOCK, 64));
		}
		// Test dimension portal: place it, walk into it to travel to the test dimension.
		if (TestMod.TEST_PORTAL != null) {
			safeAddItem(inventory, new ItemStack(TestMod.TEST_PORTAL, 16));
		}
		if (TestMod.TEST_ITEM != null) {
			safeAddItem(inventory, new ItemStack(TestMod.TEST_ITEM, 64));
		}

		safeAddItem(inventory, new ItemStack(Item.DIAMOND_PICKAXE, 1));

		// Asset platform content: tagged ore (pickaxe-gated drops), state lamp, animated
		// block and item, layered item.
		if (TestMod.TAGGED_ORE != null) {
			safeAddItem(inventory, new ItemStack(TestMod.TAGGED_ORE, 16));
			safeAddItem(inventory, new ItemStack(TestMod.LAMP, 16));
			safeAddItem(inventory, new ItemStack(TestMod.ANIM_BLOCK, 16));
			safeAddItem(inventory, new ItemStack(TestMod.ANIM_ITEM, 16));
			safeAddItem(inventory, new ItemStack(TestMod.LAYER_ITEM, 16));
			safeAddItem(inventory, new ItemStack(TestMod.DEF_ITEM, 16));
		}

		// --- 0.2.3 new-feature test kit ---
		if (TestMod.PAXEL != null) {
			safeAddItem(inventory, new ItemStack(TestMod.PAXEL, 1));          // pickaxe + axe, iron tier
			safeAddItem(inventory, new ItemStack(TestMod.DYNAMIC_TOOL, 1));   // diamond tier while undamaged
			safeAddItem(inventory, new ItemStack(TestMod.CODE_LAYERED, 16));  // layered sprite, no model JSON
			safeAddItem(inventory, new ItemStack(TestMod.FACTORY_BLOCK, 16)); // built via of(Ctor::new)
			safeAddItem(inventory, new ItemStack(TestMod.FACING_BLOCK, 16));  // .facing() rotation
		}
		// Vanilla ores to prove custom tools now harvest VANILLA blocks (the 0.2.3 headline fix).
		// PAXEL is iron-tier: mines iron/gold/diamond ore, but NOT obsidian (needs diamond).
		// DYNAMIC_TOOL is diamond-tier while undamaged: it mines the obsidian too.
		safeAddItem(inventory, new ItemStack(Block.IRON_ORE, 16));
		safeAddItem(inventory, new ItemStack(Block.GOLD_ORE, 16));
		safeAddItem(inventory, new ItemStack(Block.DIAMOND_ORE, 16));
		safeAddItem(inventory, new ItemStack(Block.OBSIDIAN, 16));
		safeAddItem(inventory, new ItemStack(Block.LOG, 16));

		// Server-side self-checks (logged so headless runs verify the asset platform):
		// tag resolution incl. a vanilla name, and a >15 state index round-trip through
		// setBlockMeta + xmeta (the lamp has 20 states; index 17 = lit=true, age=7).
		boolean oreTagged = com.periut.retroapi.tag.RetroTags.isIn(
			TestMod.TAGGED_ORE, com.periut.retroapi.tag.RetroTool.PICKAXE.mineableTag());
		boolean glassTagged = com.periut.retroapi.tag.RetroTags.isIn(
			Block.GLASS, com.periut.retroapi.tag.RetroTool.PICKAXE.mineableTag());
		TestMod.LOGGER.info("[asset-platform] mineable/pickaxe: tagged_ore={} minecraft:glass={} {}",
			oreTagged, glassTagged, (oreTagged && glassTagged) ? "PASS" : "FAIL");

		// Tier gating: needs_iron_tool.json marks the tagged ore; vanilla tools infer
		// their tier from their material, so a stone pick must fail and an iron pick pass.
		com.periut.retroapi.tag.RetroToolTier required =
			com.periut.retroapi.tag.RetroTags.requiredTier(TestMod.TAGGED_ORE);
		boolean stoneTooLow = !com.periut.retroapi.tag.RetroToolTier.of(Item.STONE_PICKAXE).isAtLeast(required);
		boolean ironEnough = com.periut.retroapi.tag.RetroToolTier.of(Item.IRON_PICKAXE).isAtLeast(required);
		TestMod.LOGGER.info("[asset-platform] needs_iron_tool: required={} stoneBlocked={} ironAllowed={} {}",
			required, stoneTooLow, ironEnough,
			(required == com.periut.retroapi.tag.RetroToolTier.IRON && stoneTooLow && ironEnough) ? "PASS" : "FAIL");

		// 26.1.2 item definition: the composite loader ran at registration (common code),
		// so def_item got a base texture tracked for the atlas. The sprite getter and the
		// handheld flag are both client-only methods, so the server can only confirm the
		// definition was parsed and a texture assigned; the apple overlay + tool pose are
		// verified visually on the client.
		com.periut.retroapi.register.block.RetroTexture defTex =
			com.periut.retroapi.register.block.RetroTextures.getTrackedTexture(TestMod.DEF_ITEM);
		boolean defTracked = defTex != null
			&& com.periut.retroapi.register.block.RetroTextures.getItemTextures().contains(defTex);
		TestMod.LOGGER.info("[asset-platform] item definition (26.1.2 composite): tracked={} {}",
			defTracked, defTracked ? "PASS" : "FAIL");

		// Data components: set, copy, and nbt round-trip must all preserve the value.
		ItemStack compStack = new ItemStack(Item.STICK);
		com.periut.retroapi.component.RetroComponents.set(compStack, TestMod.TEST_COUNT, 42);
		ItemStack compCopy = compStack.copy();
		boolean copyOk = com.periut.retroapi.component.RetroComponents.get(compCopy, TestMod.TEST_COUNT) == 42;
		net.minecraft.nbt.NbtCompound compNbt = new net.minecraft.nbt.NbtCompound();
		compStack.writeNbt(compNbt);
		ItemStack compRestored = new ItemStack(0, 0, 0);
		compRestored.readNbt(compNbt);
		boolean nbtOk = com.periut.retroapi.component.RetroComponents.get(compRestored, TestMod.TEST_COUNT) == 42;
		// Sidecar-style round-trip: strip to a sidecar entry (id/count/damage + components),
		// then rebuild and restore, the path the player inventory and dropped items use.
		net.minecraft.nbt.NbtCompound sidecar = new net.minecraft.nbt.NbtCompound();
		com.periut.retroapi.component.ComponentNbt.write(
			(com.periut.retroapi.component.RetroComponentHolder) (Object) compStack, sidecar);
		ItemStack sidecarStack = new ItemStack(Item.STICK.id, 1, 0);
		com.periut.retroapi.component.ComponentNbt.read(
			(com.periut.retroapi.component.RetroComponentHolder) (Object) sidecarStack, sidecar);
		boolean sidecarOk = com.periut.retroapi.component.RetroComponents.get(sidecarStack, TestMod.TEST_COUNT) == 42;
		// Richer types: a record (int pair) and a list, through the same nbt round-trip.
		ItemStack rich = new ItemStack(Item.STICK);
		com.periut.retroapi.component.RetroComponents.set(rich, TestMod.TEST_PAIR, new int[]{7, 9});
		com.periut.retroapi.component.RetroComponents.set(rich, TestMod.TEST_LIST, java.util.Arrays.asList(1, 2, 3));
		net.minecraft.nbt.NbtCompound richNbt = new net.minecraft.nbt.NbtCompound();
		rich.writeNbt(richNbt);
		ItemStack richBack = new ItemStack(0, 0, 0);
		richBack.readNbt(richNbt);
		int[] pair = com.periut.retroapi.component.RetroComponents.get(richBack, TestMod.TEST_PAIR);
		java.util.List<Integer> lst = com.periut.retroapi.component.RetroComponents.get(richBack, TestMod.TEST_LIST);
		boolean richOk = pair[0] == 7 && pair[1] == 9 && lst.size() == 3 && lst.get(2) == 3;
		TestMod.LOGGER.info("[asset-platform] components: copy={} nbt={} sidecar={} richTypes={} {}",
			copyOk, nbtOk, sidecarOk, richOk, (copyOk && nbtOk && sidecarOk && richOk) ? "PASS" : "FAIL");

		int lx = (int) self.x, ly = Math.min(125, (int) self.y + 3), lz = (int) self.z;
		world.setBlock(lx, ly, lz, TestMod.LAMP.id);
		com.periut.retroapi.state.RetroBlockState target =
			com.periut.retroapi.state.RetroStates.fromIndex(TestMod.LAMP, 17);
		com.periut.retroapi.state.RetroStates.set(world, lx, ly, lz, target);
		com.periut.retroapi.state.RetroBlockState readBack =
			com.periut.retroapi.state.RetroStates.get(world, lx, ly, lz);
		TestMod.LOGGER.info("[asset-platform] state round-trip: wrote {} read {} {}",
			target, readBack, (readBack == target) ? "PASS" : "FAIL");
		world.setBlock(lx, ly, lz, 0);

		// Grant the root achievement so the toast pops + the icon un-greys (tests addStat + incrementStat).
		if (TestAchievements.ROOT != null) {
			RetroAchievements.grant(TestAchievements.ROOT, self);
		}

		// Spawn a zeve test mob right where the player stands (fires on every join/world load).
		// Exercises the full modded-entity path: registration, tracking, OSL mob spawn packet,
		// biped render, entity sidecar.
		ZeveEntity zeve = new ZeveEntity(world);
		zeve.setPositionAndAngles(self.x, self.y, self.z, self.yaw, 0.0F);
		world.spawnEntity(zeve);

		// Spawn chests filled with the 200 numbered blocks
		int px = (int) self.x;
		int py = (int) self.y;
		int pz = (int) self.z;
		int blockIndex = 0;
		int blockChestCount = (TestMod.BLOCK_COUNT + 26) / 27; // 27 slots per chest
		for (int c = 0; c < blockChestCount; c++) {
			int cx = px + 2 + (c % 8);
			int cz = pz + 2 + (c / 8);
			int cy = py;

			world.setBlock(cx, cy, cz, Block.CHEST.id);
			if (world.getBlockEntity(cx, cy, cz) instanceof ChestBlockEntity chest) {
				for (int slot = 0; slot < 27 && blockIndex < TestMod.BLOCK_COUNT; slot++, blockIndex++) {
					Block block = TestMod.BLOCKS[blockIndex];
					if (block != null && block.id >= 0 && block.id < Item.ITEMS.length && Item.ITEMS[block.id] != null) {
						chest.setStack(slot, new ItemStack(block, 64));
					} else {
						TestMod.LOGGER.error("Block {} (index {}) has id {} with no Item.BY_ID entry!",
							block, blockIndex, block != null ? block.id : "null");
					}
				}
			}
		}

		// Spawn chests filled with the 200 numbered items (offset row by 2 blocks)
		int itemIndex = 0;
		int itemChestCount = (TestMod.ITEM_COUNT + 26) / 27;
		for (int c = 0; c < itemChestCount; c++) {
			int cx = px + 2 + (c % 8);
			int cz = pz + 2 + ((blockChestCount + 7) / 8) + 1 + (c / 8);
			int cy = py;

			world.setBlock(cx, cy, cz, Block.CHEST.id);
			if (world.getBlockEntity(cx, cy, cz) instanceof ChestBlockEntity chest) {
				for (int slot = 0; slot < 27 && itemIndex < TestMod.ITEM_COUNT; slot++, itemIndex++) {
					Item item = TestMod.ITEMS[itemIndex];
					if (item != null && item.id >= 0 && item.id < Item.ITEMS.length && Item.ITEMS[item.id] != null) {
						chest.setStack(slot, new ItemStack(item, 64));
					} else {
						TestMod.LOGGER.error("Item {} (index {}) has id {} with no Item.BY_ID entry!",
							item, itemIndex, item != null ? item.id : "null");
					}
				}
			}
		}

		TestMod.LOGGER.info("Gave test items, spawned a zeve, and spawned {} block chests + {} item chests",
			blockChestCount, itemChestCount);
	}
}
