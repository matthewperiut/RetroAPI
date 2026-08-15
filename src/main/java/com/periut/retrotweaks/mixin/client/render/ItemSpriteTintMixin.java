package com.periut.retrotweaks.mixin.client.render;

import com.periut.retrotweaks.feature.render.ItemTint;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.texture.TextureManager;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tints the flat inventory sprite of a block that has a colour, which vanilla only ever does for the
 * three-dimensional ones.
 *
 * <p>{@code renderGuiItem} has two branches. A block whose render type is "side lit" (a full cube) is
 * handed to {@code BlockRenderManager.render}, which multiplies it by {@link Block#getColor(int)}.
 * Everything else - crossed sprites like tall grass and ferns among them - takes the other branch,
 * which draws a flat texture and only ever sets a colour for {@code useCustomDisplayColor}, the flag
 * behind dyed leather armour. Nothing in that branch has ever asked the block what colour it is, so a
 * greyscale sprite stays grey no matter what {@code getColor} says.
 *
 * <p>So this asks, immediately before the draw, and puts the colour back to white immediately after so
 * the next icon in the inventory is unaffected. Blocks that answer white - which is every block except
 * leaves and, with {@code TallPlantColorMixin} applied, tall grass and ferns - are skipped entirely
 * rather than being multiplied by a no-op.
 */
@Environment(EnvType.CLIENT)
@Mixin(ItemRenderer.class)
public abstract class ItemSpriteTintMixin {

	// Spelled out at each use rather than shared in a constant: a static final field on a mixin is a
	// field Mixin would try to merge into ItemRenderer itself, for no benefit once javac has inlined it.

	@Inject(method = "renderGuiItem(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/client/texture/TextureManager;IIIII)V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/item/ItemRenderer;drawTexture(IIIIII)V"))
	private void retrotweaks$tintSprite(TextRenderer textRenderer, TextureManager textureManager,
			int id, int meta, int texture, int x, int y, CallbackInfo ci) {
		int color = ItemTint.of(id, meta);
		if (color == ItemTint.NONE) return;
		GL11.glColor4f(ItemTint.red(color), ItemTint.green(color), ItemTint.blue(color), 1.0F);
	}

	@Inject(method = "renderGuiItem(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/client/texture/TextureManager;IIIII)V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/item/ItemRenderer;drawTexture(IIIIII)V",
			shift = At.Shift.AFTER))
	private void retrotweaks$untintSprite(TextRenderer textRenderer, TextureManager textureManager,
			int id, int meta, int texture, int x, int y, CallbackInfo ci) {
		if (ItemTint.of(id, meta) == ItemTint.NONE) return;
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
	}

}
