package com.periut.retroapi.mixin.client.render;

import com.periut.retroapi.client.texture.LayeredItemDraw;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.Item;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Feeds the current layer's tint into the vanilla draw. Beta already multiplies every item
 * sprite by {@code Item.getColorMultiplier} (it is how leather and potions are dyed), so
 * while a {@link LayeredItemDraw layered} pass is active we just return that layer's tint
 * here and the existing renderer applies it. Outside a layered pass this does nothing.
 */
@Mixin(Item.class)
@Environment(EnvType.CLIENT)
public class ItemColorMixin {

	@Inject(method = "getColorMultiplier(I)I", at = @At("HEAD"), cancellable = true)
	private void retroapi$layerTint(int meta, CallbackInfoReturnable<Integer> cir) {
		if (LayeredItemDraw.active()) {
			cir.setReturnValue(LayeredItemDraw.forcedTint);
		}
	}
}
