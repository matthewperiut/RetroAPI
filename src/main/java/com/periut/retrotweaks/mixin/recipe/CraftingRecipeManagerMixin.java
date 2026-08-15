package com.periut.retrotweaks.mixin.recipe;

import com.periut.retrotweaks.config.Config;
import com.periut.retrotweaks.feature.recipe.RecipeTweaks;

import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.CraftingRecipeManager;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CraftingRecipeManager.class)
public class CraftingRecipeManagerMixin {

	@Inject(method = "<init>", at = @At("TAIL"))
	private void retrotweaks$applyRecipeTweaks(CallbackInfo ci) {
		RecipeTweaks.install((CraftingRecipeManager) (Object) this);
	}

	/**
	 * Combining two damaged tools, weapons or armor pieces of the same kind repairs them, as in modern
	 * Minecraft.
	 *
	 * <p>This runs before the recipe list is consulted because it is not a recipe: it matches any
	 * pair of damaged items of one type, which vanilla's fixed ingredient lists cannot express.
	 * Originally from UniTweaks by way of AnnoyanceFix.
	 *
	 * <p>Armor and weapons/tools are two separate config switches rather than one, so the flag is
	 * picked from the class of the pair actually in the grid - {@code ArmorItem} means armor, anything
	 * else damageable means a weapon or tool. A player who wants only one of the two gets only that one
	 * instead of having to take both.
	 */
	@Inject(method = "craft", at = @At("HEAD"), cancellable = true)
	private void retrotweaks$repairInCraftingGrid(CraftingInventory inventory, CallbackInfoReturnable<ItemStack> cir) {
		if (!Config.RECIPES.enableRecipes) return;

		ItemStack first = null;
		ItemStack second = null;
		for (int slot = 0; slot < inventory.size(); slot++) {
			ItemStack stack = inventory.getStack(slot);
			if (stack == null || !stack.getItem().isDamageable()) continue;
			if (first != null && first.getItem() == stack.getItem()) {
				second = stack;
			} else {
				first = stack;
			}
		}
		if (first == null || second == null) return;

		Item item = first.getItem();
		boolean armor = item instanceof ArmorItem;
		if (!(armor ? Config.RECIPES.armorRepair : Config.RECIPES.toolRepair)) return;
		int remainingA = item.getMaxDamage() - first.getDamage();
		int remainingB = item.getMaxDamage() - second.getDamage();
		// The 10% bonus matches modern Minecraft's repair bonus.
		int repaired = remainingA + remainingB + item.getMaxDamage() * 10 / 100;
		cir.setReturnValue(new ItemStack(item, 1, Math.max(0, item.getMaxDamage() - repaired)));
	}
}
