package com.periut.retroapi.client.model;

import net.minecraft.item.Item;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps modern item texture ids ({@code minecraft:item/apple}, ...) to vanilla items.png
 * sprite indices, the items.png counterpart of {@link VanillaTextureNames}: item model
 * layers reference vanilla item textures by name and resolve straight off the vanilla
 * atlas, nobody re-ships apple.png.
 *
 * <p>Both the modern (post-flattening) name and, where it differs, the historical name
 * are registered. Dyes map per-subtype (bone_meal, lapis_lazuli, ...). Unknown names
 * fall through to the normal mod-file path, so coverage gaps warn rather than vanish.</p>
 */
final class VanillaItemTextureNames {

	private static Map<String, Integer> sprites = null;

	private VanillaItemTextureNames() {}

	/**
	 * The items.png sprite for a path like {@code item/apple}, or null when unknown.
	 * Item sprite ids come from {@code Item.getTextureId}, which is a CLIENT-only method,
	 * so this returns null on the server (where item sprites are never needed anyway, and
	 * the caller falls back to a normal extended slot).
	 */
	static Integer itemSprite(String path) {
		if (net.fabricmc.loader.api.FabricLoader.getInstance().getEnvironmentType()
				!= net.fabricmc.api.EnvType.CLIENT) {
			return null;
		}
		if (path.startsWith("item/")) {
			path = path.substring("item/".length());
		} else if (path.startsWith("items/")) {
			path = path.substring("items/".length());
		}
		return table().get(path);
	}

	private static synchronized Map<String, Integer> table() {
		if (sprites != null) {
			return sprites;
		}
		Map<String, Integer> t = new HashMap<>();

		put(t, Item.IRON_SHOVEL, "iron_shovel");
		put(t, Item.IRON_PICKAXE, "iron_pickaxe");
		put(t, Item.IRON_AXE, "iron_axe");
		put(t, Item.FLINT_AND_STEEL, "flint_and_steel");
		put(t, Item.APPLE, "apple");
		put(t, Item.BOW, "bow");
		put(t, Item.ARROW, "arrow");
		put(t, Item.COAL, "coal");
		put(t, Item.DIAMOND, "diamond");
		put(t, Item.IRON_INGOT, "iron_ingot");
		put(t, Item.GOLD_INGOT, "gold_ingot");
		put(t, Item.IRON_SWORD, "iron_sword");
		put(t, Item.WOODEN_SWORD, "wooden_sword");
		put(t, Item.WOODEN_SHOVEL, "wooden_shovel");
		put(t, Item.WOODEN_PICKAXE, "wooden_pickaxe");
		put(t, Item.WOODEN_AXE, "wooden_axe");
		put(t, Item.STONE_SWORD, "stone_sword");
		put(t, Item.STONE_SHOVEL, "stone_shovel");
		put(t, Item.STONE_PICKAXE, "stone_pickaxe");
		put(t, Item.STONE_AXE, "stone_axe");
		put(t, Item.DIAMOND_SWORD, "diamond_sword");
		put(t, Item.DIAMOND_SHOVEL, "diamond_shovel");
		put(t, Item.DIAMOND_PICKAXE, "diamond_pickaxe");
		put(t, Item.DIAMOND_AXE, "diamond_axe");
		put(t, Item.STICK, "stick");
		put(t, Item.BOWL, "bowl");
		put(t, Item.MUSHROOM_STEW, "mushroom_stew");
		put(t, Item.GOLDEN_SWORD, "golden_sword");
		put(t, Item.GOLDEN_SHOVEL, "golden_shovel");
		put(t, Item.GOLDEN_PICKAXE, "golden_pickaxe");
		put(t, Item.GOLDEN_AXE, "golden_axe");
		put(t, Item.STRING, "string");
		put(t, Item.FEATHER, "feather");
		put(t, Item.GUNPOWDER, "gunpowder");
		put(t, Item.WOODEN_HOE, "wooden_hoe");
		put(t, Item.STONE_HOE, "stone_hoe");
		put(t, Item.IRON_HOE, "iron_hoe");
		put(t, Item.DIAMOND_HOE, "diamond_hoe");
		put(t, Item.GOLDEN_HOE, "golden_hoe");
		put(t, Item.WHEAT, "wheat");
		put(t, Item.BREAD, "bread");
		put(t, Item.LEATHER_HELMET, "leather_helmet");
		put(t, Item.LEATHER_CHESTPLATE, "leather_chestplate");
		put(t, Item.LEATHER_LEGGINGS, "leather_leggings");
		put(t, Item.LEATHER_BOOTS, "leather_boots");
		put(t, Item.IRON_HELMET, "iron_helmet");
		put(t, Item.IRON_CHESTPLATE, "iron_chestplate");
		put(t, Item.IRON_LEGGINGS, "iron_leggings");
		put(t, Item.IRON_BOOTS, "iron_boots");
		put(t, Item.DIAMOND_HELMET, "diamond_helmet");
		put(t, Item.DIAMOND_CHESTPLATE, "diamond_chestplate");
		put(t, Item.DIAMOND_LEGGINGS, "diamond_leggings");
		put(t, Item.DIAMOND_BOOTS, "diamond_boots");
		put(t, Item.GOLDEN_HELMET, "golden_helmet");
		put(t, Item.GOLDEN_CHESTPLATE, "golden_chestplate");
		put(t, Item.GOLDEN_LEGGINGS, "golden_leggings");
		put(t, Item.GOLDEN_BOOTS, "golden_boots");
		put(t, Item.FLINT, "flint");
		put(t, Item.PAINTING, "painting");
		put(t, Item.GOLDEN_APPLE, "golden_apple");
		put(t, Item.BUCKET, "bucket");
		put(t, Item.WATER_BUCKET, "water_bucket");
		put(t, Item.LAVA_BUCKET, "lava_bucket");
		put(t, Item.MINECART, "minecart");
		put(t, Item.SADDLE, "saddle");
		put(t, Item.IRON_DOOR, "iron_door");
		put(t, Item.REDSTONE, "redstone");
		put(t, Item.SNOWBALL, "snowball");
		put(t, Item.LEATHER, "leather");
		put(t, Item.MILK_BUCKET, "milk_bucket");
		put(t, Item.BRICK, "brick");
		put(t, Item.SUGAR_CANE, "sugar_cane");
		put(t, Item.PAPER, "paper");
		put(t, Item.BOOK, "book");
		put(t, Item.EGG, "egg");
		put(t, Item.COMPASS, "compass");
		put(t, Item.FISHING_ROD, "fishing_rod");
		put(t, Item.CLOCK, "clock");
		put(t, Item.GLOWSTONE_DUST, "glowstone_dust");
		put(t, Item.BONE, "bone");
		put(t, Item.SUGAR, "sugar");
		put(t, Item.CAKE, "cake");
		put(t, Item.REPEATER, "repeater");
		put(t, Item.COOKIE, "cookie");
		put(t, Item.SEEDS, "wheat_seeds", "seeds");
		put(t, Item.CHAIN_HELMET, "chainmail_helmet", "chain_helmet");
		put(t, Item.CHAIN_CHESTPLATE, "chainmail_chestplate", "chain_chestplate");
		put(t, Item.CHAIN_LEGGINGS, "chainmail_leggings", "chain_leggings");
		put(t, Item.CHAIN_BOOTS, "chainmail_boots", "chain_boots");
		put(t, Item.RAW_PORKCHOP, "porkchop", "raw_porkchop");
		put(t, Item.COOKED_PORKCHOP, "cooked_porkchop");
		put(t, Item.SIGN, "oak_sign", "sign");
		put(t, Item.WOODEN_DOOR, "oak_door", "wooden_door");
		put(t, Item.BOAT, "oak_boat", "boat");
		put(t, Item.CLAY, "clay_ball", "clay");
		put(t, Item.SLIMEBALL, "slime_ball", "slimeball");
		put(t, Item.CHEST_MINECART, "chest_minecart", "minecart_chest");
		put(t, Item.FURNACE_MINECART, "furnace_minecart", "minecart_furnace");
		put(t, Item.RAW_FISH, "cod", "fish", "raw_fish");
		put(t, Item.COOKED_FISH, "cooked_cod", "cooked_fish");
		put(t, Item.BED, "red_bed", "bed");
		put(t, Item.RECORD_THIRTEEN, "music_disc_13", "record_13");
		put(t, Item.RECORD_CAT, "music_disc_cat", "record_cat");
		// Dyes carry their subtype in the damage value; modern names map per-meta.
		t.put("ink_sac", Item.DYE.getTextureId(0));
		t.put("red_dye", Item.DYE.getTextureId(1));
		t.put("green_dye", Item.DYE.getTextureId(2));
		t.put("cocoa_beans", Item.DYE.getTextureId(3));
		t.put("lapis_lazuli", Item.DYE.getTextureId(4));
		t.put("purple_dye", Item.DYE.getTextureId(5));
		t.put("cyan_dye", Item.DYE.getTextureId(6));
		t.put("light_gray_dye", Item.DYE.getTextureId(7));
		t.put("gray_dye", Item.DYE.getTextureId(8));
		t.put("pink_dye", Item.DYE.getTextureId(9));
		t.put("lime_dye", Item.DYE.getTextureId(10));
		t.put("yellow_dye", Item.DYE.getTextureId(11));
		t.put("light_blue_dye", Item.DYE.getTextureId(12));
		t.put("magenta_dye", Item.DYE.getTextureId(13));
		t.put("orange_dye", Item.DYE.getTextureId(14));
		t.put("bone_meal", Item.DYE.getTextureId(15));

		sprites = t;
		return t;
	}

	/** Registers an item's sprite under one or more names. */
	private static void put(Map<String, Integer> t, Item item, String... names) {
		for (String name : names) {
			t.put(name, item.getTextureId(0));
		}
	}
}
