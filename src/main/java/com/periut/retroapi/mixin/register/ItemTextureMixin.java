package com.periut.retroapi.mixin.register;

import com.periut.retroapi.client.texture.LayeredItemDraw;
import com.periut.retroapi.component.RetroDynamicTexture;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lets a {@link RetroDynamicTexture} item pick its rendered sprite from the stack's
 * components. {@code ItemStack.getTextureId()} feeds every place an item is drawn, so
 * overriding it here makes the component-driven texture appear in the hotbar, inventory,
 * hand, and on the ground alike, with no per-render-path hooks.
 *
 * <p>It also serves the LAYERED path: while the render mixins loop a layered item one layer
 * at a time, {@link LayeredItemDraw#forcedSprite} is the sprite for the current pass, and it
 * wins over everything else so that one pass draws that one layer.</p>
 */
@Mixin(Item.class)
public class ItemTextureMixin {

	@Inject(method = "getTextureId(Lnet/minecraft/item/ItemStack;)I", at = @At("HEAD"), cancellable = true)
	private void retroapi$dynamicTexture(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
		if (LayeredItemDraw.active()) {
			cir.setReturnValue(LayeredItemDraw.forcedSprite);
		} else if ((Object) this instanceof RetroDynamicTexture) {
			cir.setReturnValue(((RetroDynamicTexture) (Object) this).getDynamicTextureId(stack));
		}
	}
}
