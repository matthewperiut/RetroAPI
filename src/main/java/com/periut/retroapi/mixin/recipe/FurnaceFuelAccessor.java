package com.periut.retroapi.mixin.recipe;

import net.minecraft.block.entity.FurnaceBlockEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Exposes vanilla's private {@code FurnaceBlockEntity.getFuelTime(ItemStack)} so
 * {@link com.periut.retroapi.register.recipe.RetroRecipes#getTotalFuelTime(ItemStack)} can answer
 * "how long does this item burn?" for ANY fuel, vanilla's table plus whatever has been injected
 * into it ({@code FurnaceBlockEntityMixin} on the RetroAPI path, StationAPI's own mixin when it is
 * present). Always applied (it adds no behavior, unlike {@code FurnaceBlockEntityMixin}).
 */
@Mixin(FurnaceBlockEntity.class)
public interface FurnaceFuelAccessor {

	@Invoker("getFuelTime")
	int retroapi$getFuelTime(ItemStack stack);
}
