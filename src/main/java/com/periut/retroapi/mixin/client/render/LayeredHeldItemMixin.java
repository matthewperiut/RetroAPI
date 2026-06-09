package com.periut.retroapi.mixin.client.render;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.periut.retroapi.client.texture.LayeredItemDraw;
import com.periut.retroapi.component.RetroTextureLayer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;

/**
 * The third draw site for {@link com.periut.retroapi.component.RetroLayeredTexture}: the item
 * held in hand. Same idea as the ItemRenderer sites, re-run the held-item draw once per layer
 * with that layer's sprite and tint, polygon-offset so the extruded layers stack cleanly.
 */
@Mixin(HeldItemRenderer.class)
@Environment(EnvType.CLIENT)
public class LayeredHeldItemMixin {

	@WrapMethod(method = "renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;)V")
	private void retroapi$layeredHeld(LivingEntity entity, ItemStack stack, Operation<Void> original) {
		List<RetroTextureLayer> layers = LayeredItemDraw.layersOf(stack);
		if (layers == null) {
			original.call(entity, stack);
			return;
		}
		try {
			for (int i = 0; i < layers.size(); i++) {
				RetroTextureLayer layer = layers.get(i);
				LayeredItemDraw.begin(layer);
				// The held render never calls getColorMultiplier, so we tint the layer with
				// glColor directly (it leaves glColor alone, so this sticks).
				int t = layer.tint();
				GL11.glColor4f(((t >> 16) & 0xFF) / 255.0f, ((t >> 8) & 0xFF) / 255.0f, (t & 0xFF) / 255.0f, 1.0f);
				// Separate overlay layers from the base WITHOUT a slope factor: a slope-scaled
				// offset shoves the extruded edge quads apart and leaves gaps between pixel
				// rows. Units-only bias (and none at all for the base) keeps each layer solid.
				if (i > 0) {
					GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
					GL11.glPolygonOffset(0.0f, -3.0f * i);
				}
				original.call(entity, stack);
				if (i > 0) {
					GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
				}
			}
		} finally {
			GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
			GL11.glPolygonOffset(0.0f, 0.0f);
			GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
			LayeredItemDraw.end();
		}
	}
}
