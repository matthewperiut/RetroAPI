package com.periut.retrotweaks.mixin.recipe;

import com.periut.retrotweaks.config.Config;

import net.minecraft.block.Block;
import net.minecraft.block.entity.FurnaceBlockEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes the wooden items vanilla forgot burn as furnace fuel.
 *
 * <p>Vanilla burns any block whose material is wood, but items made of wood - tools, bowls, boats,
 * signs, doors, ladders - are not blocks and burn for nothing. Burn times match modern Minecraft.
 * From AnnoyanceFix and UniTweaks, which shipped the same table.
 */
@Mixin(FurnaceBlockEntity.class)
public class FurnaceFuelMixin {

	@Inject(method = "getFuelTime", at = @At("HEAD"), cancellable = true)
	private void retrotweaks$extraWoodFuels(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
		if (!Config.RECIPES.enableRecipes || !Config.RECIPES.furnaceFuels || stack == null) return;

		int burnTime = retrotweaks$fuelTime(stack);
		if (burnTime > 0) cir.setReturnValue(burnTime);
	}

	private static int retrotweaks$fuelTime(ItemStack stack) {
		int id = stack.itemId;

		if (id == Item.BOAT.id) return 1200;
		if (id == Item.BOW.id || id == Item.FISHING_ROD.id || id == Block.LADDER.id) return 300;
		if (id == Item.WOODEN_AXE.id || id == Item.WOODEN_HOE.id || id == Item.WOODEN_PICKAXE.id
			|| id == Item.WOODEN_SHOVEL.id || id == Item.WOODEN_SWORD.id
			|| id == Item.SIGN.id || id == Item.WOODEN_DOOR.id) return 200;
		// Only the wooden slab (metadata 2) burns; the stone ones are not fuel.
		if (id == Block.SLAB.id && stack.getDamage() == 2) return 150;
		if (id == Item.BOWL.id) return 100;
		return 0;
	}
}
