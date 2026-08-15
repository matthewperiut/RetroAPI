package com.periut.retrotweaks.mixin.client.render;

import com.periut.retrotweaks.config.Config;

import net.minecraft.client.render.WorldRenderer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.HitResult;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The breaking-progress overlay is drawn slightly outside the block's faces, so on the bottom face
 * it z-fights and disappears. Shrinking the overlay slightly fixes this. From UniTweaks.
 */
@Mixin(WorldRenderer.class)
public class BreakingAnimationMixin {

	@Inject(method = "renderMiningProgress", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/render/Tessellator;setOffset(DDD)V", ordinal = 0))
	private void retrotweaks$increaseBreakingAnimationZOffset(PlayerEntity entity, HitResult hit, int i,
			ItemStack handStack, float tickDelta, CallbackInfo ci) {
		if (Config.BUGFIXES.breakingAnimationFix) {
			GL11.glScalef(0.95F, 0.95F, 0.95F);
		}
	}
}
