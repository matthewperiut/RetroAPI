package com.periut.retroapi.mixin.recipe;

import net.minecraft.item.ItemStack;
import net.minecraft.recipe.ShapedRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Reaches a vanilla shaped recipe's stacks so {@link com.periut.retroapi.registry.IdRemap} can repoint
 * modded ingredients/results at their post-remap ids (a mod may build vanilla {@code ShapedRecipe}s
 * directly instead of going through {@code RetroRecipes}).
 */
@Mixin(ShapedRecipe.class)
public interface ShapedRecipeAccessor {

	@Accessor("input")
	ItemStack[] retroapi$getInput();

	@Accessor("output")
	ItemStack retroapi$getOutput();

	@Accessor("outputId")
	void retroapi$setOutputId(int id);
}
