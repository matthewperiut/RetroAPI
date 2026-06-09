package com.periut.retroapi.register.recipe;

import com.periut.retroapi.compat.StationBridges;
import com.periut.retroapi.mixin.recipe.CraftingManagerAccessor;
import com.periut.retroapi.mixin.recipe.FurnaceFuelAccessor;
import com.periut.retroapi.mixin.recipe.SmeltingManagerAccessor;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.block.entity.FurnaceBlockEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.CraftingRecipeManager;
import net.minecraft.recipe.ShapedRecipe;
import net.minecraft.recipe.ShapelessRecipe;
import net.minecraft.recipe.SmeltingRecipeManager;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Public crafting/smelting/fuel registration API.
 *
 * <h2>Wildcard (any-metadata) policy</h2>
 * A bare {@link Block} or {@link Item} ingredient is treated as a <b>WILDCARD</b>
 * (damage {@code -1}): it matches an input stack of any metadata/damage. To match a
 * <b>specific</b> metadata, pass a full {@link ItemStack} (e.g.
 * {@code new ItemStack(Block.WOOL, 1, 14)}), which matches exactly. This deliberately
 * standardizes on {@code -1} for both shaped and shapeless (StationAPI inconsistently
 * uses {@code 0} for shapeless bare ingredients); it matches modder intent (most bare
 * ingredients want "any metadata") and is safe because metadata-specific recipes always
 * pass full {@code ItemStack}s.
 */
public final class RetroRecipes {

	private static final Map<Integer, Integer> FUEL_MAP = new HashMap<>();

	private RetroRecipes() {}

	private static boolean hasStationAPI() {
		return FabricLoader.getInstance().isModLoaded("stationapi");
	}

	/**
	 * Resolves an ingredient object to an {@link ItemStack}.
	 * <ul>
	 *   <li>{@link ItemStack} -> used as-is (exact metadata, a copy so callers' stacks aren't mutated)</li>
	 *   <li>bare {@link Item} -> wildcard damage {@code -1} (matches any metadata)</li>
	 *   <li>bare {@link Block} -> wildcard damage {@code -1} (matches any metadata)</li>
	 * </ul>
	 */
	private static ItemStack resolveIngredient(Object o) {
		if (o instanceof ItemStack stack) return stack.copy();      // exact metadata
		if (o instanceof Item item)       return new ItemStack(item, 1, -1);  // WILDCARD damage
		if (o instanceof Block block)     return new ItemStack(block, 1, -1); // WILDCARD damage
		throw new IllegalArgumentException("Invalid ingredient type: " + (o == null ? "null" : o.getClass()));
	}

	/**
	 * Adds a shaped crafting recipe.
	 *
	 * @param output  the result item stack
	 * @param recipe  alternating pattern strings and char-to-ingredient mappings.
	 *                Ingredients can be {@link ItemStack} (exact metadata), {@link Item}, or {@link Block} (wildcard).
	 */
	public static void addShaped(ItemStack output, Object... recipe) {
		if (hasStationAPI()) {
			// Delegate to StationAPI's CraftingRegistry so RetroAPI and StationAPI never
			// double-register or use conflicting recipe classes.
			StationBridges.get().addShapedRecipe(output, recipe);
			return;
		}

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
			charMap.put(c, resolveIngredient(recipe[idx]));
			idx++;
		}

		// Build ItemStack ingredient grid (null = empty cell)
		ItemStack[] ingredients = new ItemStack[width * height];
		for (int i = 0; i < pattern.length(); i++) {
			char c = pattern.charAt(i);
			ingredients[i] = (c == ' ') ? null : charMap.get(c);
		}

		getCraftingRecipes().add(new RetroShapedRecipe(width, height, ingredients, output));
	}

	/**
	 * Adds a shapeless crafting recipe.
	 *
	 * @param output      the result item stack
	 * @param ingredients the ingredients ({@link ItemStack} exact, or {@link Item}/{@link Block} wildcard)
	 */
	public static void addShapeless(ItemStack output, Object... ingredients) {
		if (hasStationAPI()) {
			StationBridges.get().addShapelessRecipe(output, ingredients);
			return;
		}

		ItemStack[] stacks = new ItemStack[ingredients.length];
		for (int i = 0; i < ingredients.length; i++) {
			stacks[i] = resolveIngredient(ingredients[i]);
		}

		getCraftingRecipes().add(new RetroShapelessRecipe(output, stacks));
	}

	/**
	 * Re-sorts the crafting recipe list so shaped recipes take precedence over shapeless
	 * recipes (and larger recipes over smaller), treating RetroAPI's own recipe classes
	 * like their vanilla counterparts.
	 *
	 * <p>Vanilla sorts its recipe list at {@code CraftingRecipeManager.<init>} via an inner
	 * comparator that does {@code instanceof ShapedRecipe}/{@code ShapelessRecipe} checks.
	 * Because {@link RetroShapedRecipe}/{@link RetroShapelessRecipe} are neither, freshly
	 * appended RetroAPI recipes would be mis-ordered. Rather than mixin the brittle
	 * mapping-hash synthetic comparator class (and depend on mixinextras), RetroAPI
	 * re-sorts the list here, after registration, with a RetroAPI-aware comparator that
	 * reproduces vanilla's ordering.</p>
	 */
	public static void sortCraftingRecipes() {
		if (hasStationAPI()) return; // StationAPI owns sorting when present
		List<CraftingRecipe> recipes = getCraftingRecipes();
		recipes.sort(RETRO_RECIPE_COMPARATOR);
	}

	/**
	 * Reproduces vanilla {@code CraftingRecipeManager}'s inner comparator: shaped-like
	 * recipes sort before shapeless-like recipes; otherwise larger size sorts first.
	 * Treats {@link RetroShapedRecipe} like {@link ShapedRecipe} and
	 * {@link RetroShapelessRecipe} like {@link ShapelessRecipe}.
	 */
	private static final Comparator<CraftingRecipe> RETRO_RECIPE_COMPARATOR = (a, b) -> {
		boolean aShapeless = isShapeless(a);
		boolean bShapeless = isShapeless(b);
		boolean aShaped = isShaped(a);
		boolean bShaped = isShaped(b);
		if (aShapeless && bShaped) return 1;
		if (bShapeless && aShaped) return -1;
		int sizeA = a.getSize();
		int sizeB = b.getSize();
		if (sizeB < sizeA) return -1;
		if (sizeB > sizeA) return 1;
		return 0;
	};

	private static boolean isShaped(CraftingRecipe r) {
		return r instanceof ShapedRecipe || r instanceof RetroShapedRecipe;
	}

	private static boolean isShapeless(CraftingRecipe r) {
		return r instanceof ShapelessRecipe || r instanceof RetroShapelessRecipe;
	}

	/**
	 * Removes every crafting recipe whose output is item-equal (same id + metadata) to the
	 * given stack. Optional parity helper; no-op under StationAPI (StationAPI exposes no
	 * removal API beyond direct list access). Returns the number of recipes removed.
	 */
	public static int removeRecipe(ItemStack output) {
		if (hasStationAPI()) return 0;
		List<CraftingRecipe> recipes = getCraftingRecipes();
		int before = recipes.size();
		recipes.removeIf(r -> {
			ItemStack result = r.getOutput();
			return result != null && result.isItemEqual(output);
		});
		return before - recipes.size();
	}

	@SuppressWarnings("unchecked")
	private static List<CraftingRecipe> getCraftingRecipes() {
		// Vanilla CraftingRecipeManager.getRecipes() is public, but the accessor keeps the
		// generic type tight and avoids depending on the vanilla method's raw signature.
		CraftingRecipeManager manager = CraftingRecipeManager.getInstance();
		return ((CraftingManagerAccessor) manager).retroapi$getRecipes();
	}

	/**
	 * Adds a smelting recipe.
	 *
	 * @param inputId the raw block/item ID of the input
	 * @param output  the result item stack
	 */
	public static void addSmelting(int inputId, ItemStack output) {
		if (hasStationAPI()) {
			StationBridges.get().addSmeltingRecipe(inputId, output);
			return;
		}
		SmeltingRecipeManager manager = SmeltingRecipeManager.getInstance();
		((SmeltingManagerAccessor) manager).retroapi$getRecipes().put(inputId, output);
	}

	/**
	 * Registers a fuel item with a burn time in ticks (200 ticks = 1 item smelted).
	 * Wired into the furnace via {@code FurnaceBlockEntityMixin} (non-StationAPI path).
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

	/** Probe instance used only to consult the furnace's (possibly mixin-extended) fuel table. */
	private static FurnaceBlockEntity fuelProbe;

	/**
	 * Gets the burn time in ticks for a stack from the FULL fuel table, vanilla's fuels plus
	 * anything injected into the furnace ({@link #addFuel} on the RetroAPI path, StationAPI's
	 * registrations when it is present). Returns 0 if the stack is not fuel.
	 *
	 * <p>Use this for custom machines that should "burn whatever a furnace burns"
	 * (e.g. a freezer/grinder with a fuel slot), instead of mixin-ing the furnace yourself.</p>
	 */
	public static int getTotalFuelTime(ItemStack stack) {
		if (stack == null) {
			return 0;
		}
		if (fuelProbe == null) {
			fuelProbe = new FurnaceBlockEntity();
		}
		int vanilla = ((FurnaceFuelAccessor) fuelProbe).retroapi$getFuelTime(stack);
		// Belt-and-braces: under StationAPI our FurnaceBlockEntityMixin is disabled, so FUEL_MAP
		// entries aren't in the furnace's table, still honor them here.
		return Math.max(vanilla, getFuelTime(stack.getItem().id));
	}
}
