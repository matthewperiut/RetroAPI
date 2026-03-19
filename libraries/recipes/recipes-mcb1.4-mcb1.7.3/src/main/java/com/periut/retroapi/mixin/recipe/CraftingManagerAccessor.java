package com.periut.retroapi.mixin.recipe;

import net.minecraft.crafting.CraftingManager;
import net.minecraft.crafting.recipe.Recipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(CraftingManager.class)
public interface CraftingManagerAccessor {
	@Accessor("recipes")
	List<Recipe> retroapi$getRecipes();
}
