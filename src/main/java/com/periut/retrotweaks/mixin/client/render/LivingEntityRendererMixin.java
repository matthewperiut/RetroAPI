package com.periut.retrotweaks.mixin.client.render;

import com.periut.retrotweaks.config.Config;

import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hides the entity id tags F3 draws above every mob. From UniTweaks. */
@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin {

	@Inject(method = "renderNameTag(Lnet/minecraft/entity/LivingEntity;DDD)V", at = @At("HEAD"), cancellable = true)
	private void retrotweaks$hideEntityIds(LivingEntity entity, double x, double y, double z, CallbackInfo ci) {
		if (Config.HUD.debug.disableEntityIdTags) ci.cancel();
	}
}
