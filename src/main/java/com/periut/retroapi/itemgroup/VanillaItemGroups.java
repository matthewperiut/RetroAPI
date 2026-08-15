package com.periut.retroapi.itemgroup;

import com.periut.retroapi.text.Text;
import com.periut.retroapi.register.item.ObtainableItems;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.ornithemc.osl.core.api.util.NamespacedIdentifier;
import net.ornithemc.osl.core.api.util.NamespacedIdentifiers;

/**
 * Beta's own content, sorted into modern Minecraft's creative tabs.
 *
 * <p>The tabs, their order, their icons and the order of items inside them follow modern as closely
 * as beta allows. Where modern has something beta does not - spawn eggs, banners, most of the
 * Functional tab - the entry is simply missing rather than substituted: a gap in a familiar layout
 * is easier to read than a rearrangement of it.
 *
 * <p>Registered lazily on first use so that a mod which registers its own group during init does not
 * have to care whether these exist yet.
 */
public final class VanillaItemGroups {
    private VanillaItemGroups() {
    }

    private static boolean registered;

    public static RetroItemGroup BUILDING_BLOCKS;
    public static RetroItemGroup COLORED_BLOCKS;
    public static RetroItemGroup NATURAL;
    public static RetroItemGroup FUNCTIONAL;
    public static RetroItemGroup REDSTONE;
    public static RetroItemGroup TOOLS;
    public static RetroItemGroup COMBAT;
    public static RetroItemGroup FOOD_AND_DRINKS;
    public static RetroItemGroup INGREDIENTS;
    /** Modern's inventory tab: the player's own armour, storage and hotbar, not a list of items. */
    public static RetroItemGroup INVENTORY;
    /** Modern's Operator Utilities: command blocks, and nothing a survival world can reach. */
    /** Modern's Spawn Eggs tab: the spawner and one egg per mob. See {@code SpawnEggs}. */
    public static RetroItemGroup SPAWN_EGGS;
    public static RetroItemGroup OPERATOR;
    /** Everything beta has, unsorted - modern's search tab, which is also the "did I forget one" tab. */
    public static RetroItemGroup SEARCH;

    private static NamespacedIdentifier id(final String path) {
        return NamespacedIdentifiers.from("minecraft", path);
    }

    /**
     * Registers the vanilla tabs. Called by RetroAPI before any mod's init runs, and again from
     * {@link RetroItemGroups#all()} as a safety net - a mod that reaches one of these fields must
     * never find it null, because a null group makes {@code modifyEntries} a silent no-op.
     */
    /**
     * Blocks that exist only as world states: fluids, fire, the piston's moving parts, the lit
     * variants a block switches to itself, the double slab two slabs become. Modern has no items for
     * any of them either, and a creative tab full of them is how a backport looks unfinished.
     */
    private static boolean isTechnical(final int blockId) {
        return ObtainableItems.isTechnicalBlock(blockId);
    }

    /**
     * Blocks whose held form is a different item - a bed, a door, a sign, a cake, a repeater, sugar
     * cane, redstone. The block goes in the world; the item goes in your hand, and it is the item
     * that belongs in a creative tab.
     */
    private static boolean hasSeparateItem(final int blockId) {
        return ObtainableItems.hasSeparateItem(blockId);
    }

    public static synchronized void ensureRegistered() {
        if (registered) {
            return;
        }
        registered = true;

        BUILDING_BLOCKS = RetroItemGroup.builder(id("building_blocks"))
            .displayName(Text.literal("Building Blocks"))
            .icon(() -> new ItemStack(Block.BRICKS))
            .entries(entries -> {
                entries.add(Block.STONE);
                entries.add(Block.COBBLESTONE);
                entries.add(Block.MOSSY_COBBLESTONE);
                entries.add(Block.COBBLESTONE_STAIRS);
                entries.add(Block.BRICKS);
                entries.add(Block.SANDSTONE);
                // All four slabs beta has: stone, sandstone, wood, cobblestone. The double-slab block
                // is deliberately absent - it is what two of these become, not something you place.
                entries.addRange(Block.SLAB, 0, 3);
                entries.add(Block.SAND);
                entries.add(Block.GRAVEL);
                entries.add(Block.DIRT);
                entries.add(Block.CLAY);
                // Wood: the three species beta has, then everything made of them.
                entries.addRange(Block.LOG, 0, 2);
                entries.add(Block.PLANKS);
                entries.add(Block.WOODEN_STAIRS);
                entries.add(Block.FENCE);
                entries.add(Block.BOOKSHELF);
                entries.add(Block.GLASS);
                entries.add(Block.WOOL);
                entries.add(Block.SNOW_BLOCK);
                entries.add(Block.ICE);
                entries.add(Block.OBSIDIAN);
                entries.add(Block.NETHERRACK);
                entries.add(Block.SOUL_SAND);
                entries.add(Block.GLOWSTONE);
                entries.add(Block.IRON_BLOCK);
                entries.add(Block.GOLD_BLOCK);
                entries.add(Block.DIAMOND_BLOCK);
                entries.add(Block.LAPIS_BLOCK);
                entries.add(Block.BEDROCK);
            })
            .build();

        COLORED_BLOCKS = RetroItemGroup.builder(id("colored_blocks"))
            .displayName(Text.literal("Colored Blocks"))
            .icon(() -> new ItemStack(Block.WOOL.id, 1, 14))
            .entries(entries -> entries.addRange(Block.WOOL, 0, 15))
            .build();

        NATURAL = RetroItemGroup.builder(id("natural"))
            .displayName(Text.literal("Natural Blocks"))
            .icon(() -> new ItemStack(Block.GRASS_BLOCK))
            .entries(entries -> {
                entries.add(Block.GRASS_BLOCK);
                entries.add(Block.DIRT);
                entries.add(Block.SAND);
                entries.add(Block.GRAVEL);
                entries.add(Block.CLAY);
                entries.add(Block.STONE);
                entries.add(Block.COAL_ORE);
                entries.add(Block.IRON_ORE);
                entries.add(Block.GOLD_ORE);
                entries.add(Block.DIAMOND_ORE);
                entries.add(Block.LAPIS_ORE);
                entries.add(Block.REDSTONE_ORE);
                entries.add(Block.OBSIDIAN);
                entries.addRange(Block.LOG, 0, 2);
                entries.addRange(Block.LEAVES, 0, 2);
                entries.addRange(Block.SAPLING, 0, 2);
                entries.add(Block.DEAD_BUSH);
                // Tall grass and fern are metadata on the same block; the dead-shrub state (0) is the
                // one you cannot get, so it is not offered.
                entries.addRange(Block.GRASS, 1, 2);
                entries.add(Block.DANDELION);
                entries.add(Block.ROSE);
                entries.add(Block.BROWN_MUSHROOM);
                entries.add(Block.RED_MUSHROOM);
                entries.add(Block.CACTUS);
                // The item, not the block: placing sugar cane is what the item does, and the block has
                // no item form of its own.
                entries.add(Item.SUGAR_CANE);
                entries.add(Block.PUMPKIN);
                entries.add(Block.SNOW_BLOCK);
                entries.add(Block.ICE);
                entries.add(Block.COBWEB);
                entries.add(Block.SPONGE);
                entries.add(Block.NETHERRACK);
                entries.add(Block.SOUL_SAND);
                entries.add(Block.GLOWSTONE);
            })
            .build();

        FUNCTIONAL = RetroItemGroup.builder(id("functional"))
            .displayName(Text.literal("Functional Blocks"))
            .icon(() -> new ItemStack(Block.TORCH))
            .entries(entries -> {
                entries.add(Block.TORCH);
                entries.add(Block.CRAFTING_TABLE);
                entries.add(Block.FURNACE);
                entries.add(Block.CHEST);
                entries.add(Block.JUKEBOX);
                entries.add(Block.NOTE_BLOCK);
                entries.add(Block.LADDER);
                entries.add(Item.SIGN);
                entries.add(Item.WOODEN_DOOR);
                entries.add(Item.IRON_DOOR);
                entries.add(Block.TRAPDOOR);
                entries.add(Block.FENCE);
                entries.add(Item.BED);
                entries.add(Block.CAKE);
                entries.add(Item.PAINTING);
                entries.add(Block.JACK_O_LANTERN);
                entries.add(Block.TNT);
            })
            .build();

        REDSTONE = RetroItemGroup.builder(id("redstone"))
            .displayName(Text.literal("Redstone Blocks"))
            .icon(() -> new ItemStack(Item.REDSTONE))
            .entries(entries -> {
                entries.add(Item.REDSTONE);
                // LIT_REDSTONE_TORCH is the one with an item form; REDSTONE_TORCH is the off state a
                // placed torch switches to, and is not something you can hold.
                entries.add(Block.LIT_REDSTONE_TORCH);
                entries.add(Item.REPEATER);
                entries.add(Block.LEVER);
                entries.add(Block.BUTTON);
                entries.add(Block.STONE_PRESSURE_PLATE);
                entries.add(Block.WOODEN_PRESSURE_PLATE);
                entries.add(Block.DISPENSER);
                entries.add(Block.NOTE_BLOCK);
                entries.add(Block.PISTON);
                entries.add(Block.STICKY_PISTON);
                entries.add(Block.TNT);
                entries.add(Block.RAIL);
                entries.add(Block.POWERED_RAIL);
                entries.add(Block.DETECTOR_RAIL);
                entries.add(Item.MINECART);
                entries.add(Item.CHEST_MINECART);
                entries.add(Item.FURNACE_MINECART);
            })
            .build();

        TOOLS = RetroItemGroup.builder(id("tools"))
            .displayName(Text.literal("Tools & Utilities"))
            .icon(() -> new ItemStack(Item.IRON_PICKAXE))
            .entries(entries -> {
                entries.add(Item.WOODEN_SHOVEL);
                entries.add(Item.WOODEN_PICKAXE);
                entries.add(Item.WOODEN_AXE);
                entries.add(Item.WOODEN_HOE);
                entries.add(Item.STONE_SHOVEL);
                entries.add(Item.STONE_PICKAXE);
                entries.add(Item.STONE_AXE);
                entries.add(Item.STONE_HOE);
                entries.add(Item.IRON_SHOVEL);
                entries.add(Item.IRON_PICKAXE);
                entries.add(Item.IRON_AXE);
                entries.add(Item.IRON_HOE);
                entries.add(Item.GOLDEN_SHOVEL);
                entries.add(Item.GOLDEN_PICKAXE);
                entries.add(Item.GOLDEN_AXE);
                entries.add(Item.GOLDEN_HOE);
                entries.add(Item.DIAMOND_SHOVEL);
                entries.add(Item.DIAMOND_PICKAXE);
                entries.add(Item.DIAMOND_AXE);
                entries.add(Item.DIAMOND_HOE);
                entries.add(Item.FLINT_AND_STEEL);
                entries.add(Item.BUCKET);
                entries.add(Item.WATER_BUCKET);
                entries.add(Item.LAVA_BUCKET);
                entries.add(Item.MILK_BUCKET);
                entries.add(Item.FISHING_ROD);
                entries.add(Item.COMPASS);
                entries.add(Item.CLOCK);
                entries.add(Item.BOAT);
                entries.add(Item.SADDLE);
                entries.add(Item.RECORD_THIRTEEN);
                entries.add(Item.RECORD_CAT);
            })
            .build();

        COMBAT = RetroItemGroup.builder(id("combat"))
            .displayName(Text.literal("Combat"))
            .icon(() -> new ItemStack(Item.IRON_SWORD))
            .entries(entries -> {
                entries.add(Item.WOODEN_SWORD);
                entries.add(Item.STONE_SWORD);
                entries.add(Item.IRON_SWORD);
                entries.add(Item.GOLDEN_SWORD);
                entries.add(Item.DIAMOND_SWORD);
                entries.add(Item.BOW);
                entries.add(Item.ARROW);
                entries.add(Item.LEATHER_HELMET);
                entries.add(Item.LEATHER_CHESTPLATE);
                entries.add(Item.LEATHER_LEGGINGS);
                entries.add(Item.LEATHER_BOOTS);
                entries.add(Item.CHAIN_HELMET);
                entries.add(Item.CHAIN_CHESTPLATE);
                entries.add(Item.CHAIN_LEGGINGS);
                entries.add(Item.CHAIN_BOOTS);
                entries.add(Item.IRON_HELMET);
                entries.add(Item.IRON_CHESTPLATE);
                entries.add(Item.IRON_LEGGINGS);
                entries.add(Item.IRON_BOOTS);
                entries.add(Item.GOLDEN_HELMET);
                entries.add(Item.GOLDEN_CHESTPLATE);
                entries.add(Item.GOLDEN_LEGGINGS);
                entries.add(Item.GOLDEN_BOOTS);
                entries.add(Item.DIAMOND_HELMET);
                entries.add(Item.DIAMOND_CHESTPLATE);
                entries.add(Item.DIAMOND_LEGGINGS);
                entries.add(Item.DIAMOND_BOOTS);
            })
            .build();

        FOOD_AND_DRINKS = RetroItemGroup.builder(id("food_and_drinks"))
            .displayName(Text.literal("Food & Drinks"))
            .icon(() -> new ItemStack(Item.GOLDEN_APPLE))
            .entries(entries -> {
                entries.add(Item.APPLE);
                entries.add(Item.GOLDEN_APPLE);
                entries.add(Item.BREAD);
                entries.add(Item.COOKIE);
                entries.add(Item.CAKE);
                entries.add(Item.MUSHROOM_STEW);
                entries.add(Item.RAW_PORKCHOP);
                entries.add(Item.COOKED_PORKCHOP);
                entries.add(Item.RAW_FISH);
                entries.add(Item.COOKED_FISH);
                entries.add(Item.MILK_BUCKET);
            })
            .build();

        INGREDIENTS = RetroItemGroup.builder(id("ingredients"))
            .displayName(Text.literal("Ingredients"))
            .icon(() -> new ItemStack(Item.IRON_INGOT))
            .entries(entries -> {
                entries.add(Item.COAL);
                entries.add(Item.IRON_INGOT);
                entries.add(Item.GOLD_INGOT);
                entries.add(Item.DIAMOND);
                entries.add(Item.STICK);
                entries.add(Item.BOWL);
                entries.add(Item.STRING);
                entries.add(Item.FEATHER);
                entries.add(Item.FLINT);
                entries.add(Item.LEATHER);
                entries.add(Item.BRICK);
                entries.add(Item.CLAY);
                entries.add(Item.PAPER);
                entries.add(Item.BOOK);
                entries.add(Item.SLIMEBALL);
                entries.add(Item.SNOWBALL);
                entries.add(Item.EGG);
                entries.add(Item.SEEDS);
                entries.add(Item.WHEAT);
                entries.add(Item.SUGAR);
                entries.add(Item.SUGAR_CANE);
                entries.add(Item.BONE);
                entries.add(Item.GUNPOWDER);
                entries.add(Item.GLOWSTONE_DUST);
                entries.add(Item.REDSTONE);
                // Dye, all sixteen, the way modern lists them together.
                entries.addRange(Item.DYE, 0, 15);
            })
            .build();

        // Modern's tenth paginated tab, bottom row column 4, and the last one on the first page.
        SPAWN_EGGS = RetroItemGroup.builder(id("spawn_eggs"))
            .displayName(Text.literal("Spawn Eggs"))
            .icon(() -> {
                final Item creeperEgg = com.periut.retroapi.entity.spawnegg.SpawnEggs.icon();
                return creeperEgg == null ? new ItemStack(Block.SPAWNER) : new ItemStack(creeperEgg);
            })
            .entries(entries -> {
                // Modern's own contents: the spawner first, then an egg per mob. Registered late
                // (RetroAPI's init), so this reads them when the tab is opened, never at build time.
                entries.add(Block.SPAWNER);
                for (final Item egg : com.periut.retroapi.entity.spawnegg.SpawnEggs.all()) {
                    entries.add(egg);
                }
            })
            .build();

        OPERATOR = RetroItemGroup.builder(id("op_blocks"))
            .displayName(Text.literal("Operator Utilities"))
            .icon(() -> new ItemStack(com.periut.retroapi.commandblock.CommandBlocks.IMPULSE == null
                ? Block.DISPENSER : com.periut.retroapi.commandblock.CommandBlocks.IMPULSE))
            .entries(entries -> {
                // Registered late (RetroAPI's own init) so guard the nulls: a tab that throws would
                // take the whole screen with it.
                entries.add(com.periut.retroapi.commandblock.CommandBlocks.IMPULSE);
                entries.add(com.periut.retroapi.commandblock.CommandBlocks.CHAIN);
                entries.add(com.periut.retroapi.commandblock.CommandBlocks.REPEATING);
                // The spawner is modern's, but it lives on the Spawn Eggs tab there, not this one.
                entries.add(Block.BEDROCK);
            })
            .build();

        // Contents come from the player, not from a list: the screen swaps the container's slots for
        // the player's own when this tab is picked, which is what modern does.
        INVENTORY = RetroItemGroup.builder(id("inventory"))
            .displayName(Text.literal("Inventory"))
            .icon(() -> new ItemStack(Block.CHEST))
            .entries(entries -> {
            })
            .build();

        SEARCH = RetroItemGroup.builder(id("search"))
            .displayName(Text.literal("Search Items"))
            .icon(() -> new ItemStack(Item.COMPASS))
            .entries(entries -> {
                // Built from the registries rather than by hand, because this tab exists to be
                // complete - a mod's content included - and a hand-written list would drift. What it
                // does NOT list is the blocks you cannot hold: see TECHNICAL_BLOCKS.
                final java.util.Set<Integer> listed = new java.util.HashSet<>();
                for (final Block block : Block.BLOCKS) {
                    if (block == null) {
                        continue;
                    }
                    // Decided here whether it is listed or deliberately left out - either way the item
                    // pass below must not offer it a second time.
                    listed.add(block.id);
                    if (isTechnical(block.id) || hasSeparateItem(block.id)) {
                        continue;
                    }
                    if (block.id < Item.ITEMS.length && Item.ITEMS[block.id] != null) {
                        entries.add(block);
                    }
                }

                // Then everything the block pass did not already cover.
                //
                // Which is NOT "every item whose id is past the end of the block array": RetroAPI
                // grows Block.BLOCKS to make room for modded blocks, so that boundary moves out past
                // every vanilla item id and this loop quietly stopped listing apples, buckets, tools -
                // everything that is not a block. Tracking what the first pass added says exactly the
                // same thing and cannot drift when the array grows again.
                for (final Item item : Item.ITEMS) {
                    if (item != null && !listed.contains(item.id)) {
                        entries.add(item);
                    }
                }
            })
            .build();
    }
}
