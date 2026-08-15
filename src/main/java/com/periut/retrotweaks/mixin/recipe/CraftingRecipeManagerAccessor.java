package com.periut.retrotweaks.mixin.recipe;

import net.minecraft.item.ItemStack;
import net.minecraft.recipe.CraftingRecipeManager;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Exposes vanilla's package-private recipe builders.
 *
 * <p>They parse the familiar {@code "XXX", 'X', Block.STONE} pattern form and apply vanilla's own
 * ingredient rules (a bare {@code Block} matches any metadata, a bare {@code Item} matches metadata
 * 0), so using them keeps RetroTweaks' recipes behaving exactly like the ones next to them instead of
 * subtly differently.
 */
@Mixin(CraftingRecipeManager.class)
public interface CraftingRecipeManagerAccessor {

	@Invoker("addShapedRecipe")
	void retrotweaks$addShaped(ItemStack output, Object... input);

	@Invoker("addShapelessRecipe")
	void retrotweaks$addShapeless(ItemStack output, Object... input);
}
