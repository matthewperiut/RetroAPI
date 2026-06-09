package com.periut.retroapi.register.recipe;

import net.minecraft.item.ItemStack;

/**
 * Thin facade mirroring StationAPI's {@code CraftingRegistry} method names so consumers
 * (e.g. a migrated {@code AetherRecipes}) need only an import swap to move from StationAPI
 * to RetroAPI. Ingredient semantics: a bare {@code Block}/{@code Item} is wildcard
 * (any-metadata); a full {@code ItemStack} matches exactly (see {@link RetroRecipes}).
 */
public final class CraftingRegistry {

	private CraftingRegistry() {}

	public static void addShapedRecipe(ItemStack output, Object... recipe) {
		RetroRecipes.addShaped(output, recipe);
	}

	public static void addShapelessRecipe(ItemStack output, Object... ingredients) {
		RetroRecipes.addShapeless(output, ingredients);
	}
}
