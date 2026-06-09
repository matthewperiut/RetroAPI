package com.periut.retroapi.tag;

import net.minecraft.block.Block;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps modern flattening-style names ({@code minecraft:stone}, {@code minecraft:oak_log}, ...)
 * to Beta 1.7.3 blocks, so tag data files copied from modern Minecraft resolve their vanilla
 * entries. Mirrors the flattening schema StationAPI uses for world conversion.
 *
 * <p>Both the modern name and, where it differs, the historical 1.13-flattening name are
 * registered (e.g. {@code grass} and {@code short_grass}; {@code oak_planks} and {@code planks}),
 * so files from any modern version resolve. Names that flatten from metadata (colored wool,
 * wood species) all map to the single beta block.</p>
 */
public final class VanillaBlockNames {

	private static final Map<String, Block> BY_NAME = new HashMap<>();

	private VanillaBlockNames() {}

	static {
		put(Block.STONE, "stone");
		put(Block.GRASS_BLOCK, "grass_block");
		put(Block.DIRT, "dirt");
		put(Block.COBBLESTONE, "cobblestone");
		put(Block.PLANKS, "oak_planks", "planks");
		put(Block.SAPLING, "oak_sapling", "sapling", "spruce_sapling", "birch_sapling");
		put(Block.BEDROCK, "bedrock");
		put(Block.FLOWING_WATER, "flowing_water");
		put(Block.WATER, "water");
		put(Block.FLOWING_LAVA, "flowing_lava");
		put(Block.LAVA, "lava");
		put(Block.SAND, "sand");
		put(Block.GRAVEL, "gravel");
		put(Block.GOLD_ORE, "gold_ore");
		put(Block.IRON_ORE, "iron_ore");
		put(Block.COAL_ORE, "coal_ore");
		put(Block.LOG, "oak_log", "log", "spruce_log", "birch_log");
		put(Block.LEAVES, "oak_leaves", "leaves", "spruce_leaves", "birch_leaves");
		put(Block.SPONGE, "sponge");
		put(Block.GLASS, "glass");
		put(Block.LAPIS_ORE, "lapis_ore");
		put(Block.LAPIS_BLOCK, "lapis_block");
		put(Block.DISPENSER, "dispenser");
		put(Block.SANDSTONE, "sandstone");
		put(Block.NOTE_BLOCK, "note_block");
		put(Block.BED, "red_bed", "bed", "white_bed");
		put(Block.POWERED_RAIL, "powered_rail", "golden_rail");
		put(Block.DETECTOR_RAIL, "detector_rail");
		put(Block.STICKY_PISTON, "sticky_piston");
		put(Block.COBWEB, "cobweb", "web");
		put(Block.GRASS, "short_grass", "grass", "tall_grass", "fern");
		put(Block.DEAD_BUSH, "dead_bush");
		put(Block.PISTON, "piston");
		put(Block.PISTON_HEAD, "piston_head");
		put(Block.WOOL, "white_wool", "wool", "orange_wool", "magenta_wool", "light_blue_wool",
			"yellow_wool", "lime_wool", "pink_wool", "gray_wool", "light_gray_wool", "cyan_wool",
			"purple_wool", "blue_wool", "brown_wool", "green_wool", "red_wool", "black_wool");
		put(Block.MOVING_PISTON, "moving_piston");
		put(Block.DANDELION, "dandelion");
		put(Block.ROSE, "poppy", "rose");
		put(Block.BROWN_MUSHROOM, "brown_mushroom");
		put(Block.RED_MUSHROOM, "red_mushroom");
		put(Block.GOLD_BLOCK, "gold_block");
		put(Block.IRON_BLOCK, "iron_block");
		put(Block.DOUBLE_SLAB, "double_stone_slab");
		put(Block.SLAB, "smooth_stone_slab", "stone_slab");
		put(Block.BRICKS, "bricks", "brick_block");
		put(Block.TNT, "tnt");
		put(Block.BOOKSHELF, "bookshelf");
		put(Block.MOSSY_COBBLESTONE, "mossy_cobblestone");
		put(Block.OBSIDIAN, "obsidian");
		put(Block.TORCH, "torch");
		put(Block.FIRE, "fire");
		put(Block.SPAWNER, "spawner", "mob_spawner");
		put(Block.WOODEN_STAIRS, "oak_stairs");
		put(Block.CHEST, "chest");
		put(Block.REDSTONE_WIRE, "redstone_wire");
		put(Block.DIAMOND_ORE, "diamond_ore");
		put(Block.DIAMOND_BLOCK, "diamond_block");
		put(Block.CRAFTING_TABLE, "crafting_table");
		put(Block.WHEAT, "wheat");
		put(Block.FARMLAND, "farmland");
		put(Block.FURNACE, "furnace");
		put(Block.LIT_FURNACE, "lit_furnace");
		put(Block.SIGN, "oak_sign", "sign", "standing_sign");
		put(Block.DOOR, "oak_door", "wooden_door");
		put(Block.LADDER, "ladder");
		put(Block.RAIL, "rail");
		put(Block.COBBLESTONE_STAIRS, "cobblestone_stairs", "stone_stairs");
		put(Block.WALL_SIGN, "oak_wall_sign", "wall_sign");
		put(Block.LEVER, "lever");
		put(Block.STONE_PRESSURE_PLATE, "stone_pressure_plate");
		put(Block.IRON_DOOR, "iron_door");
		put(Block.WOODEN_PRESSURE_PLATE, "oak_pressure_plate", "wooden_pressure_plate");
		put(Block.REDSTONE_ORE, "redstone_ore");
		put(Block.LIT_REDSTONE_ORE, "lit_redstone_ore");
		put(Block.REDSTONE_TORCH, "unlit_redstone_torch");
		put(Block.LIT_REDSTONE_TORCH, "redstone_torch");
		put(Block.BUTTON, "stone_button");
		put(Block.SNOW, "snow");
		put(Block.ICE, "ice");
		put(Block.SNOW_BLOCK, "snow_block");
		put(Block.CACTUS, "cactus");
		put(Block.CLAY, "clay");
		put(Block.SUGAR_CANE, "sugar_cane", "reeds");
		put(Block.JUKEBOX, "jukebox");
		put(Block.FENCE, "oak_fence", "fence");
		put(Block.PUMPKIN, "pumpkin", "carved_pumpkin");
		put(Block.NETHERRACK, "netherrack");
		put(Block.SOUL_SAND, "soul_sand");
		put(Block.GLOWSTONE, "glowstone");
		put(Block.NETHER_PORTAL, "nether_portal", "portal");
		put(Block.JACK_O_LANTERN, "jack_o_lantern", "lit_pumpkin");
		put(Block.CAKE, "cake");
		put(Block.REPEATER, "repeater", "unpowered_repeater");
		put(Block.POWERED_REPEATER, "powered_repeater");
		put(Block.TRAPDOOR, "oak_trapdoor", "trapdoor");
	}

	private static void put(Block block, String... names) {
		for (String name : names) {
			BY_NAME.put(name, block);
		}
	}

	/**
	 * Resolves a {@code minecraft:<name>} (with or without the namespace) to its beta block,
	 * or null if the name has no beta equivalent (post-beta blocks fall in here, which is
	 * exactly what makes modern tag files copy across safely: unknown entries just skip).
	 */
	public static Block resolve(String name) {
		int colon = name.indexOf(':');
		if (colon >= 0) {
			name = name.substring(colon + 1);
		}
		return BY_NAME.get(name);
	}
}
