package com.periut.retrotweaks.mixin.client.render;

import com.periut.retrotweaks.feature.render.ItemTint;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;

import org.lwjgl.opengl.GL11;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tints the dropped item entity - and the pickup animation, which renders through the same method -
 * the third of the three renderers that draw a flat item sprite without ever asking the block what
 * colour it is (see {@link ItemTint}).
 *
 * <p>Separate from {@link ItemRendererMixin} for the same reason {@code RotatedLogRenderMixin} is
 * separate from {@code BlockRenderManagerMixin}: that class carries UniTweaks' dropped-item-size
 * {@code @ModifyConstant} and is skipped wholesale when UniTweaks is installed (the two mods'
 * constant rewrites would eat each other's target). The tint lived in the same file and died with
 * it - a picked-up fern went grey the moment UniTweaks was added, even though UniTweaks' own
 * grassblockitemfix never touches this render path at all (its mixins cover the 3D inventory block
 * and block textures, nothing on {@code ItemRenderer}). These three injectors are safe to keep:
 * a {@code @ModifyExpressionValue} on a FIELD read, a {@code @WrapOperation} on
 * {@code Item.getColorMultiplier} and an {@code @Inject} at RETURN - none of which touch the 0.5F
 * constant UniTweaks 0.29.0 consumes, verified against its
 * {@code bugfixes.droppeditemfix.ItemRendererMixin} bytecode.
 *
 * <p>Still stood down under StationAPI ({@code STATIONAPI_SUPERSEDED_MIXINS}): arsenic
 * {@code @Overwrite}s this exact method, so injecting is a hard error there - and on that install
 * items render from JSON models whose tint comes from {@code StationApiItemColors} instead.
 */
@Mixin(ItemRenderer.class)
public class DroppedItemTintMixin {

	/**
	 * Opens vanilla's own colour branch for a stack that has a tint.
	 *
	 * <p>The flat-sprite half of {@code render} sets a colour in exactly one place, and it is behind
	 * {@code if (this.useCustomDisplayColor)} - the flag for dyed leather armour, which b1.7.3 does not
	 * even have. Every other item is drawn with whatever colour happened to be current when the
	 * renderer was entered, which is why a dropped fern took on its surroundings rather than its own:
	 * the world pass leaves white, and {@code PickupParticle} - the lerp-to-the-player animation - sets
	 * a flat grey from {@code World.getLuminance}, so the plant went grey for the half second it was
	 * being collected.
	 *
	 * <p>Widening the flag rather than setting a colour of our own keeps vanilla's arithmetic: it is
	 * the branch that multiplies by {@code getBrightnessAtEyes}, so a tinted item is shaded exactly
	 * like a dyed one would be, at the exact point in the method where the colour belongs.
	 */
	@ModifyExpressionValue(method = "render(Lnet/minecraft/entity/ItemEntity;DDDFF)V",
		at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/item/ItemRenderer;useCustomDisplayColor:Z"))
	private boolean retrotweaks$tintDroppedSprite(boolean vanilla, ItemEntity entity) {
		return vanilla || (entity != null && ItemTint.of(entity.stack) != ItemTint.NONE);
	}

	/** Feeds that branch the block's colour, for the stacks {@code getColorMultiplier} answers white for. */
	@WrapOperation(method = "render(Lnet/minecraft/entity/ItemEntity;DDDFF)V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/item/Item;getColorMultiplier(I)I"))
	private int retrotweaks$droppedSpriteColor(Item item, int meta, Operation<Integer> original,
			ItemEntity entity) {
		int color = entity == null ? ItemTint.NONE : ItemTint.of(entity.stack);
		return color == ItemTint.NONE ? original.call(item, meta) : color;
	}

	/**
	 * Puts the colour back afterwards, which vanilla never had to: nothing in b1.7.3 reaches its dyed
	 * branch, so nothing ever leaked a colour out of this method. Now that something does, the next
	 * entity in the same pass - or the rest of the particle batch, during a pickup - would inherit a
	 * green tint it never asked for.
	 */
	@Inject(method = "render(Lnet/minecraft/entity/ItemEntity;DDDFF)V", at = @At("RETURN"))
	private void retrotweaks$untintDroppedSprite(ItemEntity entity, double x, double y, double z,
			float yaw, float tickDelta, CallbackInfo ci) {
		if (entity == null || ItemTint.of(entity.stack) == ItemTint.NONE) return;
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
	}
}
