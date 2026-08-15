package com.periut.retrotweaks.mixin.client.render;

import com.periut.retrotweaks.config.Config;

import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Compensates for {@link com.periut.retrotweaks.mixin.item.BowHeldFixBowItemMixin}
 * forcing bows into the handheld render pose: that pose sits higher than a bow is meant to, so
 * nudge it back down right before the player's held-item render call. From UniTweaks
 * (mixin/bugfixes/bowheldfix/PlayerEntityRendererMixin).
 */
@Mixin(PlayerEntityRenderer.class)
public class BowHeldFixPlayerEntityRendererMixin {

	@Inject(method = "renderMore(Lnet/minecraft/entity/player/PlayerEntity;F)V",
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;)V",
			shift = At.Shift.BEFORE, ordinal = 1))
	private void retrotweaks$offsetBowRendering(PlayerEntity entity, float f, CallbackInfo ci) {
		if (Config.BUGFIXES.bowHeldFix) {
			ItemStack itemStack = entity.getHand();
			if (itemStack != null && itemStack.getItem() instanceof BowItem) {
				GL11.glTranslatef(0.0F, -0.5F, 0.0F);
			}
		}
	}
}
