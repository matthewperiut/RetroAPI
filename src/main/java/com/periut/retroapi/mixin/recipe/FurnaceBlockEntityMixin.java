package com.periut.retroapi.mixin.recipe;

import com.periut.retroapi.register.recipe.RetroRecipes;
import net.minecraft.block.entity.FurnaceBlockEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Wires {@link RetroRecipes#addFuel(int, int)} into the furnace so registered fuels burn.
 * Without this the FUEL_MAP would be dead code. Disabled when StationAPI is present
 * (StationAPI owns {@code FurnaceBlockEntity.getFuelTime}); see RetroAPIMixinPlugin.
 *
 * <p>Out of scope: smelting-output overstack (count &gt; 1) - Aether smelts nothing.</p>
 */
@Mixin(FurnaceBlockEntity.class)
public class FurnaceBlockEntityMixin {
	@Inject(method = "getFuelTime(Lnet/minecraft/item/ItemStack;)I", at = @At("HEAD"), cancellable = true)
	private void retroapi$customFuelTime(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
		if (stack == null) return;
		int time = RetroRecipes.getFuelTime(stack.getItem().id);
		if (time > 0) {
			cir.setReturnValue(time); // else fall through to vanilla fuel logic
		}
	}
}
