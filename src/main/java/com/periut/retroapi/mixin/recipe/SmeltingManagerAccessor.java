package com.periut.retroapi.mixin.recipe;

import net.minecraft.item.ItemStack;
import net.minecraft.recipe.SmeltingRecipeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(SmeltingRecipeManager.class)
public interface SmeltingManagerAccessor {
	@Accessor("recipes")
	Map<Integer, ItemStack> retroapi$getRecipes();
}

