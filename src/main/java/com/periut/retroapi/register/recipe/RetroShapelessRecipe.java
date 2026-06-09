package com.periut.retroapi.register.recipe;

import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.CraftingRecipe;

import java.util.BitSet;

/**
 * A shapeless crafting recipe with metadata-aware ingredient matching.
 *
 * <p>Each ingredient is an {@link ItemStack}; an ingredient whose damage value is {@code -1}
 * is a <b>WILDCARD</b> matching any metadata, otherwise it matches <b>exactly</b>. Each
 * grid stack must consume exactly one (distinct) ingredient, tracked via a {@link BitSet},
 * mirroring StationAPI's {@code StationShapelessRecipe}.</p>
 */
public final class RetroShapelessRecipe implements CraftingRecipe {
	private final ItemStack[] ingredients;
	public final ItemStack output;
	private final BitSet matchedIngredients;

	public RetroShapelessRecipe(ItemStack output, ItemStack[] ingredients) {
		this.ingredients = ingredients;
		this.output = output;
		this.matchedIngredients = new BitSet(ingredients.length);
	}

	@Override
	public boolean matches(CraftingInventory inv) {
		this.matchedIngredients.clear();
		for (int y = 0; y < 3; ++y) {
			for (int x = 0; x < 3; ++x) {
				ItemStack itemToTest = inv.getStack(x, y);
				if (itemToTest == null) continue;
				boolean noMatch = true;
				for (int i = 0; i < this.ingredients.length; i++) {
					if (this.matchedIngredients.get(i)) continue;
					ItemStack ingredient = this.ingredients[i];
					if (ingredient == null) continue;
					// WILDCARD: damage == -1 matches any metadata (see RetroShapedRecipe).
					boolean ignoreDamage = ingredient.getDamage() == -1;
					if (ignoreDamage) ingredient.setDamage(itemToTest.getDamage());
					boolean equals = ingredient.isItemEqual(itemToTest);
					if (ignoreDamage) ingredient.setDamage(-1);
					if (equals) {
						this.matchedIngredients.set(i);
						noMatch = false;
						break;
					}
				}
				if (noMatch) return false;
			}
		}
		return this.matchedIngredients.nextClearBit(0) >= this.ingredients.length;
	}

	@Override
	public ItemStack craft(CraftingInventory inv) {
		return this.output.copy();
	}

	@Override
	public int getSize() {
		return this.ingredients.length;
	}

	@Override
	public ItemStack getOutput() {
		return this.output.copy();
	}
}
