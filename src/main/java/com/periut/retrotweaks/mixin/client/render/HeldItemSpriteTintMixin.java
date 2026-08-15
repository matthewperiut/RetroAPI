package com.periut.retrotweaks.mixin.client.render;

import com.periut.retrotweaks.feature.render.ItemTint;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tints the item held in your hand, for the blocks vanilla forgets - tall grass and ferns.
 *
 * <p>{@code renderItem} splits on {@code BlockRenderManager.isSideLit}: a full cube is handed to the
 * block renderer, which multiplies by {@code Block.getColor}; everything else is extruded from a flat
 * sprite right here, and that branch never sets a colour at all. It simply inherits whatever is
 * current - which is why setting one at HEAD and putting it back at RETURN is enough, with no need to
 * find the individual quad draws (there are six of them).
 *
 * <p>Harmless on the cube branch: {@code BlockRenderManager.render} sets its own colour before drawing
 * anything, so a value left here is overwritten rather than applied twice.
 */
@Environment(EnvType.CLIENT)
@Mixin(HeldItemRenderer.class)
public abstract class HeldItemSpriteTintMixin {

	@Inject(method = "renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;)V", at = @At("HEAD"))
	private void retrotweaks$tintHeldSprite(LivingEntity holder, ItemStack stack, CallbackInfo ci) {
		int color = ItemTint.of(stack);
		if (color == ItemTint.NONE) return;
		GL11.glColor4f(ItemTint.red(color), ItemTint.green(color), ItemTint.blue(color), 1.0F);
	}

	@Inject(method = "renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;)V", at = @At("RETURN"))
	private void retrotweaks$untintHeldSprite(LivingEntity holder, ItemStack stack, CallbackInfo ci) {
		if (ItemTint.of(stack) == ItemTint.NONE) return;
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
	}
}
