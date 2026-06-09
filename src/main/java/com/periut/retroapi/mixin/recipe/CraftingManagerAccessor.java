package com.periut.retroapi.mixin.recipe;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.CraftingRecipeManager;

@Mixin(CraftingRecipeManager.class)
public interface CraftingManagerAccessor {
	@Accessor("recipes")
	List<CraftingRecipe> retroapi$getRecipes();
}

