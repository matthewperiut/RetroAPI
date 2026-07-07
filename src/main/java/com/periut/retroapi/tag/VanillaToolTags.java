package com.periut.retroapi.tag;

import net.minecraft.block.Block;

/**
 * The default {@code mineable/<tool>} and {@code needs_<tier>_tool} membership for VANILLA beta blocks,
 * reproducing beta 1.7.3's own {@code MinecraftPickaxe}/{@code MinecraftAxe}/{@code MinecraftShovel}
 * effective-block lists and harvest levels. Registered once at init by {@link com.periut.retroapi.RetroAPI}
 * when StationAPI is <b>absent</b> (StationAPI already ships this exact set as data tags, so RetroAPI stands
 * back and lets it own vanilla harvesting — see the StationAPI compat notes).
 *
 * <p>This is the fix for "custom tools only worked on modded blocks": before, no vanilla block was in any
 * RetroAPI tag, so a {@code .tool(PICKAXE).tier(IRON)} plain item had nothing to bite on vanilla stone or
 * ore. With these defaults, the same harvest hook that serves modded ores now serves vanilla blocks too.</p>
 *
 * <p>Under the decoupled harvest rules, a {@code mineable/<tool>} tag only grants SPEED; a block still needs
 * a tool to DROP only because its material says so (stone/metal) or a {@code needs_<tier>_tool} tag does.
 * So listing hand-harvestable wood/dirt blocks in {@code mineable/axe}/{@code mineable/shovel} makes custom
 * axes and shovels faster on them without ever gating their hand-harvest, matching vanilla exactly.</p>
 */
public final class VanillaToolTags {

	private static boolean registered = false;

	private VanillaToolTags() {}

	/** Idempotent: safe to call more than once. */
	public static void registerDefaults() {
		if (registered) {
			return;
		}
		registered = true;

		// --- mineable/pickaxe: stone- and metal-material blocks (these need a pickaxe to drop) ---
		mineable(RetroTool.PICKAXE,
			Block.STONE, Block.COBBLESTONE, Block.MOSSY_COBBLESTONE, Block.SANDSTONE, Block.BRICKS,
			Block.DOUBLE_SLAB, Block.SLAB, Block.COBBLESTONE_STAIRS,
			Block.COAL_ORE, Block.IRON_ORE, Block.GOLD_ORE, Block.DIAMOND_ORE, Block.LAPIS_ORE,
			Block.REDSTONE_ORE, Block.LIT_REDSTONE_ORE,
			Block.IRON_BLOCK, Block.GOLD_BLOCK, Block.DIAMOND_BLOCK, Block.LAPIS_BLOCK,
			Block.ICE, Block.NETHERRACK, Block.OBSIDIAN,
			Block.DISPENSER, Block.FURNACE, Block.LIT_FURNACE, Block.SPAWNER,
			Block.IRON_DOOR, Block.STONE_PRESSURE_PLATE, Block.BUTTON,
			Block.RAIL, Block.POWERED_RAIL, Block.DETECTOR_RAIL);

		// --- mineable/axe: wood-material blocks (hand-harvestable; tag only speeds an axe up) ---
		mineable(RetroTool.AXE,
			Block.PLANKS, Block.LOG, Block.BOOKSHELF, Block.CHEST, Block.CRAFTING_TABLE,
			Block.WOODEN_STAIRS, Block.FENCE, Block.NOTE_BLOCK, Block.JUKEBOX,
			Block.WOODEN_PRESSURE_PLATE, Block.PUMPKIN, Block.JACK_O_LANTERN, Block.TRAPDOOR);

		// --- mineable/shovel: dirt/sand-material blocks (hand-harvestable; tag only speeds a shovel up) ---
		mineable(RetroTool.SHOVEL,
			Block.GRASS_BLOCK, Block.DIRT, Block.SAND, Block.GRAVEL, Block.CLAY,
			Block.SNOW, Block.SNOW_BLOCK, Block.FARMLAND, Block.SOUL_SAND);

		// --- tool tiers, straight from beta MinecraftPickaxe.canHarvestBlock ---
		needs(RetroToolTier.STONE,
			Block.IRON_ORE, Block.IRON_BLOCK, Block.LAPIS_ORE, Block.LAPIS_BLOCK);
		needs(RetroToolTier.IRON,
			Block.GOLD_ORE, Block.GOLD_BLOCK, Block.DIAMOND_ORE, Block.DIAMOND_BLOCK,
			Block.REDSTONE_ORE, Block.LIT_REDSTONE_ORE);
		needs(RetroToolTier.DIAMOND,
			Block.OBSIDIAN);
	}

	private static void mineable(RetroTool tool, Block... blocks) {
		RetroTags.addToTag(tool.mineableTag(), blocks);
	}

	private static void needs(RetroToolTier tier, Block... blocks) {
		RetroTags.addToTag(tier.needsTag(), blocks);
	}
}
