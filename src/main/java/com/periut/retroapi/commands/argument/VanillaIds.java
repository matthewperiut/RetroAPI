package com.periut.retroapi.commands.argument;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The mod's own {@code minecraft:} registry: every vanilla block and item, by name.
 *
 * <p>Beta identifies items by number and has no registry to ask, so this table is what makes
 * {@code /give Player minecraft:stone} work with nothing else installed. StationAPI and RetroAPI
 * supplement it when present (see {@link ItemIds}) but are never required for the vanilla set.
 *
 * <p>Names follow modern Minecraft wherever beta's item is recognisably the same thing, and the
 * {@link #ALIASES} table carries the rest: the older names beta players know, and the modern names
 * for subtypes beta stores as metadata, so {@code minecraft:red_wool} resolves to wool with damage
 * 14 exactly as {@code minecraft:wool:14} does.
 *
 * <p>Where a block and an item share a name - {@code wheat}, {@code bed}, {@code cake},
 * {@code repeater} - the item wins, because that is the id that produces a usable stack.
 */
public final class VanillaIds {
    public static final String NAMESPACE = "minecraft";

    /** An item id together with the damage value that selects a subtype of it. */
    public record VanillaItem(int id, int meta) {
    }

    private static final Map<String, VanillaItem> CANONICAL = new LinkedHashMap<>();
    private static final Map<String, VanillaItem> ALIASES = new LinkedHashMap<>();
    private static final Map<Integer, String> BY_ID = new LinkedHashMap<>();

    private VanillaIds() {
    }

    static {
        // Blocks, 1-96.
        block("stone", 1);
        block("grass_block", 2);
        block("dirt", 3);
        block("cobblestone", 4);
        block("oak_planks", 5);
        block("oak_sapling", 6);
        block("bedrock", 7);
        block("flowing_water", 8);
        block("water", 9);
        block("flowing_lava", 10);
        block("lava", 11);
        block("sand", 12);
        block("gravel", 13);
        block("gold_ore", 14);
        block("iron_ore", 15);
        block("coal_ore", 16);
        block("oak_log", 17);
        block("oak_leaves", 18);
        block("sponge", 19);
        block("glass", 20);
        block("lapis_ore", 21);
        block("lapis_block", 22);
        block("dispenser", 23);
        block("sandstone", 24);
        block("note_block", 25);
        block("bed_block", 26);
        block("powered_rail", 27);
        block("detector_rail", 28);
        block("sticky_piston", 29);
        block("cobweb", 30);
        block("grass", 31);
        block("dead_bush", 32);
        block("piston", 33);
        block("piston_head", 34);
        block("wool", 35);
        block("moving_piston", 36);
        block("dandelion", 37);
        block("poppy", 38);
        block("brown_mushroom", 39);
        block("red_mushroom", 40);
        block("gold_block", 41);
        block("iron_block", 42);
        block("double_stone_slab", 43);
        block("stone_slab", 44);
        block("bricks", 45);
        block("tnt", 46);
        block("bookshelf", 47);
        block("mossy_cobblestone", 48);
        block("obsidian", 49);
        block("torch", 50);
        block("fire", 51);
        block("spawner", 52);
        block("oak_stairs", 53);
        block("chest", 54);
        block("redstone_wire", 55);
        block("diamond_ore", 56);
        block("diamond_block", 57);
        block("crafting_table", 58);
        block("wheat_crop", 59);
        block("farmland", 60);
        block("furnace", 61);
        block("lit_furnace", 62);
        block("sign_post", 63);
        block("oak_door_block", 64);
        block("ladder", 65);
        block("rail", 66);
        block("cobblestone_stairs", 67);
        block("wall_sign", 68);
        block("lever", 69);
        block("stone_pressure_plate", 70);
        block("iron_door_block", 71);
        block("oak_pressure_plate", 72);
        block("redstone_ore", 73);
        block("lit_redstone_ore", 74);
        block("redstone_torch_off", 75);
        block("redstone_torch", 76);
        block("stone_button", 77);
        block("snow", 78);
        block("ice", 79);
        block("snow_block", 80);
        block("cactus", 81);
        block("clay", 82);
        block("sugar_cane_block", 83);
        block("jukebox", 84);
        block("oak_fence", 85);
        block("pumpkin", 86);
        block("netherrack", 87);
        block("soul_sand", 88);
        block("glowstone", 89);
        block("nether_portal", 90);
        block("jack_o_lantern", 91);
        block("cake_block", 92);
        block("repeater_off", 93);
        block("repeater_on", 94);
        block("locked_chest", 95);
        block("trapdoor", 96);

        // Items, 256 and up.
        item("iron_shovel", 256);
        item("iron_pickaxe", 257);
        item("iron_axe", 258);
        item("flint_and_steel", 259);
        item("apple", 260);
        item("bow", 261);
        item("arrow", 262);
        item("coal", 263);
        item("diamond", 264);
        item("iron_ingot", 265);
        item("gold_ingot", 266);
        item("iron_sword", 267);
        item("wooden_sword", 268);
        item("wooden_shovel", 269);
        item("wooden_pickaxe", 270);
        item("wooden_axe", 271);
        item("stone_sword", 272);
        item("stone_shovel", 273);
        item("stone_pickaxe", 274);
        item("stone_axe", 275);
        item("diamond_sword", 276);
        item("diamond_shovel", 277);
        item("diamond_pickaxe", 278);
        item("diamond_axe", 279);
        item("stick", 280);
        item("bowl", 281);
        item("mushroom_stew", 282);
        item("golden_sword", 283);
        item("golden_shovel", 284);
        item("golden_pickaxe", 285);
        item("golden_axe", 286);
        item("string", 287);
        item("feather", 288);
        item("gunpowder", 289);
        item("wooden_hoe", 290);
        item("stone_hoe", 291);
        item("iron_hoe", 292);
        item("diamond_hoe", 293);
        item("golden_hoe", 294);
        item("wheat_seeds", 295);
        item("wheat", 296);
        item("bread", 297);
        item("leather_helmet", 298);
        item("leather_chestplate", 299);
        item("leather_leggings", 300);
        item("leather_boots", 301);
        item("chainmail_helmet", 302);
        item("chainmail_chestplate", 303);
        item("chainmail_leggings", 304);
        item("chainmail_boots", 305);
        item("iron_helmet", 306);
        item("iron_chestplate", 307);
        item("iron_leggings", 308);
        item("iron_boots", 309);
        item("diamond_helmet", 310);
        item("diamond_chestplate", 311);
        item("diamond_leggings", 312);
        item("diamond_boots", 313);
        item("golden_helmet", 314);
        item("golden_chestplate", 315);
        item("golden_leggings", 316);
        item("golden_boots", 317);
        item("flint", 318);
        item("porkchop", 319);
        item("cooked_porkchop", 320);
        item("painting", 321);
        item("golden_apple", 322);
        item("sign", 323);
        item("oak_door", 324);
        item("bucket", 325);
        item("water_bucket", 326);
        item("lava_bucket", 327);
        item("minecart", 328);
        item("saddle", 329);
        item("iron_door", 330);
        item("redstone", 331);
        item("snowball", 332);
        item("oak_boat", 333);
        item("leather", 334);
        item("milk_bucket", 335);
        item("brick", 336);
        item("clay_ball", 337);
        item("sugar_cane", 338);
        item("paper", 339);
        item("book", 340);
        item("slime_ball", 341);
        item("chest_minecart", 342);
        item("furnace_minecart", 343);
        item("egg", 344);
        item("compass", 345);
        item("fishing_rod", 346);
        item("clock", 347);
        item("glowstone_dust", 348);
        item("cod", 349);
        item("cooked_cod", 350);
        item("dye", 351);
        item("bone", 352);
        item("sugar", 353);
        item("cake", 354);
        item("bed", 355);
        item("repeater", 356);
        item("cookie", 357);
        item("map", 358);
        item("shears", 359);
        item("music_disc_13", 2256);
        item("music_disc_cat", 2257);

        registerAliases();
    }

    /**
     * Older beta names, plain synonyms, and modern names for the subtypes beta keeps as metadata.
     * Aliases resolve but are not suggested, so the completion list stays one entry per thing.
     */
    private static void registerAliases() {
        alias("planks", 5, 0);
        alias("sapling", 6, 0);
        alias("log", 17, 0);
        alias("wood", 17, 0);
        alias("leaves", 18, 0);
        alias("rose", 38, 0);
        alias("slab", 44, 0);
        alias("double_slab", 43, 0);
        alias("wooden_stairs", 53, 0);
        alias("button", 77, 0);
        alias("wooden_pressure_plate", 72, 0);
        alias("wooden_door", 324, 0);
        alias("fence", 85, 0);
        alias("boat", 333, 0);
        alias("raw_porkchop", 319, 0);
        alias("raw_fish", 349, 0);
        alias("cooked_fish", 350, 0);
        alias("slimeball", 341, 0);
        alias("seeds", 295, 0);
        alias("chain_helmet", 302, 0);
        alias("chain_chestplate", 303, 0);
        alias("chain_leggings", 304, 0);
        alias("chain_boots", 305, 0);
        alias("record_13", 2256, 0);
        alias("record_cat", 2257, 0);
        alias("mob_spawner", 52, 0);
        alias("monster_spawner", 52, 0);
        alias("web", 30, 0);
        alias("tall_grass", 31, 1);

        // Wool: beta stores the colour as damage, modern gives each its own id.
        final String[] colors = {"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
            "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"};
        for (int meta = 0; meta < colors.length; meta++) {
            alias(colors[meta] + "_wool", 35, meta);
        }

        // Dye, in beta's order, named the way modern names each dye.
        final String[] dyes = {"ink_sac", "red_dye", "green_dye", "cocoa_beans", "lapis_lazuli", "purple_dye",
            "cyan_dye", "light_gray_dye", "gray_dye", "pink_dye", "lime_dye", "yellow_dye", "light_blue_dye",
            "magenta_dye", "orange_dye", "bone_meal"};
        for (int meta = 0; meta < dyes.length; meta++) {
            alias(dyes[meta], 351, meta);
        }

        alias("spruce_log", 17, 1);
        alias("birch_log", 17, 2);
        alias("spruce_leaves", 18, 1);
        alias("birch_leaves", 18, 2);
        alias("sandstone_slab", 44, 1);
        alias("oak_slab", 44, 2);
        alias("cobblestone_slab", 44, 3);
    }

    private static void block(final String name, final int id) {
        CANONICAL.put(name, new VanillaItem(id, 0));
        BY_ID.putIfAbsent(id, name);
    }

    /** Items are registered after blocks and deliberately overwrite a shared name. */
    private static void item(final String name, final int id) {
        CANONICAL.put(name, new VanillaItem(id, 0));
        BY_ID.put(id, name);
    }

    private static void alias(final String name, final int id, final int meta) {
        ALIASES.put(name, new VanillaItem(id, meta));
    }

    /** Resolves a path with no namespace, or with the {@code minecraft} namespace. Null if unknown. */
    public static VanillaItem byName(final String path) {
        final String key = path.toLowerCase(Locale.ROOT);
        final VanillaItem canonical = CANONICAL.get(key);
        return canonical != null ? canonical : ALIASES.get(key);
    }

    public static String nameOf(final int id) {
        return BY_ID.get(id);
    }

    /** Canonical names only - the list the completion window shows. */
    public static List<String> names() {
        return Collections.unmodifiableList(new ArrayList<>(CANONICAL.keySet()));
    }

    /**
     * The alias that names exactly this item and subtype, or null.
     *
     * <p>This is how a metadata variant gets a real name: beta's lang file has one entry covering
     * all sixteen wools, but {@code red_wool} is registered here against wool with damage 14.
     */
    public static String aliasFor(final int id, final int meta) {
        for (final Map.Entry<String, VanillaItem> alias : ALIASES.entrySet()) {
            final VanillaItem value = alias.getValue();
            if (value.id() == id && value.meta() == meta) {
                return alias.getKey();
            }
        }
        return null;
    }

    public static Map<String, VanillaItem> aliases() {
        return Collections.unmodifiableMap(ALIASES);
    }
}
