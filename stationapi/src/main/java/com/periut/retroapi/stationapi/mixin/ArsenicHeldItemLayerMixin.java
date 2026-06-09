package com.periut.retroapi.stationapi.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.periut.retroapi.client.texture.LayeredItemDraw;
import com.periut.retroapi.component.RetroTextureLayer;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.modificationstation.stationapi.impl.client.arsenic.renderer.render.ArsenicOverlayRenderer;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;

/**
 * The held-item layered draw under StationAPI. StationAPI's arsenic renderer @Overwrites the vanilla
 * {@code HeldItemRenderer}, so RetroAPI's native {@code mixin.client.render.LayeredHeldItemMixin} (which
 * wraps the vanilla {@code renderItem(LivingEntity, ItemStack)}) is disabled under StationAPI and replaced
 * by this one - gated by {@code RetroAPIMixinPlugin}'s {@code STATIONAPI_ONLY_MIXINS} (references
 * StationAPI's {@code ArsenicOverlayRenderer}, so it must stay off the load path otherwise).
 *
 * <p>Why wrap {@code ArsenicOverlayRenderer.renderItem3D} rather than the vanilla method: arsenic routes
 * <b>third-person</b> held items through the vanilla {@code HeldItemRenderer.renderItem(LivingEntity,
 * ItemStack)} (which it @Overwrites to call {@code renderItem3D}), BUT <b>first-person</b> goes through
 * {@code render(float) -> renderVanilla(f, ...)} which calls {@code renderItem3D} <i>directly</i>, bypassing
 * the vanilla method entirely. So wrapping the vanilla method only caught third-person (which worked) and
 * missed first-person (which was broken). {@code renderItem3D} is the single shared 3D draw both paths funnel
 * into, so wrapping it once here covers BOTH with no double-loop.</p>
 *
 * <p>Per layer: force the sprite (read back by {@code ItemTextureMixin} via {@code ItemStack.getSprite} ->
 * {@code Item.getSprite(ItemStack)}, the same hook the GUI/ground paths use, which arsenic's
 * {@code renderItem3D -> renderVanilla} honors through {@code getItemStackTextureId}), tint via glColor
 * (the 3D held draw never calls {@code getColorMultiplier}), and polygon-offset overlays off the base so the
 * extruded layers don't z-fight. Mirrors the native {@code LayeredHeldItemMixin} exactly.</p>
 */
@Mixin(ArsenicOverlayRenderer.class)
public class ArsenicHeldItemLayerMixin {

	@WrapMethod(method = "renderItem3D(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;)V")
	private void retroapi$layeredHeld3D(LivingEntity entity, ItemStack item, Operation<Void> original) {
		List<RetroTextureLayer> layers = LayeredItemDraw.layersOf(item);
		if (layers == null) {
			original.call(entity, item);
			return;
		}
		try {
			for (int i = 0; i < layers.size(); i++) {
				RetroTextureLayer layer = layers.get(i);
				LayeredItemDraw.begin(layer);
				int t = layer.tint();
				GL11.glColor4f(((t >> 16) & 0xFF) / 255.0f, ((t >> 8) & 0xFF) / 255.0f, (t & 0xFF) / 255.0f, 1.0f);
				if (i > 0) {
					GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
					GL11.glPolygonOffset(0.0f, -3.0f * i);
				}
				original.call(entity, item);
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
