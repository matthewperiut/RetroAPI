package com.periut.retroapi.register.recipe;

import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.CraftingRecipe;

/**
 * A shaped crafting recipe with metadata-aware ingredient matching.
 *
 * <p>Each cell of {@link #grid} is either {@code null} (empty) or an {@link ItemStack}
 * ingredient. An ingredient {@link ItemStack} whose damage value is {@code -1} is a
 * <b>WILDCARD</b>: it matches the corresponding grid stack regardless of metadata/damage.
 * Any other damage value matches <b>exactly</b>. This mirrors StationAPI's
 * {@code StationShapedRecipe} (the {@code ignoreDamage} dance) without the tag system.</p>
 */
public final class RetroShapedRecipe implements CraftingRecipe {
	public final int width;
	public final int height;
	// entries may be null; an ItemStack with damage == -1 is a wildcard (matches any metadata)
	final ItemStack[] grid;
	public final ItemStack output;

	public RetroShapedRecipe(int width, int height, ItemStack[] grid, ItemStack output) {
		this.width = width;
		this.height = height;
		this.grid = grid;
		this.output = output;
	}

	@Override
	public boolean matches(CraftingInventory inv) {
		for (int x = 0; x <= 3 - this.width; ++x) {
			for (int y = 0; y <= 3 - this.height; ++y) {
				if (this.matches(inv, x, y, true)) return true;
				if (this.matches(inv, x, y, false)) return true;
			}
		}
		return false;
	}

	private boolean matches(CraftingInventory inv, int startX, int startY, boolean mirror) {
		for (int x = 0; x < 3; ++x) {
			for (int y = 0; y < 3; ++y) {
				int dx = x - startX;
				int dy = y - startY;
				ItemStack ingredient = null;
				if (dx >= 0 && dy >= 0 && dx < this.width && dy < this.height) {
					ingredient = this.grid[(mirror ? this.width - dx - 1 : dx) + dy * this.width];
				}
				ItemStack itemToTest = inv.getStack(x, y);
				if (itemToTest != null || ingredient != null) {
					if (itemToTest == null || ingredient == null) return false;
					// WILDCARD: damage == -1 means "match any metadata". Temporarily borrow the
					// tested stack's damage so isItemEqual passes, then restore. Crafting is
					// single-threaded in b1.7.3, so this transient mutation is safe.
					boolean ignoreDamage = ingredient.getDamage() == -1;
					if (ignoreDamage) ingredient.setDamage(itemToTest.getDamage());
					boolean equals = ingredient.isItemEqual(itemToTest);
					if (ignoreDamage) ingredient.setDamage(-1);
					if (!equals) return false;
				}
			}
		}
		return true;
	}

	@Override
	public ItemStack craft(CraftingInventory inv) {
		return this.output.copy();
	}

	@Override
	public int getSize() {
		return this.width * this.height;
	}

	@Override
	public ItemStack getOutput() {
		return this.output.copy();
	}
}
