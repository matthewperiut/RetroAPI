package com.periut.retrotweaks.mixin.block;

import com.periut.retrotweaks.feature.block.ToolEffectivity;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolItem;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Applies {@link ToolEffectivity} to the tool's mining speed. */
@Mixin(ToolItem.class)
public class ToolItemMixin {

	@Shadow
	private float miningSpeed;

	@Inject(method = "getMiningSpeedMultiplier", at = @At("HEAD"), cancellable = true)
	private void retrotweaks$extraEffectiveBlocks(ItemStack stack, Block block, CallbackInfoReturnable<Float> cir) {
		if (ToolEffectivity.isExtraEffective((Item) (Object) this, block)) {
			cir.setReturnValue(this.miningSpeed);
		}
	}
}
