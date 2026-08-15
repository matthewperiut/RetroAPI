package com.periut.retrotweaks.mixin.recipe;

import net.minecraft.block.entity.FurnaceBlockEntity;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Exposes vanilla's private {@code FurnaceBlockEntity.getFuelTime}, so "is this fuel?" can be asked
 * of the real table - vanilla's entries plus whatever {@link FurnaceFuelMixin} adds to it.
 */
@Mixin(FurnaceBlockEntity.class)
public interface FurnaceFuelAccessor {

	@Invoker("getFuelTime")
	int retrotweaks$getFuelTime(ItemStack stack);
}
