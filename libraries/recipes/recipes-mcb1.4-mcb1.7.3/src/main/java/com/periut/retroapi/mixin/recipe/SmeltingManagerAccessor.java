package com.periut.retroapi.mixin.recipe;

import net.minecraft.crafting.SmeltingManager;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(SmeltingManager.class)
public interface SmeltingManagerAccessor {
	@Accessor("recipes")
	Map<Integer, ItemStack> retroapi$getRecipes();
}
