package com.periut.retroapi.mixin.client.render;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.periut.retroapi.client.texture.LayeredItemDraw;
import com.periut.retroapi.component.RetroTextureLayer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;

/**
 * Draws a {@link com.periut.retroapi.component.RetroLayeredTexture} item as its stack of
 * tinted layers in the two ItemRenderer sites: the GUI/hotbar/inventory icon and the dropped
 * item in the world. Each wrapper asks the stack for its layers and, if it has any, re-runs
 * the original draw once per layer with that layer's sprite and tint forced (see
 * {@link LayeredItemDraw}). A non-layered item runs the original exactly once, untouched.
 *
 * <p>The world draw is 3D, so each pass gets a polygon-offset nudge to keep the extruded
 * layers from z-fighting; the flat GUI draw needs none (the layers blend back to front).</p>
 */
@Mixin(ItemRenderer.class)
@Environment(EnvType.CLIENT)
public class LayeredItemRenderMixin {

	@WrapMethod(method = "renderGuiItem(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/client/texture/TextureManager;Lnet/minecraft/item/ItemStack;II)V")
	private void retroapi$layeredGui(TextRenderer font, TextureManager textures, ItemStack stack,
			int x, int y, Operation<Void> original) {
		List<RetroTextureLayer> layers = LayeredItemDraw.layersOf(stack);
		if (layers == null) {
			original.call(font, textures, stack, x, y);
			return;
		}
		try {
			for (RetroTextureLayer layer : layers) {
				LayeredItemDraw.begin(layer);
				original.call(font, textures, stack, x, y);
			}
		} finally {
			LayeredItemDraw.end();
		}
	}

	@WrapMethod(method = "render(Lnet/minecraft/entity/ItemEntity;DDDFF)V")
	private void retroapi$layeredDropped(ItemEntity entity, double x, double y, double z,
			float yaw, float tickDelta, Operation<Void> original) {
		List<RetroTextureLayer> layers = LayeredItemDraw.layersOf(entity.stack);
		if (layers == null) {
			original.call(entity, x, y, z, yaw, tickDelta);
			return;
		}
		try {
			for (int i = 0; i < layers.size(); i++) {
				LayeredItemDraw.begin(layers.get(i));
				// Base draws untouched; overlays get a units-only depth bias (no slope factor,
				// which would split the extruded edge quads, see LayeredHeldItemMixin).
				if (i > 0) {
					GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
					GL11.glPolygonOffset(0.0f, -3.0f * i);
				}
				original.call(entity, x, y, z, yaw, tickDelta);
				if (i > 0) {
					GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
				}
			}
		} finally {
			GL11.glPolygonOffset(0.0f, 0.0f);
			GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
			LayeredItemDraw.end();
		}
	}
}
