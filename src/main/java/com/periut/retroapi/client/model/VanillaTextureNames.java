package com.periut.retroapi.client.model;

import net.minecraft.block.Block;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps modern texture ids ({@code minecraft:block/cobblestone}, ...) to vanilla terrain
 * atlas sprite indices, so model JSONs copied from modern packs (or StationAPI mods)
 * reference vanilla textures by name and just work; nobody re-ships cobblestone.png.
 *
 * <p>Sprites are sampled from the blocks' own {@code getTexture(face)} (0 = bottom,
 * 1 = top, 2+ = sides), evaluated once on first use. Both the modern name and, where it
 * differs, the historical flattening name are registered. Unknown names fall through to
 * the normal mod-file path and warn there, so coverage gaps are loud, not invisible.</p>
 */
final class VanillaTextureNames {

	private static Map<String, Integer> sprites = null;

	private VanillaTextureNames() {}

	/** The terrain sprite for a path like {@code block/cobblestone}, or null when unknown. */
	static Integer blockSprite(String path) {
		if (path.startsWith("block/")) {
			path = path.substring("block/".length());
		} else if (path.startsWith("blocks/")) {
			path = path.substring("blocks/".length());
		}
		return table().get(path);
	}

	private static synchronized Map<String, Integer> table() {
		if (sprites != null) {
			return sprites;
		}
		Map<String, Integer> t = new HashMap<>();

		all(t, Block.STONE, "stone");
		put(t, Block.GRASS_BLOCK.getTexture(1), "grass_block_top");
		put(t, Block.GRASS_BLOCK.getTexture(2), "grass_block_side");
		all(t, Block.DIRT, "dirt");
		all(t, Block.COBBLESTONE, "cobblestone");
		all(t, Block.PLANKS, "oak_planks", "planks");
		all(t, Block.BEDROCK, "bedrock");
		all(t, Block.SAND, "sand");
		all(t, Block.GRAVEL, "gravel");
		all(t, Block.GOLD_ORE, "gold_ore");
		all(t, Block.IRON_ORE, "iron_ore");
		all(t, Block.COAL_ORE, "coal_ore");
		put(t, Block.LOG.getTexture(2), "oak_log", "log_oak", "log");
		put(t, Block.LOG.getTexture(1), "oak_log_top", "log_oak_top", "log_top");
		all(t, Block.LEAVES, "oak_leaves", "leaves");
		all(t, Block.SPONGE, "sponge");
		all(t, Block.GLASS, "glass");
		all(t, Block.LAPIS_ORE, "lapis_ore");
		all(t, Block.LAPIS_BLOCK, "lapis_block");
		put(t, Block.SANDSTONE.getTexture(2), "sandstone", "sandstone_side");
		put(t, Block.SANDSTONE.getTexture(1), "sandstone_top");
		put(t, Block.SANDSTONE.getTexture(0), "sandstone_bottom");
		all(t, Block.NOTE_BLOCK, "note_block", "noteblock");
		all(t, Block.COBWEB, "cobweb", "web");
		all(t, Block.WOOL, "white_wool", "wool", "wool_colored_white");
		all(t, Block.GOLD_BLOCK, "gold_block");
		all(t, Block.IRON_BLOCK, "iron_block");
		all(t, Block.BRICKS, "bricks", "brick");
		put(t, Block.TNT.getTexture(2), "tnt_side");
		put(t, Block.TNT.getTexture(1), "tnt_top");
		put(t, Block.TNT.getTexture(0), "tnt_bottom");
		put(t, Block.BOOKSHELF.getTexture(2), "bookshelf");
		all(t, Block.MOSSY_COBBLESTONE, "mossy_cobblestone", "cobblestone_mossy");
		all(t, Block.OBSIDIAN, "obsidian");
		all(t, Block.DIAMOND_ORE, "diamond_ore");
		all(t, Block.DIAMOND_BLOCK, "diamond_block");
		put(t, Block.CRAFTING_TABLE.getTexture(1), "crafting_table_top");
		put(t, Block.CRAFTING_TABLE.getTexture(2), "crafting_table_front");
		put(t, Block.CRAFTING_TABLE.getTexture(4), "crafting_table_side");
		put(t, Block.FURNACE.getTexture(2), "furnace_front");
		put(t, Block.FURNACE.getTexture(4), "furnace_side");
		put(t, Block.FURNACE.getTexture(1), "furnace_top");
		all(t, Block.REDSTONE_ORE, "redstone_ore");
		all(t, Block.SNOW_BLOCK, "snow", "snow_block");
		all(t, Block.ICE, "ice");
		all(t, Block.CLAY, "clay");
		all(t, Block.JUKEBOX, "jukebox_side");
		put(t, Block.JUKEBOX.getTexture(1), "jukebox_top");
		put(t, Block.PUMPKIN.getTexture(2), "pumpkin_side");
		put(t, Block.PUMPKIN.getTexture(1), "pumpkin_top");
		all(t, Block.NETHERRACK, "netherrack");
		all(t, Block.SOUL_SAND, "soul_sand");
		all(t, Block.GLOWSTONE, "glowstone");

		sprites = t;
		return t;
	}

	/** Registers one sprite under several names. */
	private static void put(Map<String, Integer> t, int sprite, String... names) {
		for (String name : names) {
			t.put(name, sprite);
		}
	}

	/** Registers a block's face-0 sprite (blocks that look the same on every face). */
	private static void all(Map<String, Integer> t, Block block, String... names) {
		put(t, block.getTexture(0), names);
	}
}
