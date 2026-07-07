package com.periut.retroapi.tag;

import net.minecraft.item.Item;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps modern flattening-style item names ({@code minecraft:iron_ingot}, {@code minecraft:oak_boat}, ...)
 * to Beta 1.7.3 items, the item-side counterpart of {@link VanillaBlockNames}. This lets item tag data
 * files copied from modern Minecraft (or written in StationAPI's {@code data/{ns}/stationapi/tags/items/}
 * layout) resolve their vanilla entries.
 *
 * <p>Both the modern name and, where it differs, the historical or StationAPI spelling are registered
 * (e.g. {@code cod} and {@code raw_fish}; {@code slime_ball} and {@code slimeball}; {@code golden_pickaxe}
 * and {@code gold_pickaxe}). Names that flatten from item metadata (the dye family, records) map to the
 * single beta item, mirroring how {@link VanillaBlockNames} handles metadata-flattened blocks.</p>
 */
public final class VanillaItemNames {

	private static final Map<String, Item> BY_NAME = new HashMap<>();

	private VanillaItemNames() {}

	static {
		// Tools / weapons (field name lowercased == modern name for these).
		put(Item.WOODEN_SWORD, "wooden_sword");
		put(Item.WOODEN_SHOVEL, "wooden_shovel");
		put(Item.WOODEN_PICKAXE, "wooden_pickaxe");
		put(Item.WOODEN_AXE, "wooden_axe");
		put(Item.WOODEN_HOE, "wooden_hoe");
		put(Item.STONE_SWORD, "stone_sword");
		put(Item.STONE_SHOVEL, "stone_shovel");
		put(Item.STONE_PICKAXE, "stone_pickaxe");
		put(Item.STONE_AXE, "stone_axe");
		put(Item.STONE_HOE, "stone_hoe");
		put(Item.IRON_SWORD, "iron_sword");
		put(Item.IRON_SHOVEL, "iron_shovel");
		put(Item.IRON_PICKAXE, "iron_pickaxe");
		put(Item.IRON_AXE, "iron_axe");
		put(Item.IRON_HOE, "iron_hoe");
		put(Item.DIAMOND_SWORD, "diamond_sword");
		put(Item.DIAMOND_SHOVEL, "diamond_shovel");
		put(Item.DIAMOND_PICKAXE, "diamond_pickaxe");
		put(Item.DIAMOND_AXE, "diamond_axe");
		put(Item.DIAMOND_HOE, "diamond_hoe");
		// Beta "gold" tools flatten to "golden_" in modern; register both.
		put(Item.GOLDEN_SWORD, "golden_sword", "gold_sword");
		put(Item.GOLDEN_SHOVEL, "golden_shovel", "gold_shovel");
		put(Item.GOLDEN_PICKAXE, "golden_pickaxe", "gold_pickaxe");
		put(Item.GOLDEN_AXE, "golden_axe", "gold_axe");
		put(Item.GOLDEN_HOE, "golden_hoe", "gold_hoe");
		put(Item.FLINT_AND_STEEL, "flint_and_steel");
		put(Item.BOW, "bow");
		put(Item.FISHING_ROD, "fishing_rod");
		put(Item.SHEARS, "shears");

		// Armor.
		put(Item.LEATHER_HELMET, "leather_helmet");
		put(Item.LEATHER_CHESTPLATE, "leather_chestplate");
		put(Item.LEATHER_LEGGINGS, "leather_leggings");
		put(Item.LEATHER_BOOTS, "leather_boots");
		put(Item.CHAIN_HELMET, "chainmail_helmet", "chain_helmet");
		put(Item.CHAIN_CHESTPLATE, "chainmail_chestplate", "chain_chestplate");
		put(Item.CHAIN_LEGGINGS, "chainmail_leggings", "chain_leggings");
		put(Item.CHAIN_BOOTS, "chainmail_boots", "chain_boots");
		put(Item.IRON_HELMET, "iron_helmet");
		put(Item.IRON_CHESTPLATE, "iron_chestplate");
		put(Item.IRON_LEGGINGS, "iron_leggings");
		put(Item.IRON_BOOTS, "iron_boots");
		put(Item.DIAMOND_HELMET, "diamond_helmet");
		put(Item.DIAMOND_CHESTPLATE, "diamond_chestplate");
		put(Item.DIAMOND_LEGGINGS, "diamond_leggings");
		put(Item.DIAMOND_BOOTS, "diamond_boots");
		put(Item.GOLDEN_HELMET, "golden_helmet", "gold_helmet");
		put(Item.GOLDEN_CHESTPLATE, "golden_chestplate", "gold_chestplate");
		put(Item.GOLDEN_LEGGINGS, "golden_leggings", "gold_leggings");
		put(Item.GOLDEN_BOOTS, "golden_boots", "gold_boots");

		// Materials / misc.
		put(Item.APPLE, "apple");
		put(Item.ARROW, "arrow");
		put(Item.COAL, "coal", "charcoal");
		put(Item.DIAMOND, "diamond");
		put(Item.IRON_INGOT, "iron_ingot");
		put(Item.GOLD_INGOT, "gold_ingot");
		put(Item.STICK, "stick");
		put(Item.BOWL, "bowl");
		put(Item.MUSHROOM_STEW, "mushroom_stew", "suspicious_stew");
		put(Item.STRING, "string");
		put(Item.FEATHER, "feather");
		put(Item.GUNPOWDER, "gunpowder");
		put(Item.SEEDS, "wheat_seeds", "seeds");
		put(Item.WHEAT, "wheat");
		put(Item.BREAD, "bread");
		put(Item.FLINT, "flint");
		put(Item.RAW_PORKCHOP, "porkchop", "raw_porkchop");
		put(Item.COOKED_PORKCHOP, "cooked_porkchop");
		put(Item.PAINTING, "painting");
		put(Item.GOLDEN_APPLE, "golden_apple");
		put(Item.SIGN, "oak_sign", "sign");
		put(Item.WOODEN_DOOR, "oak_door", "wooden_door");
		put(Item.BUCKET, "bucket");
		put(Item.WATER_BUCKET, "water_bucket");
		put(Item.LAVA_BUCKET, "lava_bucket");
		put(Item.MILK_BUCKET, "milk_bucket");
		put(Item.MINECART, "minecart");
		put(Item.SADDLE, "saddle");
		put(Item.IRON_DOOR, "iron_door");
		put(Item.REDSTONE, "redstone");
		put(Item.SNOWBALL, "snowball");
		put(Item.BOAT, "oak_boat", "boat");
		put(Item.LEATHER, "leather");
		put(Item.BRICK, "brick");
		put(Item.CLAY, "clay_ball", "clay");
		put(Item.SUGAR_CANE, "sugar_cane", "reeds");
		put(Item.PAPER, "paper");
		put(Item.BOOK, "book");
		put(Item.SLIMEBALL, "slime_ball", "slimeball");
		put(Item.CHEST_MINECART, "chest_minecart", "minecart_chest");
		put(Item.FURNACE_MINECART, "furnace_minecart", "minecart_furnace");
		put(Item.EGG, "egg");
		put(Item.COMPASS, "compass");
		put(Item.CLOCK, "clock");
		put(Item.GLOWSTONE_DUST, "glowstone_dust");
		put(Item.RAW_FISH, "cod", "raw_fish", "fish");
		put(Item.COOKED_FISH, "cooked_cod", "cooked_fish");
		// The dye family flattens per-metadata; every modern split maps to the one beta item.
		put(Item.DYE, "dye", "lapis_lazuli", "ink_sac", "cocoa_beans", "bone_meal");
		put(Item.BONE, "bone");
		put(Item.SUGAR, "sugar");
		put(Item.CAKE, "cake");
		put(Item.BED, "red_bed", "bed", "white_bed");
		put(Item.REPEATER, "repeater", "unpowered_repeater");
		put(Item.COOKIE, "cookie");
		put(Item.MAP, "filled_map", "map");
		put(Item.RECORD_THIRTEEN, "music_disc_13", "record_13", "record_thirteen");
		put(Item.RECORD_CAT, "music_disc_cat", "record_cat");
	}

	private static void put(Item item, String... names) {
		for (String name : names) {
			BY_NAME.put(name, item);
		}
	}

	/**
	 * Resolves a {@code minecraft:<name>} (with or without the namespace) to its beta item, or null if
	 * the name has no beta equivalent (post-beta items fall in here, which is exactly what makes modern
	 * item tag files copy across safely: unknown entries just skip).
	 */
	public static Item resolve(String name) {
		int colon = name.indexOf(':');
		if (colon >= 0) {
			name = name.substring(colon + 1);
		}
		return BY_NAME.get(name);
	}
}
