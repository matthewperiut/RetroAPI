package com.periut.retroapi.mixin.component;

import com.llamalad7.mixinextras.sugar.Local;
import com.periut.retroapi.component.RetroComponentHolder;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.HashMap;

/**
 * Carries data components through item PICKUP. When the inventory places a picked-up
 * stack into an empty slot, {@code combineStacks} builds a brand-new stack with
 * {@code new ItemStack(id, count, damage)}, which (like every raw id/count/damage
 * construction in beta) drops the components. This redirects that one construction to
 * also copy the components off the picked-up source stack, so a component-bearing item
 * keeps its data when you walk over it.
 */
@Mixin(PlayerInventory.class)
public class PlayerInventoryComponentMixin {

	@Redirect(
		method = "combineStacks",
		at = @At(value = "NEW", target = "(III)Lnet/minecraft/item/ItemStack;"))
	private ItemStack retroapi$pickUpWithComponents(int id, int count, int damage,
			@Local(argsOnly = true) ItemStack source) {
		ItemStack placed = new ItemStack(id, count, damage);
		RetroComponentHolder src = (RetroComponentHolder) (Object) source;
		if (!src.retroapi$components().isEmpty()) {
			((RetroComponentHolder) (Object) placed)
				.retroapi$setComponents(new HashMap<>(src.retroapi$components()));
		}
		return placed;
	}
}
