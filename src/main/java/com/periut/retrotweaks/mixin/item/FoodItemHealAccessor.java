package com.periut.retrotweaks.mixin.item;

import net.minecraft.item.FoodItem;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** How much a food heals, which vanilla keeps private and never shows the player. */
@Mixin(FoodItem.class)
public interface FoodItemHealAccessor {

	@Accessor("healthRestored")
	int retrotweaks$getHealthRestored();
}
