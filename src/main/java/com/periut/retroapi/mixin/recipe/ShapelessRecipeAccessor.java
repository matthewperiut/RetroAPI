package com.periut.retroapi.mixin.recipe;

import net.minecraft.item.ItemStack;
import net.minecraft.recipe.ShapelessRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

/** Vanilla shapeless-recipe stacks, for {@link com.periut.retroapi.registry.IdRemap}. See {@link ShapedRecipeAccessor}. */
@Mixin(ShapelessRecipe.class)
public interface ShapelessRecipeAccessor {

	@Accessor("input")
	List<ItemStack> retroapi$getInput();

	@Accessor("output")
	ItemStack retroapi$getOutput();
}
