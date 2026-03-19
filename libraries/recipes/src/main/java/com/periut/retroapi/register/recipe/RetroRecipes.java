package com.periut.retroapi.register.recipe;

import com.periut.retroapi.mixin.recipe.CraftingManagerAccessor;
#if MC_VER >= 120
import com.periut.retroapi.mixin.recipe.SmeltingManagerAccessor;
#endif
import net.minecraft.block.Block;
import net.minecraft.crafting.CraftingManager;
#if MC_VER >= 120
import net.minecraft.crafting.SmeltingManager;
import net.minecraft.crafting.recipe.ShapedRecipe;
import net.minecraft.crafting.recipe.ShapelessRecipe;
#else
import net.minecraft.crafting.Recipe;
#endif
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class RetroRecipes {

	private static final Map<Integer, Integer> FUEL_MAP = new HashMap<>();

	private RetroRecipes() {}

	/**
	 * Adds a shaped crafting recipe.
	 *
	 * @param output  the result item stack
	 * @param recipe  alternating pattern strings and char-to-ingredient mappings.
	 *                Ingredients can be {@link ItemStack}, {@link Item}, or {@link Block}.
	 */
	public static void addShaped(ItemStack output, Object... recipe) {
		// Parse pattern strings
		String pattern = "";
		int idx = 0;
		int width = 0;
		int height = 0;

		while (idx < recipe.length && recipe[idx] instanceof String row) {
			pattern += row;
			width = row.length();
			height++;
			idx++;
		}

		// Parse character-to-ingredient mappings
		Map<Character, ItemStack> charMap = new HashMap<>();
		while (idx < recipe.length) {
			char c = (Character) recipe[idx];
			idx++;
			ItemStack ingredient;
			if (recipe[idx] instanceof ItemStack is) {
				ingredient = is;
			} else if (recipe[idx] instanceof Item item) {
				ingredient = new ItemStack(item);
			} else if (recipe[idx] instanceof Block block) {
				ingredient = new ItemStack(block);
			} else {
				throw new IllegalArgumentException("Invalid ingredient type: " + recipe[idx].getClass());
			}
			charMap.put(c, ingredient);
			idx++;
		}

#if MC_VER >= 120
		// Build ItemStack ingredient array
		ItemStack[] ingredients = new ItemStack[width * height];
		for (int i = 0; i < pattern.length(); i++) {
			char c = pattern.charAt(i);
			if (c == ' ') {
				ingredients[i] = null;
			} else {
				ingredients[i] = charMap.get(c);
			}
		}

		CraftingManager manager = CraftingManager.getInstance();
		((CraftingManagerAccessor) manager).retroapi$getRecipes().add(
			new ShapedRecipe(width, height, ingredients, output)
		);
#else
		// Build int[] ingredient array (item IDs)
		int[] ingredients = new int[width * height];
		for (int i = 0; i < pattern.length(); i++) {
			char c = pattern.charAt(i);
			if (c == ' ') {
				ingredients[i] = -1;
			} else {
				ItemStack stack = charMap.get(c);
				ingredients[i] = stack != null ? stack.id : -1;
			}
		}

		CraftingManager manager = CraftingManager.getInstance();
		((CraftingManagerAccessor) manager).retroapi$getRecipes().add(
			new Recipe(width, height, ingredients, output)
		);
#endif
	}

	/**
	 * Adds a shapeless crafting recipe.
	 *
	 * @param output      the result item stack
	 * @param ingredients the ingredients (ItemStack, Item, or Block)
	 */
	public static void addShapeless(ItemStack output, Object... ingredients) {
#if MC_VER >= 120
		List<ItemStack> stacks = new ArrayList<>();
		for (Object obj : ingredients) {
			if (obj instanceof ItemStack is) {
				stacks.add(is);
			} else if (obj instanceof Item item) {
				stacks.add(new ItemStack(item));
			} else if (obj instanceof Block block) {
				stacks.add(new ItemStack(block));
			} else {
				throw new IllegalArgumentException("Invalid ingredient type: " + obj.getClass());
			}
		}

		CraftingManager manager = CraftingManager.getInstance();
		((CraftingManagerAccessor) manager).retroapi$getRecipes().add(
			new ShapelessRecipe(output, stacks)
		);
#else
		// No shapeless recipe support pre-b1.2; register as 1-wide shaped recipe
		int[] ids = new int[ingredients.length];
		for (int i = 0; i < ingredients.length; i++) {
			Object obj = ingredients[i];
			if (obj instanceof ItemStack is) {
				ids[i] = is.id;
			} else if (obj instanceof Item item) {
				ids[i] = item.id;
			} else if (obj instanceof Block block) {
				ids[i] = block.id;
			} else {
				throw new IllegalArgumentException("Invalid ingredient type: " + obj.getClass());
			}
		}

		CraftingManager manager = CraftingManager.getInstance();
		((CraftingManagerAccessor) manager).retroapi$getRecipes().add(
			new Recipe(ingredients.length, 1, ids, output)
		);
#endif
	}

	/**
	 * Adds a smelting recipe.
	 *
	 * @param inputId the raw block/item ID of the input
	 * @param output  the result item stack
	 */
	public static void addSmelting(int inputId, ItemStack output) {
#if MC_VER >= 120
		SmeltingManager manager = SmeltingManager.getInstance();
		((SmeltingManagerAccessor) manager).retroapi$getRecipes().put(inputId, output);
#endif
	}

	/**
	 * Registers a fuel item with a burn time in ticks (200 ticks = 1 item smelted).
	 *
	 * @param itemId   the item ID
	 * @param burnTime burn time in ticks
	 */
	public static void addFuel(int itemId, int burnTime) {
		FUEL_MAP.put(itemId, burnTime);
	}

	/**
	 * Gets the fuel burn time for an item, or 0 if not a registered fuel.
	 */
	public static int getFuelTime(int itemId) {
		return FUEL_MAP.getOrDefault(itemId, 0);
	}

	/**
	 * Returns true if the given item ID has a registered fuel time.
	 */
	public static boolean isFuel(int itemId) {
		return FUEL_MAP.containsKey(itemId);
	}
}
