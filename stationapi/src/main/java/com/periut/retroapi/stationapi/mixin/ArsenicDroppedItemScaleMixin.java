package com.periut.retroapi.stationapi.mixin;

import com.periut.retroapi.client.render.DroppedItemScale;
import com.periut.retroapi.register.block.RetroBlockAccess;

import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.modificationstation.stationapi.api.client.texture.SpriteAtlasTexture;
import net.modificationstation.stationapi.impl.client.arsenic.renderer.render.ArsenicItemRenderer;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * {@link RetroBlockAccess#droppedItemScale(float)} under StationAPI.
 *
 * <p>RetroAPI's own hook is a {@code @ModifyConstant} on {@code ItemRenderer.render}, and arsenic
 * {@code @Overwrite}s that method to delegate to its own renderer, so the constant is not there to modify:
 * with the priority raised far enough for the injection to even be legal against a merged method, both the
 * scale constant and the {@code glScalef} call it guards report {@code Scanned 0 target(s)}. That is why
 * the native hook is disabled under StationAPI rather than left to fail at class load.
 *
 * <p>Arsenic did not change the rule though, it moved it. {@code ArsenicItemRenderer.renderVanilla} carries
 * beta's decision verbatim:
 *
 * <pre>
 * float scale = 0.25F;
 * if (!block.isFullCube() &amp;&amp; block.id != Block.SLAB.id &amp;&amp; block.getRenderType() != 16) scale = 0.5F;
 * GL11.glScalef(scale, scale, scale);
 * </pre>
 *
 * So the same correction applies, one class along. The first {@code glScalef} in that method is the
 * three-dimensional block draw; the later one belongs to the flat sprite branch, which has no such rule and
 * is left alone.
 *
 * <p>The block comes from the stack rather than from a captured local, because a local's index is a detail
 * of how arsenic happens to compile and the stack is the method's own argument.
 */
@Mixin(ArsenicItemRenderer.class)
public class ArsenicDroppedItemScaleMixin {

	@Redirect(method = "renderVanilla",
		at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glScalef(FFF)V", ordinal = 0))
	private void retroapi$droppedItemScale(float x, float y, float z,
			ItemEntity entity, float tickDelta, float rotation, float bob, float brightness,
			ItemStack stack, float red, float green, byte count, SpriteAtlasTexture atlas) {
		// The same answer the non-StationAPI path gives, from the same place: a block's declared size
		// first, then the "Dropped Item Size" tweak, then whatever arsenic was about to use. Asking
		// DroppedItemScale rather than re-deriving it here is what stops the two renderers drifting -
		// and the tweak's own mixin cannot reach this install at all (arsenic @Overwrites the method it
		// targets), so before this it simply did not apply under StationAPI.
		float scale = DroppedItemScale.override(stack);
		if (scale > 0.0F) {
			GL11.glScalef(scale, scale, scale);
			return;
		}
		GL11.glScalef(x, y, z);
	}
}
