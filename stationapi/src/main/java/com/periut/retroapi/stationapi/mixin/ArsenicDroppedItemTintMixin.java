package com.periut.retroapi.stationapi.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import com.periut.retrotweaks.feature.render.ItemTint;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.modificationstation.stationapi.impl.client.arsenic.renderer.render.ArsenicItemRenderer;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The tweaks half's dropped/pickup sprite tint, re-applied inside arsenic's replacement renderer.
 *
 * <p>{@code mixin.client.render.DroppedItemTintMixin} cannot run under StationAPI - arsenic
 * {@code @Overwrite}s the method it targets - so short grass and ferns lost their green for the half
 * second they were being collected. Arsenic draws a dropped item one of two ways: an item with a
 * working JSON model goes through the baked-model path, where the item-colour registry answers (see
 * {@code compat.stationapi.StationApiItemColors}); anything else falls back to {@code renderVanilla},
 * whose flat-sprite branch is a port of beta's. There the colour sits behind the same
 * {@code useCustomDisplayColor} flag (the dyed-leather gate, always false for the entity renderer) and
 * even opened computes {@code Item.getColorMultiplier}, which {@code BlockItem} never overrides - so a
 * tall-grass stack answers white and the sprite draws in whatever {@code glColor} is current, which
 * during a pickup is the flat grey the particle set from the block's luminance.
 *
 * <p>Three injections, matching the non-StationAPI fix exactly: open the flag for a stack
 * {@link ItemTint} has a colour for, feed that colour through the {@code getColorMultiplier} call, and
 * put white back at the end so the rest of the pass does not inherit the tint.
 *
 * <h2>Why it lives here and not beside its twin</h2>
 *
 * <p>It used to, targeting arsenic by string with {@code remap = false} so the tweaks half would not
 * need StationAPI on its compile classpath - which forced the two Minecraft members named above to be
 * written as raw intermediary by hand, and a hand-written intermediary name is only correct for ONE
 * toolchain. They were babric's ({@code class_92.field_1707}, {@code class_124.method_440}), so on the
 * Ornithe build - whose runtime namespace is calamus gen2, {@code net/minecraft/unmapped/C_...} -
 * neither name existed and the tint was never applied. In this module the real class is on the compile
 * classpath, so both members are ordinary mapped references that loom remaps per toolchain, and the
 * whole question goes away.
 *
 * <p>This module is also a better gate than the mixin plugin's {@code STATIONAPI_ONLY_MIXINS} check:
 * the mod it belongs to is only loaded when StationAPI is installed, so there is nothing to skip.
 */
@Mixin(ArsenicItemRenderer.class)
public class ArsenicDroppedItemTintMixin {

	@ModifyExpressionValue(method = "renderVanilla",
		at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/item/ItemRenderer;useCustomDisplayColor:Z"))
	private boolean retroapi$tintDroppedSprite(boolean vanilla, @Local(argsOnly = true) ItemStack stack) {
		return vanilla || ItemTint.of(stack) != ItemTint.NONE;
	}

	@WrapOperation(method = "renderVanilla",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/item/Item;getColorMultiplier(I)I"))
	private int retroapi$droppedSpriteColor(Item item, int meta, Operation<Integer> original) {
		final int color = ItemTint.of(item.id, meta);
		return color == ItemTint.NONE ? original.call(item, meta) : color;
	}

	@Inject(method = "renderVanilla", at = @At("RETURN"))
	private void retroapi$untintDroppedSprite(CallbackInfo ci, @Local(argsOnly = true) ItemStack stack) {
		if (ItemTint.of(stack) == ItemTint.NONE) {
			return;
		}
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
	}
}
